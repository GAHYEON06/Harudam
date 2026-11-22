const { test } = require("node:test");
const assert = require("node:assert/strict");
const { parsePlaces } = require("./parse-places");
const url = "https://map.naver.com/p/entry/place/123";
const entity = { "@type": "Restaurant", name: "식당 & 카페", address: { addressRegion: "부산", streetAddress: "중앙로 1" }, geo: { latitude: "35.1", longitude: "129.1" } };
const html = (data = entity) => `<script type="application/ld+json">${JSON.stringify(data)}</script>`;
const page = (body = html()) => new Response(body, { headers: { "Content-Type": "text/html; charset=utf-8" } });
const run = (data, fetchImpl = async () => page()) => parsePlaces(data, { fetchImpl });

test("single URL yields CRUD-compatible preview without invented ID", async () => {
  const result = await run({ url });
  assert.equal(result.successCount, 1);
  assert.deepEqual(result.results[0].place, { name: "식당 & 카페", address: "부산 중앙로 1", category: "FOOD", sourceUrl: url, latitude: 35.1, longitude: 129.1, visitDate: null, visitOrder: null, note: "", visitTime: null });
  assert.equal(result.results[0].requiresConfirmation, true);
  assert.equal(result.results[0].providerPlaceId, "123");
});
test("batch preserves order and duplicates but fetches identical input once", async () => {
  let calls = 0;
  const r = await run({ urls: [url, "https://example.com", url] }, async () => { calls++; return page(); });
  assert.equal(calls, 1);
  assert.equal(r.successCount, 2);
  assert.equal(r.errorCount, 1);
  assert.deepEqual(r.results.map((r) => r.status), ["success", "error", "success"]);
});
test("invalid top-level shapes rejected", async () => {
  for (const data of [{}, { url, urls: [url] }, { urls: [] }, { urls: "x" }, { url: 123 }, { urls: Array(11).fill(url) }, { url: " " }, { url: "a".repeat(501) }]) {
    await assert.rejects(() => run(data), (e) => e.code === "invalid-argument");
  }
});
test("untrusted hosts, credentials, schemes and ports cause no request", async () => {
  for (const u of ["http://127.0.0.1", "https://map.naver.com.evil.test/p/entry/place/1", "https://evilnaver.com", "file:///tmp/x", "https://a:b@map.naver.com/p/entry/place/1", "https://map.naver.com:8443", "not a url"]) {
    const r = await run({ url: u }, async () => { assert.fail("network must not be used"); });
    assert.equal(r.errorCount, 1, u);
  }
});
test("shortlink resolves with manual redirects and HTTPS upgrade", async () => {
  const seen = [];
  const r = await run({ url: "http://naver.me/abc" }, async (u, opts) => {
    seen.push(u);
    assert.equal(opts.redirect, "manual");
    return seen.length === 1 ? new Response(null, { status: 302, headers: { location: url } }) : page();
  });
  assert.equal(r.successCount, 1);
  assert.deepEqual(seen, ["https://naver.me/abc", url]);
  assert.equal(r.results[0].place.sourceUrl, "http://naver.me/abc");
});
test("redirect to private host blocked before second request", async () => {
  let calls = 0;
  const r = await run({ url }, async () => { calls++; return new Response(null, { status: 302, headers: { location: "http://169.254.169.254" } }); });
  assert.equal(calls, 1);
  assert.equal(r.results[0].error.code, "UNSUPPORTED_URL");
});
test("redirect loops bounded", async () => {
  let calls = 0;
  const r = await run({ url }, async () => { calls++; return new Response(null, { status: 302, headers: { location: url } }); });
  assert.equal(calls, 5);
  assert.equal(r.results[0].error.code, "REDIRECT_ERROR");
});
test("saved list and search are explicit unsupported pages", async () => {
  for (const u of ["https://map.naver.com/p/favorite/myPlace/folder/1", "https://map.naver.com/p/search/cafe"]) {
    const r = await run({ url: u });
    assert.equal(r.results[0].error.code, "UNSUPPORTED_PAGE");
  }
});
test("OG fallback warns about missing address, coordinates and category", async () => {
  const r = await run({ url }, async () => page('<meta content="바다 &amp; 공원 : 네이버 지도" property="og:title">'));
  assert.equal(r.results[0].place.name, "바다 & 공원");
  assert.equal(r.results[0].place.latitude, null);
  assert.equal(r.results[0].warnings.length, 3);
});
test("generic title and malformed JSON never become sample places", async () => {
  for (const body of ["<title>네이버 지도</title>", '<meta property="og:title" content="네이버 지도">', '<script type="application/ld+json">{broken</script>']) {
    const r = await run({ url }, async () => page(body));
    assert.equal(r.results[0].error.code, "METADATA_UNAVAILABLE");
    assert.equal(r.results[0].place, undefined);
  }
});
test("schema graph and categories; invalid coordinate pair becomes null", async () => {
  for (const [type, expected] of [["Hotel", "STAY"], ["Museum", "TOUR"], ["Place", "OTHER"]]) {
    const r = await run({ url }, async () => page(html({ "@graph": [{ ...entity, "@type": type, geo: { latitude: 999, longitude: 1 } }] })));
    assert.equal(r.results[0].place.category, expected);
    assert.equal(r.results[0].place.latitude, null);
    assert.equal(r.results[0].place.longitude, null);
  }
});
test("HTTP errors, content type, network failures and size limits", async () => {
  const cases = [
    [async () => new Response("blocked", { status: 403 }), "FETCH_FAILED"],
    [async () => new Response("limited", { status: 429 }), "PROVIDER_RATE_LIMITED"],
    [async () => new Response("{}", { headers: { "content-type": "application/json" } }), "UNSUPPORTED_CONTENT"],
    [async () => { throw new Error("secret upstream details"); }, "FETCH_FAILED"],
    [async () => new Response("x", { headers: { "content-type": "text/html", "content-length": "2000000" } }), "PAGE_TOO_LARGE"],
    [async () => page("x".repeat(1048577)), "PAGE_TOO_LARGE"],
  ];
  for (const [fetcher, code] of cases) {
    const r = await run({ url }, fetcher);
    assert.equal(r.results[0].error.code, code);
    assert.ok(!JSON.stringify(r).includes("secret upstream"));
  }
});
test("desktop JavaScript shell falls back to public place metadata", async () => {
  const seen = [];
  const r = await run({ url }, async (u) => { seen.push(u); return page(seen.length === 1 ? "<title>네이버 지도</title>" : html()); });
  assert.equal(r.successCount, 1);
  assert.deepEqual(seen, [url, "https://m.place.naver.com/place/123/home"]);
  assert.equal(r.results[0].place.sourceUrl, url);
});
test("shared deadline aborts slow fetches and remaining batch items", async () => {
  const keepAlive = setTimeout(() => {}, 1000);
  try {
    const r = await parsePlaces({ urls: [url, `${url}?a=1`, `${url}?a=2`] }, {
      timeoutMs: 10,
      fetchImpl: async (_, { signal }) => {
        if (signal.aborted) throw signal.reason;
        await new Promise((_, reject) => signal.addEventListener("abort", () => reject(signal.reason), { once: true }));
      },
    });
    assert.equal(r.errorCount, 3);
    assert.ok(r.results.every((r) => r.error.code === "TIMEOUT"));
  } finally { clearTimeout(keepAlive); }
});
test("at most two fetches run together", async () => {
  let active = 0;
  let peak = 0;
  const r = await run({ urls: Array.from({ length: 10 }, (_, i) => `${url}?n=${i}`) }, async () => {
    active++; peak = Math.max(peak, active);
    await new Promise((r) => setTimeout(r, 5)); active--;
    return page();
  });
  assert.equal(r.successCount, 10);
  assert.equal(peak, 2);
});

test("boolean, blank, array and object coordinates are never coerced into numbers", async () => {
  for (const value of [true, false, " ", [], [35], {}, "0x23", "Infinity"]) {
    const r = await run({ url }, async () => page(html({ ...entity, geo: { latitude: value, longitude: "129.1" } })));
    assert.equal(r.results[0].place.latitude, null, JSON.stringify(value));
    assert.equal(r.results[0].place.longitude, null);
  }
});

test("schema type must be an exact supported type, not a substring", async () => {
  const r = await run({ url }, async () => page(html({ ...entity, "@type": "NotAPlace" })));
  assert.equal(r.results[0].error.code, "METADATA_UNAVAILABLE");
});

test("schema URL matching target place takes priority over unrelated graph nodes", async () => {
  const r = await run({ url }, async () => page(html({ "@graph": [
    { ...entity, name: "다른 식당", url: "https://m.place.naver.com/restaurant/999/home" },
    { ...entity, name: "일치하는 식당", url: "https://m.place.naver.com/restaurant/123/home" },
  ] })));
  assert.equal(r.results[0].place.name, "일치하는 식당");
});

test("schema explicitly identifying another place is not imported", async () => {
  const r = await run({ url }, async () => page(html({ ...entity, url: "https://m.place.naver.com/restaurant/999/home" })));
  assert.equal(r.results[0].error.code, "METADATA_UNAVAILABLE");
});

test("standard schema type URL and array are supported", async () => {
  const r = await run({ url }, async () => page(html({ ...entity, "@type": ["https://schema.org/Restaurant", "LocalBusiness"] })));
  assert.equal(r.results[0].place.category, "FOOD");
});

test("matching schema in a later script wins over an unidentified earlier node", async () => {
  const r = await run({ url }, async () => page(
    html({ ...entity, name: "식별 안 된 장소" }) + html({ ...entity, name: "대상 장소", "@id": `${url}#business` })
  ));
  assert.equal(r.results[0].place.name, "대상 장소");
});

test("known legacy mobile and desktop place URLs identify numeric code", async () => {
  for (const u of ["https://m.map.naver.com/search2/site.naver?code=123", "https://map.naver.com/local/siteview.nhn?code=123"]) {
    const r = await run({ url: u });
    assert.equal(r.successCount, 1);
    assert.equal(r.results[0].providerPlaceId, "123");
  }
});

test("an arbitrary id query must not turn a saved list or shortlink into a place", async () => {
  for (const u of ["https://map.naver.com/p/favorite/myPlace/folder/1?id=123", "https://naver.me/example?id=123"]) {
    const r = await run({ url: u });
    assert.equal(r.results[0].error.code, "UNSUPPORTED_PAGE");
  }
});

test("zero, negative and decimal coordinate strings remain valid", async () => {
  for (const [lat, lng] of [[0, 0], ["-35.1", "+129.1"], [" .5 ", "0"]]) {
    const r = await run({ url }, async () => page(html({ ...entity, geo: { latitude: lat, longitude: lng } })));
    assert.equal(r.results[0].place.latitude, Number(lat));
    assert.equal(r.results[0].place.longitude, Number(lng));
  }
});
