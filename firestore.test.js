const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastTimestamp() {
  return new Date(Date.now() - 5000);
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

test("Unauthenticated user cannot read or write markets or products", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("markets").get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("products").get()
  );
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("markets")
      .doc("m1")
      .set({
        userId: ALICE_UID,
        name: "Giassi 13",
        active: true,
        createdAt: pastTimestamp(),
        updatedAt: pastTimestamp(),
      })
  );
});

test("Authenticated user can create and read own settings and markets", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = pastTimestamp();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      seededDefaults: true,
      notificationsEnabled: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("markets")
      .doc("market_1")
      .set({
        userId: ALICE_UID,
        name: "Giassi 13",
        active: true,
        createdAt: ts,
        updatedAt: ts,
      })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("markets").get()
  );
});

test("Cross-user isolation: Bob cannot read or write Alice's markets or products", async () => {
  const ts = pastTimestamp();
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await db
      .collection("users")
      .doc(ALICE_UID)
      .collection("markets")
      .doc("market_1")
      .set({
        userId: ALICE_UID,
        name: "Komprão 66",
        active: true,
        createdAt: ts,
        updatedAt: ts,
      });
    await db
      .collection("users")
      .doc(ALICE_UID)
      .collection("products")
      .doc("prod_1")
      .set({
        userId: ALICE_UID,
        name: "Iogurte Natural",
        marketId: "market_1",
        marketName: "Komprão 66",
        expiryDate: "2026-11-16",
        notes: "",
        status: "active",
        createdAt: ts,
        updatedAt: ts,
      });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("markets").get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("products").get()
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("products")
      .doc("prod_1")
      .get()
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("products")
      .doc("prod_2")
      .set({
        userId: BOB_UID,
        name: "Invasor",
        marketId: "market_1",
        marketName: "Komprão 66",
        expiryDate: "2026-11-20",
        status: "active",
        createdAt: ts,
        updatedAt: ts,
      })
  );
});

test("Product creation requires valid market, valid date, and either name or photo", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = pastTimestamp();

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("markets")
      .doc("market_1")
      .set({
        userId: ALICE_UID,
        name: "Manenti",
        active: true,
        createdAt: ts,
        updatedAt: ts,
      })
  );

  // Valid product with name and no photo
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("products")
      .doc("prod_name")
      .set({
        userId: ALICE_UID,
        name: "Queijo Mussarela",
        marketId: "market_1",
        marketName: "Manenti",
        expiryDate: "2026-11-18",
        notes: "Prateleira frios",
        status: "active",
        createdAt: ts,
        updatedAt: ts,
      })
  );

  // Valid product with photo and empty name
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("products")
      .doc("prod_photo")
      .set({
        userId: ALICE_UID,
        name: "",
        marketId: "market_1",
        marketName: "Manenti",
        expiryDate: "2026-11-14",
        photoBase64: "dGVzdF9waG90b19iYXNlNjQ=",
        status: "active",
        createdAt: ts,
        updatedAt: ts,
      })
  );

  // Fails when both name is empty and photo is missing
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("products")
      .doc("prod_empty")
      .set({
        userId: ALICE_UID,
        name: "",
        marketId: "market_1",
        marketName: "Manenti",
        expiryDate: "2026-11-14",
        status: "active",
        createdAt: ts,
        updatedAt: ts,
      })
  );

  // Fails with ghost field (Shadow Update test)
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("products")
      .doc("prod_ghost")
      .set({
        userId: ALICE_UID,
        name: "Leite",
        marketId: "market_1",
        marketName: "Manenti",
        expiryDate: "2026-11-15",
        status: "active",
        isAdmin: true,
        createdAt: ts,
        updatedAt: ts,
      })
  );

  // Fails if market does not exist
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("products")
      .doc("prod_orphan")
      .set({
        userId: ALICE_UID,
        name: "Leite",
        marketId: "missing_market",
        marketName: "Inexistente",
        expiryDate: "2026-11-15",
        status: "active",
        createdAt: ts,
        updatedAt: ts,
      })
  );
});

test("Product archive, resolve, and restore transitions are enforced", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = pastTimestamp();

  await aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("markets")
    .doc("market_1")
    .set({
      userId: ALICE_UID,
      name: "Moniari",
      active: true,
      createdAt: ts,
      updatedAt: ts,
    });

  const prodRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("products")
    .doc("prod_flow");

  await assertSucceeds(
    prodRef.set({
      userId: ALICE_UID,
      name: "Requeijão",
      marketId: "market_1",
      marketName: "Moniari",
      expiryDate: "2026-11-10",
      status: "active",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Auto-archive succeeds with reason & archivedDate
  await assertSucceeds(
    prodRef.update({
      status: "archived_auto",
      archiveReason: "auto",
      archivedDate: "2026-11-14",
      updatedAt: pastTimestamp(),
    })
  );

  // Restoring removes archiveReason and archivedDate and sets status back to active
  await assertSucceeds(
    prodRef.set(
      {
        userId: ALICE_UID,
        name: "Requeijão",
        marketId: "market_1",
        marketName: "Moniari",
        expiryDate: "2026-11-10",
        status: "active",
        createdAt: ts,
        updatedAt: pastTimestamp(),
      }
    )
  );
});
