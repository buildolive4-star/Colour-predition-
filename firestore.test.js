const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");
const crypto = require("node:crypto");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const SUPER_ADMIN_UID = "super_admin_uid";
const SUPER_ADMIN_EMAIL = "buildolive4@gmail.com";
const ALICE_UID = "alice_player_01";
const BOB_UID = "bob_player_02";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function sha256Hex(raw) {
  return crypto.createHash("sha256").update(raw, "utf8").digest("hex");
}

function nowPast(msAgo = 2000) {
  return new Date(Date.now() - msAgo);
}

function validUserPayload(uid, email = "alice@example.com") {
  const ts = nowPast(5000);
  return {
    userId: uid,
    displayName: "Chroma Player",
    email,
    publicUserId: "CP-123456",
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

function validAppConfigPayload(email = SUPER_ADMIN_EMAIL) {
  return {
    announcementText: "Welcome to ChromaPulse Virtual Arena!",
    redGreenMultiplier: 2.0,
    violetMultiplier: 4.5,
    mode30sEnabled: true,
    mode60sEnabled: true,
    mode3mEnabled: true,
    mode5mEnabled: true,
    adminQrLabel: "ChromaPulse Virtual Counter",
    adminQrPayload: "CHROMAPULSE:VIRTUAL-DEMO:COUNTER-01",
    updatedByEmail: email,
    updatedAt: nowPast(2000),
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

test("Unauthenticated user cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("app_config").doc("global").get());
  await assertFails(unauthDb.collection("co_admin_invites").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).set(validUserPayload(ALICE_UID))
  );
});

test("User profile isolation, valid create/update, and ghost-field rejection", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, { email: "bob@example.com", email_verified: true }).firestore();

  // Valid create with initial 2500 VC
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(validUserPayload(ALICE_UID))
  );

  // Reject creating profile with forged starting coins or extra ghost field
  await assertFails(
    bobDb.collection("users").doc(BOB_UID).set({
      ...validUserPayload(BOB_UID, "bob@example.com"),
      virtualCoins: 999999,
    })
  );
  await assertFails(
    bobDb.collection("users").doc(BOB_UID).set({
      ...validUserPayload(BOB_UID, "bob@example.com"),
      isAdmin: true,
    })
  );

  // Cross-user read & listing users denied for regular player
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(aliceDb.collection("users").get());

  // Owner partial update allowed; cannot mutate accountStatus or createdAt
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      displayName: "Alice Star",
      virtualCoins: 2400,
      updatedAt: nowPast(1000),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      accountStatus: "SUSPENDED",
      updatedAt: nowPast(500),
    })
  );
});

test("Spoofed unverified admin email is rejected", async () => {
  const spoofDb = testEnv
    .authenticatedContext("spoof_uid", {
      email: SUPER_ADMIN_EMAIL,
      email_verified: false,
    })
    .firestore();

  await assertFails(
    spoofDb.collection("app_config").doc("global").set(validAppConfigPayload())
  );
  await assertFails(spoofDb.collection("users").get());
});

test("Super Admin creates Co-Admin password -> Co-Admin unlocks via password -> Super Admin revokes password instantaneously", async () => {
  const superDb = testEnv
    .authenticatedContext(SUPER_ADMIN_UID, {
      email: SUPER_ADMIN_EMAIL,
      email_verified: true,
    })
    .firestore();
  const aliceDb = testEnv
    .authenticatedContext(ALICE_UID, {
      email: "alice@example.com",
      email_verified: true,
    })
    .firestore();

  const coAdminPassword = "TeamMod@2026";
  const inviteHash = sha256Hex(coAdminPassword);
  const wrongHash = sha256Hex("WrongPass123");

  // Super Admin initializes app_config and Alice's user doc
  await assertSucceeds(
    superDb.collection("app_config").doc("global").set(validAppConfigPayload())
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(validUserPayload(ALICE_UID))
  );

  // Regular user cannot update app_config or create co_admin_invites or list invites
  await assertFails(
    aliceDb.collection("app_config").doc("global").update({
      announcementText: "Hacked!",
      updatedAt: nowPast(1000),
    })
  );
  await assertFails(aliceDb.collection("co_admin_invites").get());

  // Super Admin creates hashed Co-Admin password invite
  const ts = nowPast(3000);
  await assertSucceeds(
    superDb.collection("co_admin_invites").doc(inviteHash).set({
      label: "Night Moderator",
      active: true,
      createdByEmail: SUPER_ADMIN_EMAIL,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Alice cannot claim Co-Admin with a wrong hash
  await assertFails(
    aliceDb.collection("co_admins").doc(ALICE_UID).set({
      userId: ALICE_UID,
      email: "alice@example.com",
      displayName: "Alice",
      label: "Fake",
      inviteHash: wrongHash,
      active: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Alice verifies password by getting co_admin_invites/{inviteHash} and claims Co-Admin grant
  await assertSucceeds(aliceDb.collection("co_admin_invites").doc(inviteHash).get());
  await assertSucceeds(
    aliceDb.collection("co_admins").doc(ALICE_UID).set({
      userId: ALICE_UID,
      email: "alice@example.com",
      displayName: "Alice",
      label: "Night Moderator",
      inviteHash,
      active: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Now Alice (active Co-Admin) CAN update global app_config, list users, and create vouchers!
  await assertSucceeds(
    aliceDb.collection("app_config").doc("global").update({
      announcementText: "Updated live by Co-Admin!",
      updatedByEmail: "alice@example.com",
      updatedAt: nowPast(1000),
    })
  );
  await assertSucceeds(aliceDb.collection("users").get());
  await assertSucceeds(
    aliceDb.collection("virtual_vouchers").doc("CP-COADMIN-500").set({
      code: "CP-COADMIN-500",
      coinsAmount: 500,
      label: "Co-Admin Gift QR",
      publicVisible: true,
      redeemed: false,
      createdByEmail: "alice@example.com",
      createdAt: nowPast(1000),
      updatedAt: nowPast(1000),
    })
  );

  // Co-Admin CANNOT create new Co-Admin invites (Super Admin only!)
  const secondHash = sha256Hex("SneakyPass");
  await assertFails(
    aliceDb.collection("co_admin_invites").doc(secondHash).set({
      label: "Unauthorized Invite",
      active: true,
      createdByEmail: "alice@example.com",
      createdAt: nowPast(1000),
      updatedAt: nowPast(1000),
    })
  );

  // Super Admin revokes the Co-Admin password -> Alice loses Admin access IMMEDIATELY
  await assertSucceeds(
    superDb.collection("co_admin_invites").doc(inviteHash).update({
      active: false,
      updatedAt: nowPast(500),
    })
  );
  await assertFails(
    aliceDb.collection("app_config").doc("global").update({
      announcementText: "Should be blocked!",
      updatedAt: nowPast(200),
    })
  );
});

test("Player voucher redemption and Kotlin-aligned filtered queries succeed", async () => {
  const superDb = testEnv
    .authenticatedContext(SUPER_ADMIN_UID, {
      email: SUPER_ADMIN_EMAIL,
      email_verified: true,
    })
    .firestore();
  const bobDb = testEnv
    .authenticatedContext(BOB_UID, {
      email: "bob@example.com",
      email_verified: true,
    })
    .firestore();

  const ts = nowPast(4000);
  await assertSucceeds(
    superDb.collection("virtual_vouchers").doc("CP-WELCOME-500").set({
      code: "CP-WELCOME-500",
      coinsAmount: 500,
      label: "Welcome Gift QR",
      publicVisible: true,
      redeemed: false,
      createdByEmail: SUPER_ADMIN_EMAIL,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Filtered query matching Kotlin repository succeeds; unfiltered list fails for normal player
  await assertSucceeds(
    bobDb.collection("virtual_vouchers").where("publicVisible", "==", true).get()
  );
  await assertFails(bobDb.collection("virtual_vouchers").get());

  // Bob redeems voucher once
  await assertSucceeds(
    bobDb.collection("virtual_vouchers").doc("CP-WELCOME-500").update({
      redeemed: true,
      redeemedByUid: BOB_UID,
      redeemedAt: nowPast(1000),
      updatedAt: nowPast(1000),
    })
  );

  // Double-redemption fails
  await assertFails(
    bobDb.collection("virtual_vouchers").doc("CP-WELCOME-500").update({
      redeemed: true,
      redeemedByUid: BOB_UID,
      redeemedAt: nowPast(500),
      updatedAt: nowPast(500),
    })
  );

  // Leaderboard & game_rounds Kotlin-aligned filtered queries succeed
  await assertSucceeds(
    bobDb.collection("leaderboard").where("totalVirtualPoints", ">=", 0).get()
  );
  await assertSucceeds(
    bobDb.collection("game_rounds").where("status", "==", "COMPLETED").get()
  );
  await assertFails(bobDb.collection("game_rounds").get());
});

test("Multi-bet predictions (Color, Number, Big/Small) and Deposit/Withdrawal wallet_requests enforce security rules", async () => {
  const superDb = testEnv
    .authenticatedContext(SUPER_ADMIN_UID, {
      email: SUPER_ADMIN_EMAIL,
      email_verified: true,
    })
    .firestore();
  const aliceDb = testEnv
    .authenticatedContext(ALICE_UID, {
      email: "alice@example.com",
      email_verified: true,
    })
    .firestore();
  const bobDb = testEnv
    .authenticatedContext(BOB_UID, {
      email: "bob@example.com",
      email_verified: true,
    })
    .firestore();

  const ts = nowPast(4000);

  // Alice places multiple bets in the same round: Number 7 + Big + Green
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("predictions").doc("mode_30s_20261122-30S-00001_1").set({
      userId: ALICE_UID,
      modeId: "mode_30s",
      periodId: "20261122-30S-00001",
      betType: "NUMBER",
      betLabel: "Number 7",
      selectedColor: "GREEN",
      selectedDigit: 7,
      stakeCoins: 100,
      status: "PENDING",
      rewardCoins: 0,
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("predictions").doc("mode_30s_20261122-30S-00001_2").set({
      userId: ALICE_UID,
      modeId: "mode_30s",
      periodId: "20261122-30S-00001",
      betType: "SIZE",
      betLabel: "Big (5-9)",
      selectedColor: "GREEN",
      selectedSize: "BIG",
      stakeCoins: 200,
      status: "PENDING",
      rewardCoins: 0,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Alice submits a PENDING Deposit Request via Admin QR
  await assertSucceeds(
    aliceDb.collection("wallet_requests").doc("dep_alice_001").set({
      userId: ALICE_UID,
      userName: "Alice Star",
      userEmail: "alice@example.com",
      publicUserId: "CP-111111",
      requestType: "DEPOSIT",
      amountCoins: 500,
      paymentMethod: "QR_UPI",
      paymentDetails: "UTR412345678901",
      status: "PENDING",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Alice CANNOT create a pre-approved deposit request
  await assertFails(
    aliceDb.collection("wallet_requests").doc("dep_alice_hack").set({
      userId: ALICE_UID,
      userName: "Alice Star",
      userEmail: "alice@example.com",
      publicUserId: "CP-111111",
      requestType: "DEPOSIT",
      amountCoins: 50000,
      paymentMethod: "QR_UPI",
      paymentDetails: "FAKEUTR123",
      status: "APPROVED",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Alice CANNOT self-approve her own request
  await assertFails(
    aliceDb.collection("wallet_requests").doc("dep_alice_001").update({
      status: "APPROVED",
      updatedAt: nowPast(1000),
    })
  );

  // Bob CANNOT read Alice's deposit request or list all requests
  await assertFails(bobDb.collection("wallet_requests").doc("dep_alice_001").get());
  await assertFails(bobDb.collection("wallet_requests").get());

  // Alice CAN list her own requests using Kotlin-aligned whereEqualTo("userId", uid)
  await assertSucceeds(
    aliceDb.collection("wallet_requests").where("userId", "==", ALICE_UID).get()
  );

  // Super Admin CAN list all requests and approve Alice's deposit
  await assertSucceeds(superDb.collection("wallet_requests").get());
  await assertSucceeds(
    superDb.collection("wallet_requests").doc("dep_alice_001").update({
      status: "APPROVED",
      adminNote: "QR UTR verified & credited",
      processedByEmail: SUPER_ADMIN_EMAIL,
      updatedAt: nowPast(1000),
    })
  );
});

test("7. Admin per-arena forced digits + Spin Wheel control + Referral +500 VC Collect rules", async () => {
  const superDb = testEnv
    .authenticatedContext(SUPER_ADMIN_UID, {
      email: SUPER_ADMIN_EMAIL,
      email_verified: true,
    })
    .firestore();
  const aliceDb = testEnv
    .authenticatedContext(ALICE_UID, {
      email: "alice@example.com",
      email_verified: true,
    })
    .firestore();
  const bobDb = testEnv
    .authenticatedContext(BOB_UID, {
      email: "bob@example.com",
      email_verified: true,
    })
    .firestore();

  const ts = nowPast(3000);

  // Super Admin sets exact next digit 5 for 30s, 8 for 60s, and Spin Wheel slice 3 (500 VC)
  await assertSucceeds(
    superDb.collection("app_config").doc("global").set({
      announcementText: "Live test announcement",
      redGreenMultiplier: 2.0,
      violetMultiplier: 4.5,
      mode30sEnabled: true,
      mode60sEnabled: true,
      mode3mEnabled: true,
      mode5mEnabled: true,
      forced30sDigit: 5,
      forced60sDigit: 8,
      forced3mDigit: null,
      forced5mDigit: null,
      forcedSpinSliceIndex: 3,
      adminQrLabel: "ChromaPulse Counter",
      adminQrPayload: "admin@ybl",
      updatedByEmail: SUPER_ADMIN_EMAIL,
      updatedAt: ts,
    })
  );

  // Regular player CANNOT modify app_config forced digits
  await assertFails(
    aliceDb.collection("app_config").doc("global").update({
      forced30sDigit: 9,
      updatedAt: nowPast(1000),
    })
  );

  // Alice registers her shareable referral code CP-ALICE1
  await assertSucceeds(
    aliceDb.collection("referral_codes").doc("CP-ALICE1").set({
      code: "CP-ALICE1",
      ownerUid: ALICE_UID,
      ownerName: "Alice Star",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Alice CANNOT refer herself
  await assertFails(
    aliceDb.collection("referrals").doc(ALICE_UID).set({
      referredUid: ALICE_UID,
      referredName: "Alice Star",
      referrerUid: ALICE_UID,
      referrerCode: "CP-ALICE1",
      referrerName: "Alice Star",
      bonusCoins: 500,
      referrerRewardClaimed: false,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Bob applies Alice's code CP-ALICE1 to claim +500 VC welcome bonus
  await assertSucceeds(
    bobDb.collection("referrals").doc(BOB_UID).set({
      referredUid: BOB_UID,
      referredName: "Bob Runner",
      referrerUid: ALICE_UID,
      referrerCode: "CP-ALICE1",
      referrerName: "Alice Star",
      bonusCoins: 500,
      referrerRewardClaimed: false,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Bob CANNOT list Alice's referrals or collect Alice's reward
  await assertFails(bobDb.collection("referrals").where("referrerUid", "==", ALICE_UID).get());
  await assertFails(
    bobDb.collection("referrals").doc(BOB_UID).update({
      referrerRewardClaimed: true,
      updatedAt: nowPast(1000),
    })
  );

  // Alice CAN list her own referrals and collect her +500 VC reward once
  await assertSucceeds(aliceDb.collection("referrals").where("referrerUid", "==", ALICE_UID).get());
  await assertSucceeds(
    aliceDb.collection("referrals").doc(BOB_UID).update({
      referrerRewardClaimed: true,
      updatedAt: nowPast(1000),
    })
  );

  // Alice CANNOT re-collect the same referral twice
  await assertFails(
    aliceDb.collection("referrals").doc(BOB_UID).update({
      referrerRewardClaimed: true,
      updatedAt: nowPast(500),
    })
  );
});


