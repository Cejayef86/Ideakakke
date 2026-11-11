const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: "127.0.0.1",
      port: 8085,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: cannot read ideas", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("ideas").get());
});

test("Unauthenticated user: cannot create ideas", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection("ideas").doc("idea_1").set({
      userId: ALICE_UID,
      title: "My Idea",
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );
});

test("Authenticated user: can create their own valid idea", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("ideas").doc("idea_1").set({
      userId: ALICE_UID,
      title: "Automated Greenhouse",
      category: "Creative Projects",
      tags: ["#tech", "#nature"],
      priority: "High",
      matrixQuadrant: "do_first",
      isPublic: true,
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );
});

test("Authenticated user: cannot create idea with spoofed userId", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("ideas").doc("idea_2").set({
      userId: BOB_UID,
      title: "Spoofed Idea",
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );
});

test("Authenticated user: cannot update another user's idea", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("ideas").doc("bob_idea").set({
      userId: BOB_UID,
      title: "Bob's Secret Idea",
      isPublic: false,
      createdAt: new Date(),
      updatedAt: new Date(),
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("ideas").doc("bob_idea").update({
      title: "Hacked Title",
      updatedAt: new Date(),
    })
  );
});

test("Authenticated user: can read public idea created by another user", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("ideas").doc("bob_public_idea").set({
      userId: BOB_UID,
      title: "Bob's Public Idea",
      isPublic: true,
      createdAt: new Date(),
      updatedAt: new Date(),
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("ideas").doc("bob_public_idea").get());
});

test("Authenticated user: cannot read private idea of another user", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("ideas").doc("bob_private_idea").set({
      userId: BOB_UID,
      title: "Bob's Private Idea",
      isPublic: false,
      createdAt: new Date(),
      updatedAt: new Date(),
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("ideas").doc("bob_private_idea").get());
});

test("Comments: authenticated user can comment on public idea", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("ideas").doc("shared_idea").set({
      userId: BOB_UID,
      title: "Shared Idea",
      isPublic: true,
      createdAt: new Date(),
      updatedAt: new Date(),
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("ideas").doc("shared_idea").collection("comments").doc("comment_1").set({
      ideaId: "shared_idea",
      userId: ALICE_UID,
      userName: "Alice",
      content: "Great idea! I recommend checking marketplace for sensors.",
      createdAt: new Date(),
    })
  );
});
