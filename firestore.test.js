const { initializeTestEnvironment, assertFails, assertSucceeds } = require('@firebase/rules-unit-testing');
const { test, before, after, beforeEach } = require('node:test');
const { readFileSync } = require('node:fs');
const { doc, setDoc, getDoc, updateDoc, deleteDoc, collection, getDocs, query, where, serverTimestamp } = require('firebase/firestore');
let env;
const alice='alice_123', bob='bob_456';
const idea = (extra={}) => ({userId:alice,title:'Greenhouse',isPublic:false,createdAt:serverTimestamp(),updatedAt:serverTimestamp(),...extra});
const db = uid => uid ? env.authenticatedContext(uid).firestore() : env.unauthenticatedContext().firestore();
before(async()=>{ env=await initializeTestEnvironment({projectId:'demo-ideaforge',firestore:{host:'127.0.0.1',port:8085,rules:readFileSync('firestore.rules','utf8')}}); });
after(async()=>{await env?.cleanup();});
beforeEach(async()=>{await env.clearFirestore();});
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
