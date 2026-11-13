import { test, before, after, beforeEach } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { initializeTestEnvironment } from '@firebase/rules-unit-testing';
import { initializeApp, deleteApp } from 'firebase/app';
import { connectAuthEmulator, createUserWithEmailAndPassword, getAuth, signInWithEmailAndPassword } from 'firebase/auth';
import { collection, connectFirestoreEmulator, doc, getDocs, getFirestore, setDoc, writeBatch } from 'firebase/firestore';
import { deleteIdeaForgeAccount } from '../src/delete-account.js';

const projectId = 'demo-ideaforge-deletion';
let env;
let app;
let auth;
let db;
before(async () => {
  env = await initializeTestEnvironment({ projectId, firestore: { host: '127.0.0.1', port: 8085, rules: readFileSync('firestore.rules', 'utf8') } });
});
beforeEach(async () => {
  await env.clearFirestore();
  app = initializeApp({ projectId, apiKey: 'demo-key', appId: 'demo-app' }, `deletion-${Date.now()}`);
  auth = getAuth(app);
  connectAuthEmulator(auth, 'http://127.0.0.1:9099', { disableWarnings: true });
  db = getFirestore(app);
  connectFirestoreEmulator(db, '127.0.0.1', 8085);
});
after(async () => { await env.cleanup(); });

async function seed(email) {
  const { user } = await createUserWithEmailAndPassword(auth, email, 'disposable-emulator-password');
  await env.withSecurityRulesDisabled(async context => {
    const admin = context.firestore();
    const batch = writeBatch(admin);
    const stamp = new Date();
    batch.set(doc(admin, 'ideas', 'mine'), { userId: user.uid, title: 'My idea', isPublic: false, createdAt: stamp, updatedAt: stamp });
    batch.set(doc(admin, 'ideas', 'other'), { userId: 'someone_else', title: 'Keep this', isPublic: false, createdAt: stamp, updatedAt: stamp });
    for (let i = 0; i < 105; i++) {
      batch.set(doc(admin, 'ideas', 'mine', 'comments', `foreign_${i}`), { userId: 'someone_else', ideaId: 'mine', content: 'Discussion', createdAt: stamp });
      batch.set(doc(admin, 'ideas', 'other', 'comments', `mine_${i}`), { userId: user.uid, ideaId: 'other', content: 'My comment', createdAt: stamp });
    }
    batch.set(doc(admin, 'ideas', 'orphan', 'comments', 'mine'), { userId: user.uid, ideaId: 'orphan', content: 'Orphan', createdAt: stamp });
    batch.set(doc(admin, 'ideas', 'other', 'comments', 'keep'), { userId: 'someone_else', ideaId: 'other', content: 'Keep', createdAt: stamp });
    batch.set(doc(admin, 'users', user.uid, 'blocks', 'someone_else'), { createdAt: stamp });
    batch.set(doc(admin, 'users', user.uid, 'preferences', 'community'), { termsVersion: '2026-11-13', acceptedAt: stamp });
    batch.set(doc(admin, 'users', user.uid, 'reports', 'r'), { kind: 'ai', ideaId: 'mine', commentId: '', reason: 'Other', details: 'Wrong', createdAt: stamp });
    await batch.commit();
  });
  return user.uid;
}

test('full account cleanup paginates data and preserves other accounts', async () => {
  const uid = await seed('delete-complete@example.test');
  await deleteIdeaForgeAccount(auth, db);
  assert.equal(auth.currentUser, null);
  await assert.rejects(signInWithEmailAndPassword(auth, 'delete-complete@example.test', 'disposable-emulator-password'));
  await env.withSecurityRulesDisabled(async context => {
    const admin = context.firestore();
    assert.equal((await getDocs(collection(admin, 'ideas'))).docs.some(item => item.id === 'mine'), false);
    assert.equal((await getDocs(collection(admin, 'ideas', 'mine', 'comments'))).size, 0);
    assert.equal((await getDocs(collection(admin, 'ideas', 'orphan', 'comments'))).size, 0);
    const remaining = await getDocs(collection(admin, 'ideas', 'other', 'comments'));
    assert.deepEqual(remaining.docs.map(item => item.id), ['keep']);
    for (const name of ['blocks', 'reports', 'preferences']) assert.equal((await getDocs(collection(admin, 'users', uid, name))).size, 0);
    const markers = await getDocs(collection(admin, 'accountDeletions'));
    assert.equal(markers.size, 1);
    assert.deepEqual(Object.keys(markers.docs[0].data()), ['startedAt']);
  });
  await deleteApp(app);
});

test('interrupted deletion stays locked, is retryable, and deletes auth last', async () => {
  const uid = await seed('delete-retry@example.test');
  await assert.rejects(deleteIdeaForgeAccount(auth, db, progress => { if (progress.includes('ideas')) throw new Error('Simulated interruption'); }), /Simulated interruption/);
  assert.equal(auth.currentUser.uid, uid);
  await assert.rejects(setDoc(doc(db, 'ideas', 'late'), { userId: uid, title: 'Late', createdAt: new Date(), updatedAt: new Date() }));
  await deleteIdeaForgeAccount(auth, db);
  assert.equal(auth.currentUser, null);
  await deleteApp(app);
});
