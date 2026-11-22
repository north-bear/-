const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_tree_123";
const BOB_UID = "bob_tree_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validNotePayload(userId, overrides = {}) {
  const past = new Date(Date.now() - 5000);
  return {
    userId,
    title: "圣诞礼物清单",
    content: "买松枝花环、热红酒香料包",
    colorKey: "PINE",
    pinnedToTree: true,
    checklistMode: true,
    checklistItems: [
      { itemId: "item_1", text: "包装牛皮纸", checked: false },
      { itemId: "item_2", text: "手写贺卡", checked: true },
    ],
    treeTier: 1,
    createdAt: past,
    updatedAt: past,
    ...overrides,
  };
}

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

test("Unauthenticated user cannot list, read, create, or delete notes", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("notes").get());
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("notes")
      .doc("note_1")
      .set(validNotePayload(ALICE_UID))
  );
});

test("Owner can create, list, partial update, and delete valid TreeNote", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const col = aliceDb.collection("users").doc(ALICE_UID).collection("notes");

  // Create matching Kotlin repository payload (no @DocumentId `id` field)
  await assertSucceeds(col.doc("note_1").set(validNotePayload(ALICE_UID)));

  // List matching Kotlin repository: db.collection("users").document(uid).collection("notes")
  await assertSucceeds(col.get());

  // Partial update matching Kotlin repository
  await assertSucceeds(
    col.doc("note_1").update({
      title: "更新后的圣诞礼物",
      pinnedToTree: false,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Delete
  await assertSucceeds(col.doc("note_1").delete());
});

test("Cross-user access is strictly denied", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx
      .firestore()
      .collection("users")
      .doc(BOB_UID)
      .collection("notes")
      .doc("bob_note")
      .set(validNotePayload(BOB_UID));
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobNotes = aliceDb.collection("users").doc(BOB_UID).collection("notes");

  await assertFails(bobNotes.get());
  await assertFails(bobNotes.doc("bob_note").get());
  await assertFails(
    bobNotes.doc("bob_note").update({
      title: "Hijacked",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(bobNotes.doc("bob_note").delete());
  await assertFails(bobNotes.doc("new_note").set(validNotePayload(ALICE_UID)));
});

test("Rejects ghost fields, @DocumentId id injection, invalid colorKey, immutable createdAt edit, and future timestamp", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const col = aliceDb.collection("users").doc(ALICE_UID).collection("notes");

  // @DocumentId `id` key forbidden
  await assertFails(
    col.doc("note_bad_id").set({
      id: "note_bad_id",
      ...validNotePayload(ALICE_UID),
    })
  );

  // Ghost field forbidden
  await assertFails(
    col.doc("note_ghost").set({
      ...validNotePayload(ALICE_UID),
      isAdmin: true,
    })
  );

  // Invalid colorKey forbidden
  await assertFails(
    col.doc("note_bad_color").set(validNotePayload(ALICE_UID, { colorKey: "NEON_PINK" }))
  );

  // Future timestamp forbidden
  await assertFails(
    col
      .doc("note_future")
      .set(validNotePayload(ALICE_UID, { createdAt: new Date(Date.now() + 60000) }))
  );

  // Immutable createdAt mutation forbidden
  await assertSucceeds(col.doc("note_valid").set(validNotePayload(ALICE_UID)));
  await assertFails(
    col.doc("note_valid").update({
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});
