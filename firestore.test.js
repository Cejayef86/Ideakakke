const { initializeTestEnvironment, assertFails, assertSucceeds } = require('@firebase/rules-unit-testing');
const { test, before, after, beforeEach } = require('node:test');
const { readFileSync } = require('node:fs');
const { doc, setDoc, getDoc, updateDoc, deleteDoc, collection, collectionGroup, getDocs, query, where, serverTimestamp } = require('firebase/firestore');
let env;
const alice='alice_123', bob='bob_456';
const idea = (extra={}) => ({userId:alice,title:'Greenhouse',isPublic:false,createdAt:serverTimestamp(),updatedAt:serverTimestamp(),...extra});
const db = uid => uid ? env.authenticatedContext(uid).firestore() : env.unauthenticatedContext().firestore();
before(async()=>{ env=await initializeTestEnvironment({projectId:'demo-ideaforge',firestore:{host:'127.0.0.1',port:8085,rules:readFileSync('firestore.rules','utf8')}}); });
after(async()=>{await env?.cleanup();});
beforeEach(async()=>{
 await env.clearFirestore();
 await env.withSecurityRulesDisabled(async context=>{for(const uid of [alice,bob]) await setDoc(doc(context.firestore(),'users',uid,'preferences','community'),{termsVersion:'2026-11-13',acceptedAt:new Date()});});
});
test('anonymous users cannot create or query ideas',async()=>{
 await assertFails(setDoc(doc(db(null),'ideas','one'),idea()));
 await assertFails(getDocs(collection(db(null),'ideas')));
});
test('owners can create, query, edit and delete private ideas',async()=>{
 const ref=doc(db(alice),'ideas','one');await assertSucceeds(setDoc(ref,idea()));
 await assertSucceeds(getDocs(query(collection(db(alice),'ideas'),where('userId','==',alice))));
 await assertSucceeds(updateDoc(ref,{title:'Updated',updatedAt:serverTimestamp()}));
 await assertSucceeds(deleteDoc(ref));
});
test('another user cannot read, change or delete a private idea',async()=>{
 await setDoc(doc(db(alice),'ideas','one'),idea());const ref=doc(db(bob),'ideas','one');
 await assertFails(getDoc(ref));await assertFails(updateDoc(ref,{title:'Stolen',updatedAt:serverTimestamp()}));await assertFails(deleteDoc(ref));
});
test('public feed is readable but cannot be edited by another user',async()=>{
 await setDoc(doc(db(alice),'ideas','one'),idea({isPublic:true}));
 await assertSucceeds(getDocs(query(collection(db(bob),'ideas'),where('isPublic','==',true))));
 await assertFails(updateDoc(doc(db(bob),'ideas','one'),{title:'Stolen',updatedAt:serverTimestamp()}));
});
test('cannot spoof owner, change ownership or creation time',async()=>{
 await assertFails(setDoc(doc(db(bob),'ideas','fake'),idea()));
 const ref=doc(db(alice),'ideas','one');await setDoc(ref,idea());
 await assertFails(updateDoc(ref,{userId:bob,updatedAt:serverTimestamp()}));
 await assertFails(updateDoc(ref,{createdAt:new Date(0),updatedAt:serverTimestamp()}));
});
for(const [field,max] of [['tags',30],['collaborators',30],['aiActionPlan',50],['aiMarketplaceKeywords',50]]) {
 for(const invalid of [123,null,true,{}]) test(`${field}: rejects non-string later element ${JSON.stringify(invalid)}`,async()=>{
  await assertFails(setDoc(doc(db(alice),'ideas','bad'),idea({isPublic:true,[field]:['valid',invalid]})));
 });
 test(`${field}: validates final element at its maximum size`,async()=>{
  const values=Array(max).fill('valid');values[max-1]=42;
  await assertFails(setDoc(doc(db(alice),'ideas','bad'),idea({[field]:values})));
 });
 test(`${field}: rejects oversized list`,async()=>{
  await assertFails(setDoc(doc(db(alice),'ideas','bad'),idea({[field]:Array(max+1).fill('text')})));
 });
}
test('all lists can simultaneously reach their documented limits',async()=>{
 await assertSucceeds(setDoc(doc(db(alice),'ideas','full'),idea({tags:Array(30).fill('tag'),collaborators:Array(30).fill('name'),aiActionPlan:Array(50).fill('step'),aiMarketplaceKeywords:Array(50).fill('gear')})));
});
test('empty lists and partial priority changes are valid and preserve research',async()=>{
 const ref=doc(db(alice),'ideas','one');await setDoc(ref,idea({tags:[],aiResearchSummary:'Fresh research',collaborators:['Alice','Bob']}));
 await updateDoc(ref,{priority:'High',updatedAt:serverTimestamp()});
 const saved=(await getDoc(ref)).data();require('node:assert/strict').equal(saved.aiResearchSummary,'Fresh research');
 require('node:assert/strict').deepEqual(saved.collaborators,['Alice','Bob']);
});
test('unknown fields including injected document IDs are rejected',async()=>{
 await assertFails(setDoc(doc(db(alice),'ideas','bad'),idea({id:'spoof'})));
});
test('invalid title, timestamps, priority and quadrant are rejected',async()=>{
 for(const extra of [{title:''},{title:'a'.repeat(201)},{createdAt:'bad'},{updatedAt:'bad'},{priority:'Critical'},{matrixQuadrant:'anything'}]) {
  await assertFails(setDoc(doc(db(alice),'ideas','bad'),idea(extra)));
 }
});
test('valid comments, author and owner deletion, and privacy changes',async()=>{
 const parent=doc(db(alice),'ideas','one');await setDoc(parent,idea({isPublic:true}));
 const comment=doc(db(bob),'ideas','one','comments','c1');
 const payload={ideaId:'one',userId:bob,userName:'Bob',content:'Useful',createdAt:serverTimestamp()};
 await assertSucceeds(setDoc(comment,payload));
 await assertFails(updateDoc(comment,{content:'Edited'}));
 await assertSucceeds(deleteDoc(comment));
 await setDoc(comment,payload);
 await assertSucceeds(deleteDoc(doc(db(alice),'ideas','one','comments','c1')));
 await updateDoc(parent,{isPublic:false,updatedAt:serverTimestamp()});
 await assertFails(setDoc(comment,payload));await assertFails(getDocs(collection(db(bob),'ideas','one','comments')));
});
test('comment author and idea ID cannot be spoofed',async()=>{
 await setDoc(doc(db(alice),'ideas','one'),idea({isPublic:true}));
 for(const extra of [{userId:alice},{ideaId:'other'},{content:''},{content:'a'.repeat(2001)},{id:'injected'}]) {
  await assertFails(setDoc(doc(db(bob),'ideas','one','comments','bad'),{ideaId:'one',userId:bob,content:'Good',createdAt:serverTimestamp(),...extra}));
 }
});

test('string lists support empty text, punctuation and newlines',async()=>{
 await assertSucceeds(setDoc(doc(db(alice),'ideas','text'),idea({tags:['','a|b','line\nbreak'],aiActionPlan:['one','two']})));
});
test('list separator control character is reserved',async()=>{
 await assertFails(setDoc(doc(db(alice),'ideas','control'),idea({tags:['a\u001Fb']})));
});


test('terms consent is required for public uploads and comments, private creation remains available',async()=>{
 await deleteDoc(doc(db(alice),'users',alice,'preferences','community'));
 await assertSucceeds(setDoc(doc(db(alice),'ideas','private'),idea()));
 await assertFails(setDoc(doc(db(alice),'ideas','public'),idea({isPublic:true})));
 await assertFails(updateDoc(doc(db(alice),'ideas','private'),{isPublic:true,updatedAt:serverTimestamp()}));
 await setDoc(doc(db(bob),'ideas','bobs'),idea({userId:bob,isPublic:true}));
 await assertFails(setDoc(doc(db(alice),'ideas','bobs','comments','c'),{ideaId:'bobs',userId:alice,content:'Hi',createdAt:serverTimestamp()}));
 await assertSucceeds(setDoc(doc(db(alice),'users',alice,'preferences','community'),{termsVersion:'2026-11-13',acceptedAt:serverTimestamp()}));
 await assertFails(updateDoc(doc(db(alice),'users',alice,'preferences','community'),{termsVersion:'invalid'}));
});

test('block lists are private and block either direction of new comments',async()=>{
 await setDoc(doc(db(alice),'ideas','one'),idea({isPublic:true}));
 const block=doc(db(alice),'users',alice,'blocks',bob);
 await assertSucceeds(setDoc(block,{createdAt:serverTimestamp()}));
 await assertFails(getDoc(doc(db(bob),'users',alice,'blocks',bob)));
 await assertFails(setDoc(doc(db(bob),'users',alice,'blocks',bob),{createdAt:serverTimestamp()}));
 await assertFails(setDoc(doc(db(alice),'users',alice,'blocks',alice),{createdAt:serverTimestamp()}));
 const c=doc(db(bob),'ideas','one','comments','c');const payload={ideaId:'one',userId:bob,content:'Hi',createdAt:serverTimestamp()};
 await assertFails(setDoc(c,payload));
 await deleteDoc(block);
 await setDoc(doc(db(bob),'users',bob,'blocks',alice),{createdAt:serverTimestamp()});
 await assertFails(setDoc(c,payload));
 await deleteDoc(doc(db(bob),'users',bob,'blocks',alice));
 await assertSucceeds(setDoc(c,payload));
});

test('reports are bounded, owner-only, immutable and cannot target unreadable ideas',async()=>{
 await setDoc(doc(db(alice),'ideas','one'),idea());
 const payload={kind:'ai',ideaId:'one',commentId:'',reason:'Other',details:'Incorrect suggestion',createdAt:serverTimestamp()};
 const report=doc(db(alice),'users',alice,'reports','r1');
 await assertSucceeds(setDoc(report,payload));
 await assertSucceeds(getDoc(report));
 await assertFails(getDoc(doc(db(bob),'users',alice,'reports','r1')));
 await assertFails(updateDoc(report,{details:'Edited'}));
 await assertFails(setDoc(doc(db(alice),'users',alice,'reports','r2'),{...payload,details:'x'.repeat(2001)}));
 await assertFails(setDoc(doc(db(alice),'users',alice,'reports','r2'),{...payload,admin:true}));
 await assertFails(setDoc(doc(db(bob),'users',bob,'reports','r2'),payload));
 await assertSucceeds(deleteDoc(report));
});

test('deletion marker requires recent authentication, cannot be spoofed, reset or removed',async()=>{
 const fresh=env.authenticatedContext(alice,{auth_time:Math.floor(Date.now()/1000)}).firestore();
 const stale=env.authenticatedContext(bob,{auth_time:1}).firestore();
 const marker=doc(fresh,'accountDeletions',alice);
 await assertFails(setDoc(doc(stale,'accountDeletions',bob),{startedAt:serverTimestamp()}));
 await assertFails(setDoc(doc(fresh,'accountDeletions',bob),{startedAt:serverTimestamp()}));
 await assertFails(setDoc(marker,{startedAt:serverTimestamp(),email:'secret'}));
 await assertSucceeds(setDoc(marker,{startedAt:serverTimestamp()}));
 await assertFails(setDoc(marker,{startedAt:serverTimestamp()}));
 await assertFails(deleteDoc(marker));
 await assertFails(getDoc(doc(db(bob),'accountDeletions',alice)));
 await assertFails(getDocs(collection(fresh,'accountDeletions')));
});

test('deletion prevents all new user data and new comments on deleting owners ideas',async()=>{
 await setDoc(doc(db(alice),'ideas','one'),idea({isPublic:true}));
 await setDoc(doc(db(bob),'ideas','other'),idea({userId:bob,isPublic:true}));
 const fresh=env.authenticatedContext(alice,{auth_time:Math.floor(Date.now()/1000)}).firestore();
 await setDoc(doc(fresh,'accountDeletions',alice),{startedAt:serverTimestamp()});
 await assertFails(setDoc(doc(fresh,'ideas','new'),idea()));
 await assertFails(updateDoc(doc(fresh,'ideas','one'),{title:'Recreated',updatedAt:serverTimestamp()}));
 await assertFails(setDoc(doc(fresh,'users',alice,'blocks',bob),{createdAt:serverTimestamp()}));
 await assertFails(setDoc(doc(fresh,'users',alice,'preferences','community'),{termsVersion:'2026-11-13',acceptedAt:serverTimestamp()}));
 await assertFails(setDoc(doc(fresh,'ideas','other','comments','c'),{ideaId:'other',userId:alice,content:'Late',createdAt:serverTimestamp()}));
 await assertFails(setDoc(doc(db(bob),'ideas','one','comments','late'),{ideaId:'one',userId:bob,content:'Late',createdAt:serverTimestamp()}));
 await assertSucceeds(getDocs(query(collection(fresh,'ideas'),where('userId','==',alice))));
 await assertSucceeds(deleteDoc(doc(fresh,'ideas','one')));
});

test('account cleanup can find and delete own orphaned and private-parent comments only',async()=>{
 await env.withSecurityRulesDisabled(async context=>{
  const admin=context.firestore();
  await setDoc(doc(admin,'ideas','private'),idea({userId:bob,isPublic:false}));
  for(const parent of ['orphan','private']) {
   await setDoc(doc(admin,'ideas',parent,'comments','own'),{ideaId:parent,userId:alice,content:'Mine',createdAt:new Date()});
   await setDoc(doc(admin,'ideas',parent,'comments','other'),{ideaId:parent,userId:bob,content:'Not mine',createdAt:new Date()});
  }
 });
 const own=await assertSucceeds(getDocs(query(collectionGroup(db(alice),'comments'),where('userId','==',alice))));
 require('node:assert/strict').equal(own.size,2);
 for(const comment of own.docs) await assertSucceeds(deleteDoc(comment.ref));
 await assertFails(getDocs(collectionGroup(db(alice),'comments')));
 await assertFails(getDocs(query(collectionGroup(db(alice),'comments'),where('userId','==',bob))));
 await assertFails(deleteDoc(doc(db(alice),'ideas','orphan','comments','other')));
});

test('string-list text is bounded on create and update',async()=>{
 for(const field of ['tags','collaborators','aiActionPlan','aiMarketplaceKeywords']) {
  const ref=doc(db(alice),'ideas',field);
  await assertFails(setDoc(ref,idea({[field]:['x'.repeat(15001)]})));
  await setDoc(ref,idea());
  await assertFails(updateDoc(ref,{[field]:['x'.repeat(15001)],updatedAt:serverTimestamp()}));
 }
});
