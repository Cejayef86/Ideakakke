import { initializeApp } from 'firebase/app';
import { browserSessionPersistence, getAuth, GoogleAuthProvider, onAuthStateChanged, reauthenticateWithPopup, setPersistence, signInWithPopup, signOut } from 'firebase/auth';
import { getFirestore } from 'firebase/firestore';
import { firebaseConfig } from './firebase-config.js';
import { deleteIdeaForgeAccount } from './delete-account.js';

const auth = getAuth(initializeApp(firebaseConfig));
const db = getFirestore();
const provider = new GoogleAuthProvider();
provider.setCustomParameters({ prompt: 'select_account' });
const signInButton = document.querySelector('#sign-in');
const signOutButton = document.querySelector('#sign-out');
const deleteButton = document.querySelector('#delete-account');
const confirm = document.querySelector('#confirm-delete');
const account = document.querySelector('#account');
const status = document.querySelector('#status');
let busy = false;
let completed = false;
function render() {
  signInButton.hidden = !!auth.currentUser;
  signOutButton.hidden = !auth.currentUser;
  deleteButton.disabled = busy || !auth.currentUser || !confirm.checked;
  confirm.disabled = busy;
  signInButton.disabled = busy;
  signOutButton.disabled = busy;
  account.textContent = completed ? 'Account deleted.' : auth.currentUser ? `Signed in as ${auth.currentUser.email || auth.currentUser.displayName || 'your Google account'}` : 'Not signed in.';
}
function friendlyError(error) {
  if (error.code === 'auth/popup-closed-by-user' || error.code === 'auth/cancelled-popup-request') return 'Google confirmation was cancelled. Your account was not deleted.';
  if (error.code === 'auth/user-mismatch') return 'Choose the same Google account that is currently signed in.';
  if (error.code === 'auth/unauthorized-domain') return 'Google sign-in is not configured for this website. Contact IdeaForge support.';
  return 'The operation did not finish. Check your internet connection, sign in with the same account, and retry. If deletion started, already deleted data stays deleted. You can also email IdeaForgeHQ27@gmail.com for help.';
}
async function action(fn) {
  busy = true; status.textContent = ''; render();
  try { await fn(); }
  catch (error) { status.textContent = friendlyError(error); }
  finally { busy = false; render(); }
}
confirm.addEventListener('change', render);
signInButton.addEventListener('click', () => action(async () => { await signInWithPopup(auth, provider); completed = false; }));
signOutButton.addEventListener('click', () => action(() => signOut(auth)));
deleteButton.addEventListener('click', () => action(async () => {
  if (!confirm.checked || !auth.currentUser) return;
  // Verify the current account immediately before the irreversible operation.
  await reauthenticateWithPopup(auth.currentUser, provider);
  await deleteIdeaForgeAccount(auth, db, message => { status.textContent = message; });
  completed = true;
  confirm.checked = false;
  status.textContent = 'Your IdeaForge sign-in account and the app data described above have been deleted. Your Google account is unchanged.';
}));
await setPersistence(auth, browserSessionPersistence);
onAuthStateChanged(auth, render);
render();
