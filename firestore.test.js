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

test("Unauthenticated user cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("documents").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("documents").doc("doc_1").set({
      userId: ALICE_UID,
      title: "Giáo trình",
      chapterNumber: 1,
      sourceType: "NOTE",
      contentText: "Nội dung",
      summary: "Tóm tắt",
      keyConcepts: ["Hàng hóa"],
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Owner can create, list, and partially update StudyDocument; invalid payloads & cross-user fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 5000);

  const docRef = aliceDb.collection("users").doc(ALICE_UID).collection("documents").doc("doc_ch2");
  await assertSucceeds(
    docRef.set({
      userId: ALICE_UID,
      title: "Chương 2 - Hàng hóa và thị trường",
      chapterNumber: 2,
      sourceType: "PDF_FILE",
      contentText: "Hàng hóa có hai thuộc tính là giá trị sử dụng và giá trị.",
      summary: "Tóm tắt lý luận giá trị lao động.",
      keyConcepts: ["Giá trị sử dụng", "Giá trị"],
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("documents").get());

  await assertSucceeds(
    docRef.update({
      summary: "Tóm tắt cập nhật về quy luật giá trị.",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject immutable createdAt mutation
  await assertFails(
    docRef.update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject @DocumentId 'id' field in payload
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("documents").doc("doc_bad").set({
      id: "doc_bad",
      userId: ALICE_UID,
      title: "Bad doc",
      chapterNumber: 1,
      sourceType: "NOTE",
      contentText: "Text",
      summary: "Summary",
      keyConcepts: [],
      createdAt: now,
      updatedAt: now,
    })
  );

  // Reject cross-user read and write
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("documents").doc("doc_ch2").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("documents").get());
});

test("Owner can create TutorSession and TutorMessage with parent gate; future timestamp fails", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date(Date.now() - 5000);

  const sessionRef = aliceDb.collection("users").doc(ALICE_UID).collection("sessions").doc("sess_1");
  await assertSucceeds(
    sessionRef.set({
      userId: ALICE_UID,
      title: "Hỏi đáp Giá trị thặng dư",
      mode: "SOCRATIC",
      modelTier: "FLASH",
      chapterNumber: 3,
      linkedDocumentId: "",
      linkedDocumentTitle: "",
      lastMessagePreview: "Công thức m' là gì?",
      messageCount: 1,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    sessionRef.collection("messages").doc("msg_1").set({
      userId: ALICE_UID,
      role: "ASSISTANT",
      content: "Theo em, trong tư bản ứng trước c + v, bộ phận nào trực tiếp tạo ra giá trị mới?",
      mode: "SOCRATIC",
      modelUsed: "gemini-3.8-flash",
      hasAttachment: false,
      attachmentLabel: "",
      hintChips: ["Tư bản khả biến v", "Tư bản bất biến c"],
      createdAt: new Date(Date.now() - 2000),
    })
  );

  // Orphan message without parent session fails
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("sessions").doc("missing_sess")
      .collection("messages").doc("msg_orphan").set({
        userId: ALICE_UID,
        role: "USER",
        content: "Xin chào",
        mode: "SOCRATIC",
        modelUsed: "gemini-3.8-flash",
        hasAttachment: false,
        attachmentLabel: "",
        hintChips: [],
        createdAt: now,
      })
  );

  // Tampered future timestamp fails
  await assertFails(
    sessionRef.collection("messages").doc("msg_future").set({
      userId: ALICE_UID,
      role: "USER",
      content: "Tương lai",
      mode: "SOCRATIC",
      modelUsed: "gemini-3.8-flash",
      hasAttachment: false,
      attachmentLabel: "",
      hintChips: [],
      createdAt: new Date(Date.now() + 60000),
    })
  );
});

test("Flashcards, OralExamAttempts, and ChapterProgress enforce schema bounds", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date(Date.now() - 5000);

  const cardRef = aliceDb.collection("users").doc(ALICE_UID).collection("flashcards").doc("card_1");
  await assertSucceeds(
    cardRef.set({
      userId: ALICE_UID,
      documentId: "doc_ch2",
      chapterNumber: 3,
      frontTerm: "Tỷ suất giá trị thặng dư (m')",
      backDefinition: "Tỷ lệ phần trăm giữa giá trị thặng dư và tư bản khả biến.",
      formula: "m' = (m / v) × 100%",
      example: "Nếu v = 100, m = 100 thì m' = 100%",
      mastered: false,
      reviewCount: 0,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    cardRef.update({
      mastered: true,
      reviewCount: 1,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Oral attempt valid vs out-of-range score
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("oralAttempts").doc("oral_1").set({
      userId: ALICE_UID,
      chapterNumber: 3,
      documentId: "",
      question: "Phân biệt giá trị thặng dư tuyệt đối và tương đối?",
      userAnswer: "Tuyệt đối kéo dài ngày lao động, tương đối rút ngắn thời gian lao động tất yếu.",
      score: 8.5,
      strengths: ["Phân biệt đúng bản chất hai phương pháp"],
      gaps: ["Cần nêu thêm ví dụ về tăng năng suất lao động xã hội"],
      modelFeedback: "Câu trả lời chính xác trọng tâm.",
      createdAt: now,
    })
  );

  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("oralAttempts").doc("oral_bad").set({
      userId: ALICE_UID,
      chapterNumber: 3,
      documentId: "",
      question: "Câu hỏi?",
      userAnswer: "Trả lời",
      score: 12.0,
      strengths: [],
      gaps: [],
      modelFeedback: "Lỗi điểm",
      createdAt: now,
    })
  );

  // Chapter progress valid
  const progRef = aliceDb.collection("users").doc(ALICE_UID).collection("chapterProgress").doc("chapter_3");
  await assertSucceeds(
    progRef.set({
      userId: ALICE_UID,
      chapterNumber: 3,
      sessionsCompleted: 1,
      oralBestScore: 8.5,
      masteryPercent: 45,
      createdAt: now,
      updatedAt: now,
    })
  );
});
