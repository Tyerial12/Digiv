const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_tamer_01";
const BOB_UID = "bob_tamer_02";

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

function buildValidCreature(userId) {
  const now = new Date();
  return {
    userId,
    nickname: "NovaAgumon",
    speciesId: "agumon",
    speciesName: "Agumon",
    stage: "Rookie",
    attribute: "Vaccine",
    health: 95,
    stamina: 80,
    hunger: 85,
    evolutionProgress: 30,
    attack: 45,
    defense: 35,
    speed: 40,
    wisdom: 30,
    bondLevel: 25,
    careMistakes: 0,
    isResting: false,
    unlockedForms: ["agumon"],
    lastUpdatedMs: Date.now(),
    createdAt: now,
    updatedAt: now,
  };
}

function buildValidTradeListing(creatorUserId) {
  const now = new Date();
  return {
    creatorUserId,
    creatorTrainerName: "TamerAlice",
    offeredCreatureId: "creature_trade_1",
    offeredNickname: "BlazeGreymon",
    offeredSpeciesId: "greymon",
    offeredSpeciesName: "Greymon",
    offeredStage: "Champion",
    offeredAttribute: "Vaccine",
    offeredLevel: 36,
    offeredAttack: 98,
    offeredDefense: 86,
    offeredSpeed: 74,
    offeredWisdom: 68,
    offeredBondLevel: 45,
    requestedStageOrSpecies: "Champion",
    status: "OPEN",
    accepterUserId: "",
    accepterTrainerName: "",
    tradedInNickname: "",
    tradedInSpeciesId: "",
    tradedInSpeciesName: "",
    tradedInStage: "Rookie",
    tradedInAttribute: "Vaccine",
    tradedInLevel: 1,
    tradedInAttack: 50,
    tradedInDefense: 50,
    tradedInSpeed: 50,
    tradedInWisdom: 50,
    tradedInBondLevel: 20,
    claimedByCreator: false,
    createdAt: now,
    updatedAt: now,
  };
}

test("Unauthenticated user: cannot read creatures, logs, notifications, leaderboard, or tradeListings", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("creatures").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("trainingLogs").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("notifications").get());
  await assertFails(unauthDb.collection("leaderboard").where("leagueId", "==", "global").get());
  await assertFails(unauthDb.collection("tradeListings").where("status", "==", "OPEN").get());
});

test("Authenticated owner: can create, read, and update own creature", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const creatureRef = aliceDb.collection("users").doc(ALICE_UID).collection("creatures").doc("creature_1");
  await assertSucceeds(creatureRef.set(buildValidCreature(ALICE_UID)));
  await assertSucceeds(creatureRef.get());
  await assertSucceeds(
    creatureRef.update({
      stamina: 65,
      evolutionProgress: 45,
      updatedAt: new Date(),
    })
  );
});

test("Cross-user isolation: Bob cannot read or modify Alice's creatures", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context
      .firestore()
      .collection("users")
      .doc(ALICE_UID)
      .collection("creatures")
      .doc("creature_1")
      .set(buildValidCreature(ALICE_UID));
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const aliceCreatureRef = bobDb.collection("users").doc(ALICE_UID).collection("creatures").doc("creature_1");
  await assertFails(aliceCreatureRef.get());
  await assertFails(aliceCreatureRef.update({ health: 10 }));
});

test("Shadow update rejection: cannot inject undeclared ghost field into creature", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const creatureRef = aliceDb.collection("users").doc(ALICE_UID).collection("creatures").doc("creature_1");
  const badPayload = {
    ...buildValidCreature(ALICE_UID),
    isAdminHack: true,
  };
  await assertFails(creatureRef.set(badPayload));
});

test("Training logs: owner can create and list own logs, cannot update immutable log", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const logRef = aliceDb.collection("users").doc(ALICE_UID).collection("trainingLogs").doc("log_1");
  const now = new Date();
  await assertSucceeds(
    logRef.set({
      userId: ALICE_UID,
      creatureId: "creature_1",
      creatureName: "Agumon",
      activityType: "Monster Trade",
      grade: "EVO",
      statDeltaSummary: "Traded Greymon for Garurumon with TamerBob",
      xpGained: 120,
      dateKey: "2026-11-14",
      createdAt: now,
    })
  );
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("trainingLogs").get());
  await assertFails(logRef.update({ xpGained: 99999 }));
});

test("Notifications: owner can create, list, and mark as read", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const notifRef = aliceDb.collection("users").doc(ALICE_UID).collection("notifications").doc("notif_1");
  const now = new Date();
  await assertSucceeds(
    notifRef.set({
      userId: ALICE_UID,
      creatureId: "creature_1",
      creatureName: "Agumon",
      severity: "WARNING",
      title: "Low Hunger Alert",
      message: "Agumon satiety dropped below 25%. Feed Digi-Meat soon!",
      isRead: false,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(notifRef.update({ isRead: true, updatedAt: new Date() }));
});

test("Leaderboard: owner can write own entry, any authenticated trainer can query with leagueId == 'global', cannot spoof another user", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date();

  const aliceEntryRef = aliceDb.collection("leaderboard").doc(ALICE_UID);
  await assertSucceeds(
    aliceEntryRef.set({
      userId: ALICE_UID,
      leagueId: "global",
      trainerName: "TamerAlice",
      tamerTitle: "Cyber Vanguard",
      partnerName: "WarGreymon",
      partnerStage: "Mega",
      partnerAttribute: "Vaccine",
      totalCombatPower: 4250,
      evolutionsUnlocked: 6,
      trainingStreakDays: 5,
      sparringWins: 12,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(bobDb.collection("leaderboard").where("leagueId", "==", "global").get());

  await assertFails(
    bobDb.collection("leaderboard").doc(ALICE_UID).update({
      totalCombatPower: 10,
      updatedAt: new Date(),
    })
  );
});

test("Trade Listings: Alice posts OPEN trade, Bob queries & accepts with his monster, Alice claims completed trade", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  const tradeRefAlice = aliceDb.collection("tradeListings").doc("trade_01");
  await assertSucceeds(tradeRefAlice.set(buildValidTradeListing(ALICE_UID)));

  // Bob can query OPEN trade listings
  await assertSucceeds(bobDb.collection("tradeListings").where("status", "==", "OPEN").get());

  // Alice cannot accept her own trade listing
  await assertFails(
    tradeRefAlice.update({
      status: "COMPLETED",
      accepterUserId: ALICE_UID,
      accepterTrainerName: "TamerAlice",
      tradedInNickname: "SelfWolf",
      tradedInSpeciesId: "garurumon",
      tradedInSpeciesName: "Garurumon",
      tradedInStage: "Champion",
      tradedInAttribute: "Vaccine",
      tradedInLevel: 34,
      tradedInAttack: 90,
      tradedInDefense: 80,
      tradedInSpeed: 104,
      tradedInWisdom: 78,
      tradedInBondLevel: 40,
      updatedAt: new Date(),
    })
  );

  // Bob accepts Alice's OPEN trade listing with his Garurumon
  const tradeRefBob = bobDb.collection("tradeListings").doc("trade_01");
  await assertSucceeds(
    tradeRefBob.update({
      status: "COMPLETED",
      accepterUserId: BOB_UID,
      accepterTrainerName: "TamerBob",
      tradedInNickname: "FrostGarurumon",
      tradedInSpeciesId: "garurumon",
      tradedInSpeciesName: "Garurumon",
      tradedInStage: "Champion",
      tradedInAttribute: "Vaccine",
      tradedInLevel: 38,
      tradedInAttack: 92,
      tradedInDefense: 82,
      tradedInSpeed: 106,
      tradedInWisdom: 79,
      tradedInBondLevel: 50,
      updatedAt: new Date(),
    })
  );

  // Alice claims the completed trade
  await assertSucceeds(
    tradeRefAlice.update({
      claimedByCreator: true,
      updatedAt: new Date(),
    })
  );

  // Terminal state lock: once claimedByCreator is true, neither Alice nor Bob can mutate it again
  await assertFails(
    tradeRefAlice.update({
      claimedByCreator: false,
      updatedAt: new Date(),
    })
  );
});

test("Player Chat & Friends: unauthenticated users are blocked; authenticated users can send chat messages and manage friendships", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date();

  // Unauthenticated cannot read or send chat or friend requests
  await assertFails(unauthDb.collection("chatMessages").where("channelId", "==", "global").get());
  await assertFails(unauthDb.collection("friendships").get());

  // Alice sends a global chat message
  const msgRef = aliceDb.collection("chatMessages").doc("msg_01");
  await assertSucceeds(
    msgRef.set({
      senderUserId: ALICE_UID,
      senderTrainerName: "TamerAlice",
      senderPartnerName: "WarGreymon",
      senderPartnerStage: "Mega",
      recipientUserId: "",
      channelId: "global",
      messageText: "Looking to spar or trade LVL 100 monsters!",
      createdAt: now,
    })
  );

  // Bob can read global chat messages
  await assertSucceeds(bobDb.collection("chatMessages").where("channelId", "==", "global").get());

  // Chat messages are immutable
  await assertFails(msgRef.update({ messageText: "Edited text" }));

  // Alice sends a friend request to Bob
  const friendDocId = `fr_${ALICE_UID}_${BOB_UID}`;
  const friendRefAlice = aliceDb.collection("friendships").doc(friendDocId);
  await assertSucceeds(
    friendRefAlice.set({
      requesterUserId: ALICE_UID,
      requesterTrainerName: "TamerAlice",
      requesterPartnerName: "WarGreymon",
      recipientUserId: BOB_UID,
      recipientTrainerName: "TamerBob",
      recipientPartnerName: "MetalGarurumon",
      status: "PENDING",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Alice cannot self-accept her own outgoing request
  await assertFails(
    friendRefAlice.update({
      status: "ACCEPTED",
      updatedAt: new Date(),
    })
  );

  // Bob can query his incoming requests and accept
  await assertSucceeds(bobDb.collection("friendships").where("recipientUserId", "==", BOB_UID).get());
  const friendRefBob = bobDb.collection("friendships").doc(friendDocId);
  await assertSucceeds(
    friendRefBob.update({
      status: "ACCEPTED",
      recipientTrainerName: "TamerBob",
      recipientPartnerName: "MetalGarurumon",
      updatedAt: new Date(),
    })
  );

  // Either friend can remove the friendship
  await assertSucceeds(friendRefAlice.delete());
});

