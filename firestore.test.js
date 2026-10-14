const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";

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

test("Public user (no auth): can read categories and designs but cannot write", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("categories").doc("cat1").set({
      id: "cat1",
      name: "৩ডি ওয়ালপেপার",
      slug: "3d-wallpaper",
      imageUrl: "https://example.com/cat.jpg",
    });
    await context.firestore().collection("designs").doc("des1").set({
      id: "des1",
      name: "রয়্যাল গোল্ডেন লোটাস ৩ডি",
      category: "৩ডি ওয়ালপেপার",
      subcategory: "বেডরুম",
      imageUrl: "https://example.com/des.jpg",
    });
  });

  const publicDb = testEnv.unauthenticatedContext().firestore();
  await assertSucceeds(publicDb.collection("categories").get());
  await assertSucceeds(publicDb.collection("designs").get());
  await assertFails(publicDb.collection("categories").doc("cat2").set({
    id: "cat2",
    name: "৩ডি সিলিং",
    slug: "3d-ceiling",
    imageUrl: "https://example.com/cat2.jpg",
  }));
  await assertFails(publicDb.collection("designs").doc("des2").set({
    id: "des2",
    name: "নতুন ডিজাইন",
    category: "৩ডি ওয়ালপেপার",
    subcategory: "বেডরুম",
    imageUrl: "https://example.com/des2.jpg",
  }));
});

test("Public user (no auth): can read subcategories and app settings but cannot write", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("subcategories").doc("sub_bedroom").set({
      id: "sub_bedroom",
      name: "বেডরুম",
      nameEn: "Bedroom",
      slug: "bedroom",
      order: 1,
    });
    await context.firestore().collection("settings").doc("app").set({
      phoneNumber: "+8801976600300",
      phoneDisplay: "০১৯৭৬-৬০০৩০০",
      whatsappNumber: "8801976600300",
      heroTitle: "আর এস ৩ডি ওয়ালপেপার অ্যান্ড ফ্লোর",
      heroSubtitle: "৩ডি ওয়ালপেপার",
      heroDescription: "সারা বাংলাদেশে ডেলিভারি",
      heroImageUrl: "https://example.com/hero.jpg",
    });
  });

  const publicDb = testEnv.unauthenticatedContext().firestore();
  await assertSucceeds(publicDb.collection("subcategories").get());
  await assertSucceeds(publicDb.collection("settings").doc("app").get());
  await assertFails(publicDb.collection("subcategories").doc("sub_office").set({
    id: "sub_office",
    name: "অফিস",
    slug: "office",
  }));
  await assertFails(publicDb.collection("settings").doc("app").set({
    phoneNumber: "+8801976600300",
    whatsappNumber: "8801976600300",
    heroTitle: "Public write must be denied",
    heroImageUrl: "",
  }));
});

test("Secrets stay private: settings/integrations is Admin-SDK-only", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("settings").doc("integrations").set({
      cloudinary: { cloudName: "x", apiKey: "y", apiSecret: "z", folder: "rs-decor" },
    });
  });

  const publicDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(publicDb.collection("settings").doc("integrations").get());
  await assertFails(publicDb.collection("settings").doc("integrations").set({ hacked: true }));
});

test("Public user (no auth): invalid subcategory/settings writes are rejected", async () => {
  const publicDb = testEnv.unauthenticatedContext().firestore();
  // Blank slug must not become a filter pill
  await assertFails(
    publicDb.collection("subcategories").doc("bad").set({
      id: "bad",
      name: "Broken Room",
      slug: "",
    })
  );
  // Missing contact numbers must not blank out the call buttons
  await assertFails(
    publicDb.collection("settings").doc("app").set({
      heroTitle: "Hacked",
    })
  );
});
