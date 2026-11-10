const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_123";
const BOB_UID = "bob_uid_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
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

// 1. Unauthenticated checks
test("Unauthenticated user: cannot read posts or users", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("posts").get());
  await assertFails(unauthDb.collection("users").get());
});

// 2. User profile creation and update
test("User profile: Alice can create and update her own profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      username: "alice_vyra",
      displayName: "Alice V",
      email: "alice@example.com",
      createdAt: new Date(),
    })
  );

  // Alice cannot update someone else's profile
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      username: "bob_vyra",
      displayName: "Bob V",
      email: "bob@example.com",
      createdAt: new Date(),
    })
  );
});

// 3. Post creation and feed reading
test("Posts: authenticated user can create a post and read feed", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const postRef = aliceDb.collection("posts").doc("post_1");
  await assertSucceeds(
    postRef.set({
      userId: ALICE_UID,
      username: "alice_vyra",
      mediaUrl: "https://example.com/photo.jpg",
      mediaType: "image",
      caption: "Hello VYRA world!",
      createdAt: new Date(),
    })
  );

  // Bob can read public feed
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertSucceeds(bobDb.collection("posts").doc("post_1").get());

  // Bob cannot delete Alice's post
  await assertFails(bobDb.collection("posts").doc("post_1").delete());
});

// 4. Saved items isolation
test("Saved items: Bob cannot read Alice's saved posts", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(ALICE_UID).collection("saved").doc("s1").set({
      userId: ALICE_UID,
      postId: "post_1",
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("saved").doc("s1").get()
  );

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("saved").doc("s1").get()
  );
});

// 5. Conversations & Messages
test("Conversations: only participants can read and message", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("conversations").doc("conv_1").set({
      type: "direct",
      memberIds: [ALICE_UID, BOB_UID],
      createdAt: new Date(),
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("conversations").doc("conv_1").get());

  const charlieDb = testEnv.authenticatedContext("charlie_789").firestore();
  await assertFails(charlieDb.collection("conversations").doc("conv_1").get());
});
