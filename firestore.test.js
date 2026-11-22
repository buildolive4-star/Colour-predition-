const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_001";
const BOB_UID = "bob_uid_002";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validProfile(uid, ts) {
  return {
    userId: uid,
    displayName: "Nova Player",
    email: `${uid}@example.com`,
    publicUserId: "CP-A1B2C3",
    virtualCoins: 2500,
    totalGames: 0,
    totalWins: 0,
    totalVirtualPoints: 0,
    currentStreak: 0,
    bestStreak: 0,
    notifyResults: true,
    notifyRoundClosing: true,
    notifyBonuses: true,
    accountStatus: "ACTIVE",
    createdAt: ts,
    updatedAt: ts,
  };
}

function validPrediction(uid, ts) {
  return {
    userId: uid,
    modeId: "mode_30s",
    periodId: "20261122-30S-00101",
    selectedColor: "GREEN",
    stakeCoins: 100,
    status: "PENDING",
    rewardCoins: 0,
    createdAt: ts,
    updatedAt: ts,
  };
}

function validWalletTx(uid, ts) {
  return {
    userId: uid,
    txType: "WELCOME_BONUS",
    amount: 2500,
    previousBalance: 0,
    newBalance: 2500,
    referenceId: "welcome_bonus",
    description: "Initial virtual demo coins",
    createdAt: ts,
  };
}

function validRound(ts) {
  return {
    modeId: "mode_30s",
    periodId: "20261122-30S-00101",
    startTimeMs: 1795350000000,
    endTimeMs: 1795350030000,
    winningColor: "GREEN",
    resultDigit: 7,
    resultHash: "a".repeat(64),
    status: "COMPLETED",
    visibility: "PUBLIC",
    createdAt: ts,
  };
}

function validLeaderboard(uid, ts) {
  return {
    userId: uid,
    displayName: "Nova Player",
    publicUserId: "CP-A1B2C3",
    totalVirtualPoints: 200,
    totalGames: 1,
    totalWins: 1,
    bestStreak: 1,
    badgeTitle: "Pulse Rookie",
    visibility: "PUBLIC",
    createdAt: ts,
    updatedAt: ts,
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

test("Unauthenticated users are denied on all collections", async () => {
  const db = testEnv.unauthenticatedContext().firestore();
  const now = new Date(Date.now() - 1000);
  await assertFails(db.collection("users").doc(ALICE_UID).get());
  await assertFails(db.collection("users").doc(ALICE_UID).set(validProfile(ALICE_UID, now)));
  await assertFails(db.collection("game_rounds").where("visibility", "==", "PUBLIC").get());
  await assertFails(db.collection("leaderboard").where("visibility", "==", "PUBLIC").get());
});

test("User profile: owner create/read/update succeeds; cross-user PII read, shadow fields, and future timestamps fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const past = new Date(Date.now() - 2000);

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(validProfile(ALICE_UID, past)));
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());

  // Cross-user PII read denied & list denied
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(aliceDb.collection("users").get());

  // Valid profile update action
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      displayName: "Chroma Ace",
      notifyResults: false,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Valid wallet & stats update action
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      virtualCoins: 2400,
      totalGames: 1,
      updatedAt: new Date(Date.now() - 400),
    })
  );

  // Shadow field / privilege escalation fails
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 300),
    })
  );

  // Immutable createdAt tamper fails
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      createdAt: new Date(Date.now() - 200),
      updatedAt: new Date(Date.now() - 200),
    })
  );

  // Negative virtual coins fail
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      virtualCoins: -50,
      updatedAt: new Date(Date.now() - 200),
    })
  );

  // Future timestamp rejected
  await assertFails(
    bobDb.collection("users").doc(BOB_UID).set(validProfile(BOB_UID, new Date(Date.now() + 60000)))
  );
});

test("Predictions: create PENDING, settle once to WON, reject second update (terminal lock) and cross-user access", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const past = new Date(Date.now() - 3000);
  const predId = "mode_30s_20261122-30S-00101";

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("predictions").doc(predId).set(validPrediction(ALICE_UID, past))
  );

  // Owner list subcollection succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("predictions").get()
  );

  // Cross-user read/list fails
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("predictions").get()
  );

  // Settle PENDING -> WON succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("predictions").doc(predId).update({
      status: "WON",
      winningColor: "GREEN",
      rewardCoins: 200,
      resultHash: "b".repeat(64),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Terminal state lock: modifying WON prediction fails
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("predictions").doc(predId).update({
      rewardCoins: 9999,
      updatedAt: new Date(Date.now() - 200),
    })
  );

  // Delete prediction fails
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("predictions").doc(predId).delete()
  );
});

test("Wallet ledger: immutable create with balanced math succeeds; balance mismatch or mutation fails", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const past = new Date(Date.now() - 2000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("wallet_transactions").doc("welcome_bonus").set(validWalletTx(ALICE_UID, past))
  );

  // Unbalanced math fails
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("wallet_transactions").doc("bad_tx").set({
      ...validWalletTx(ALICE_UID, past),
      newBalance: 99999,
    })
  );

  // Update and delete denied on immutable ledger
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("wallet_transactions").doc("welcome_bonus").update({ amount: 5000 })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("wallet_transactions").doc("welcome_bonus").delete()
  );
});

test("Notifications: create, mark read, and delete succeed; mutating message fails", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const past = new Date(Date.now() - 2000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("notifications").doc("notif_1").set({
      userId: ALICE_UID,
      category: "DAILY_BONUS",
      title: "Daily Bonus Claimed",
      message: "You received 500 virtual coins.",
      referenceId: "daily_20261122",
      read: false,
      createdAt: past,
      updatedAt: past,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("notifications").doc("notif_1").update({
      read: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("notifications").doc("notif_1").update({
      message: "Tampered",
      updatedAt: new Date(Date.now() - 400),
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("notifications").doc("notif_1").delete()
  );
});

test("Game rounds & Leaderboard: PUBLIC filtered queries succeed; unfiltered queries and cross-user writes fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const past = new Date(Date.now() - 2000);
  const roundId = "mode_30s_20261122-30S-00101";

  await assertSucceeds(aliceDb.collection("game_rounds").doc(roundId).set(validRound(past)));
  await assertSucceeds(aliceDb.collection("game_rounds").where("visibility", "==", "PUBLIC").get());
  await assertFails(aliceDb.collection("game_rounds").get());
  await assertFails(aliceDb.collection("game_rounds").doc(roundId).update({ winningColor: "RED" }));

  await assertSucceeds(aliceDb.collection("leaderboard").doc(ALICE_UID).set(validLeaderboard(ALICE_UID, past)));
  await assertSucceeds(bobDb.collection("leaderboard").where("visibility", "==", "PUBLIC").get());
  await assertFails(bobDb.collection("leaderboard").get());
  await assertFails(bobDb.collection("leaderboard").doc(ALICE_UID).update({ totalVirtualPoints: 0 }));
});
