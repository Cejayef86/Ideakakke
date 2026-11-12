import { deleteUser } from 'firebase/auth';
import { collection, collectionGroup, doc, getDocsFromServer, limit, query, runTransaction, serverTimestamp, where, writeBatch } from 'firebase/firestore';

/** Idempotent, paginated cleanup shared by the website and emulator integration tests. */
export async function deleteIdeaForgeAccount(auth, db, progress = () => {}) {
  const user = auth.currentUser;
  if (!user) throw new Error('Sign in with your IdeaForge Google account first.');
  const uid = user.uid;
  const check = () => { if (auth.currentUser?.uid !== uid) throw new Error('Your account changed. Sign in again.'); };
  check();
  await user.getIdToken(true);
  const marker = doc(db, 'accountDeletions', uid);
  await runTransaction(db, async tx => {
    if (!(await tx.get(marker)).exists()) tx.set(marker, { startedAt: serverTimestamp() });
  });
  const removePages = async source => {
    while (true) {
      check();
      const page = await getDocsFromServer(query(source, limit(100)));
      if (page.empty) return;
      check();
      const batch = writeBatch(db);
      page.docs.forEach(item => batch.delete(item.ref));
      await batch.commit();
    }
  };
  progress('Removing your comments…');
  await removePages(query(collectionGroup(db, 'comments'), where('userId', '==', uid)));
  progress('Removing your ideas and their discussions…');
  while (true) {
    check();
    const page = await getDocsFromServer(query(collection(db, 'ideas'), where('userId', '==', uid), limit(100)));
    if (page.empty) break;
    for (const idea of page.docs) {
      await removePages(collection(idea.ref, 'comments'));
      check();
      const batch = writeBatch(db);
      batch.delete(idea.ref);
      await batch.commit();
    }
  }
  progress('Removing settings and reports…');
  for (const name of ['blocks', 'reports', 'preferences']) await removePages(collection(db, 'users', uid, name));
  check();
  progress('Deleting your IdeaForge sign-in account…');
  await deleteUser(user);
  // The UID-only security marker is intentionally retained to stop old tokens recreating data.
}
