"use strict";

const { test } = require("node:test");
const assert = require("node:assert/strict");
const { travelApi } = require("./index");
const { getFirestore } = require("firebase-admin/firestore");

const url = "https://m.place.naver.com/restaurant/123/home";
function providerPage() {
  return new Response('<script type="application/ld+json">' + JSON.stringify({
    "@type": "Restaurant", name: "테스트 식당", url,
    address: "테스트 주소", geo: { latitude: "35.1", longitude: "129.1" },
  }) + '</script>', { headers: { "content-type": "text/html" } });
}
function call(data, auth = { uid: "test-user" }) {
  // Runs the exported Callable handler; transport/token verification is covered by the emulator suite.
  return travelApi.run({ auth, data });
}

test("Callable parsePlaces returns success using the exported handler without database access", async (t) => {
  t.mock.method(globalThis, "fetch", async () => providerPage());
  t.mock.method(getFirestore(), "collection", () => assert.fail("preview must not read or write Firestore"));
  t.mock.method(getFirestore(), "runTransaction", () => assert.fail("preview must not start transactions"));
  const result = await call({ action: "parsePlaces", url });
  assert.equal(result.successCount, 1);
  assert.equal(result.results[0].place.name, "테스트 식당");
  assert.equal(result.results[0].place.sourceUrl, url);
  assert.equal(result.results[0].place.category, "FOOD");
  assert.equal(result.results[0].place.latitude, 35.1);
  assert.equal(result.results[0].requiresConfirmation, true);
});

test("Callable authentication rejects before any provider request", async (t) => {
  t.mock.method(globalThis, "fetch", () => assert.fail("unauthenticated request must not reach provider"));
  await assert.rejects(() => call({ action: "parsePlaces", url }, null), (e) => e.code === "unauthenticated");
});

test("Callable request validation rejects malformed inputs before any provider request", async (t) => {
  t.mock.method(globalThis, "fetch", () => assert.fail("invalid request must not reach provider"));
  for (const data of [null, [], { action: "parsePlaces", urls: [] }, { action: "parsePlaces", url, urls: [url] }]) {
    await assert.rejects(() => call(data), (e) => e.code === "invalid-argument");
  }
});

test("Callable batch returns successful and unsupported URL items in input order", async (t) => {
  let requests = 0;
  t.mock.method(globalThis, "fetch", async () => { requests++; return providerPage(); });
  const result = await call({ action: "parsePlaces", urls: [url, "https://example.com/place", url] });
  assert.equal(requests, 1);
  assert.equal(result.successCount, 2);
  assert.equal(result.errorCount, 1);
  assert.deepEqual(result.results.map((r) => r.status), ["success", "error", "success"]);
});

test("Callable provider rate limit returns an item error without sample places", async (t) => {
  t.mock.method(globalThis, "fetch", async () => new Response("limited", { status: 429 }));
  const result = await call({ action: "parsePlaces", url });
  assert.equal(result.successCount, 0);
  assert.equal(result.results[0].error.code, "PROVIDER_RATE_LIMITED");
  assert.equal(result.results[0].place, undefined);
});
