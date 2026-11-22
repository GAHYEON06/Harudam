"use strict";

const { HttpsError } = require("firebase-functions/v2/https");
const HOSTS = new Set(["naver.me", "map.naver.com", "m.map.naver.com", "place.naver.com", "m.place.naver.com", "pcmap.place.naver.com"]);
const MAX_BYTES = 1024 * 1024;

class ParseError extends Error {
  constructor(code, message) { super(message); this.code = code; }
}

function safeUrl(raw, base) {
  let url;
  try { url = new URL(raw, base); } catch { throw new ParseError("INVALID_URL", "올바른 URL을 입력해 주세요."); }
  if (!["http:", "https:"].includes(url.protocol) || !HOSTS.has(url.hostname) ||
      url.username || url.password || url.port) {
    throw new ParseError("UNSUPPORTED_URL", "지원하는 네이버 지도 공유 링크만 사용할 수 있어요.");
  }
  // Upgrade old shared HTTP links; never send plaintext requests.
  url.protocol = "https:";
  url.hash = "";
  if (url.href.length > 500) throw new ParseError("INVALID_URL", "URL은 최대 500자예요.");
  return url;
}

function inputs(data) {
  if ((data.url !== undefined) === (data.urls !== undefined)) {
    throw new HttpsError("invalid-argument", "url 또는 urls 중 하나만 입력해 주세요.");
  }
  const values = data.url !== undefined ? [data.url] : data.urls;
  if (!Array.isArray(values) || !values.length || values.length > 10 ||
      values.some((v) => typeof v !== "string" || !v.trim() || v.trim().length > 500)) {
    throw new HttpsError("invalid-argument", "1~10개 URL을 각각 1~500자로 입력해 주세요.");
  }
  return values.map((v) => v.trim());
}

async function readPage(url, fetchImpl, signal) {
  for (let hop = 0; hop <= 4; hop++) {
    // Validate every redirect, including redirects from naver.me.
    url = safeUrl(url.href);
    const response = await fetchImpl(url.href, { redirect: "manual", signal,
      headers: { Accept: "text/html,application/xhtml+xml", "User-Agent": "Harudam-PlaceParser/1.0" } });
    if ([301, 302, 303, 307, 308].includes(response.status)) {
      await response.body?.cancel();
      const location = response.headers.get("location");
      if (!location || hop === 4) throw new ParseError("REDIRECT_ERROR", "공유 링크 이동을 확인하지 못했어요.");
      url = safeUrl(location, url);
      continue;
    }
    if (!response.ok) {
      await response.body?.cancel();
      if (response.status === 429) throw new ParseError("PROVIDER_RATE_LIMITED", "네이버에서 접근을 제한했어요. 잠시 후 다시 시도하거나 직접 입력해 주세요.");
      throw new ParseError("FETCH_FAILED", "지도 페이지를 불러오지 못했어요.");
    }
    const type = response.headers.get("content-type") || "";
    if (!/^(text\/html|application\/xhtml\+xml)\b/i.test(type)) {
      await response.body?.cancel();
      throw new ParseError("UNSUPPORTED_CONTENT", "HTML 지도 페이지가 아니에요.");
    }
    if (Number(response.headers.get("content-length")) > MAX_BYTES) {
      await response.body?.cancel();
      throw new ParseError("PAGE_TOO_LARGE", "지도 페이지가 너무 커요.");
    }
    const reader = response.body.getReader();
    const chunks = [];
    let size = 0;
    try {
      while (true) {
        const { done, value } = await reader.read();
        if (done) break;
        size += value.byteLength;
        if (size > MAX_BYTES) throw new ParseError("PAGE_TOO_LARGE", "지도 페이지가 너무 커요.");
        chunks.push(Buffer.from(value));
      }
    } finally { await reader.cancel(); }
    return { url, html: Buffer.concat(chunks).toString("utf8") };
  }
}

function clean(value, max) {
  if (typeof value !== "string") return "";
  return value.replace(/&#(x[0-9a-f]+|\d+);/gi, (_, n) => {
    const code = n[0].toLowerCase() === "x" ? parseInt(n.slice(1), 16) : Number(n);
    return code > 0 && code <= 0x10ffff ? String.fromCodePoint(code) : "";
  }).replace(/&quot;/g, '"').replace(/&#39;|&apos;/g, "'").replace(/&lt;/g, "<")
    .replace(/&gt;/g, ">").replace(/&amp;/g, "&").replace(/<[^>]*>/g, "")
    .replace(/\s+/g, " ").trim().slice(0, max);
}

function category(types) {
  if (/Restaurant|CafeOrCoffeeShop|Bakery|BarOrPub|FoodEstablishment/i.test(types)) return "FOOD";
  if (/Hotel|LodgingBusiness|Motel|Hostel|Resort/i.test(types)) return "STAY";
  if (/TouristAttraction|Museum|Park|LandmarksOrHistoricalBuildings|Beach/i.test(types)) return "TOUR";
  return "OTHER";
}

function extract(html, url, sourceUrl) {
  const placeId = url.pathname.match(/\/(?:place|restaurant|cafe|accommodation)\/(\d+)(?:\/|$)/)?.[1] ||
    (/^\d+$/.test(url.searchParams.get("id") || "") ? url.searchParams.get("id") : null);
  if (!placeId) throw new ParseError("UNSUPPORTED_PAGE", "개별 장소 링크를 사용해 주세요. 저장 목록·검색 결과 가져오기는 지원하지 않아요.");
  let entity;
  for (const match of html.matchAll(/<script\b[^>]*type\s*=\s*["']application\/ld\+json["'][^>]*>([\s\S]*?)<\/script\s*>/gi)) {
    let root;
    try { root = JSON.parse(match[1]); } catch { continue; }
    const nodes = Array.isArray(root) ? root : [root];
    const candidates = nodes.flatMap((n) => n && typeof n === "object" ? [n, ...(Array.isArray(n["@graph"]) ? n["@graph"] : [])] : []);
    entity = candidates.find((n) => n && clean(n.name, 100) &&
      /LocalBusiness|Restaurant|CafeOrCoffeeShop|Bakery|BarOrPub|FoodEstablishment|Hotel|LodgingBusiness|Motel|Hostel|Resort|TouristAttraction|Museum|Park|LandmarksOrHistoricalBuildings|Beach|Place/.test(String(n["@type"])));
    if (entity) break;
  }
  const meta = {};
  for (const tag of html.matchAll(/<meta\b[^>]*>/gi)) {
    const attrs = {};
    for (const a of tag[0].matchAll(/([\w:-]+)\s*=\s*(?:"([^"]*)"|'([^']*)')/g)) attrs[a[1].toLowerCase()] = a[2] ?? a[3];
    meta[attrs.property || attrs.name] = attrs.content;
  }
  const name = clean(entity?.name || meta["og:title"], 100).replace(/\s*[:|\-]\s*네이버\s*(지도|플레이스)\s*$/, "");
  if (!name || /^(네이버\s*(지도|플레이스)|NAVER\s*Map|로그인|접근\s*제한|서비스\s*이용.*)$/i.test(name)) {
    throw new ParseError("METADATA_UNAVAILABLE", "장소 정보를 확인하지 못했어요. 이름과 주소를 직접 입력해 주세요.");
  }
  const a = entity?.address;
  const address = clean(typeof a === "string" ? a : a && [a.addressRegion, a.addressLocality, a.streetAddress].filter((v) => typeof v === "string").join(" "), 300);
  const rawLat = entity?.geo?.latitude;
  const rawLng = entity?.geo?.longitude;
  let latitude = rawLat !== undefined && rawLat !== null && rawLat !== "" ? Number(rawLat) : NaN;
  let longitude = rawLng !== undefined && rawLng !== null && rawLng !== "" ? Number(rawLng) : NaN;
  if (!Number.isFinite(latitude) || !Number.isFinite(longitude) || latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) latitude = longitude = null;
  return {
    place: { name, address, category: category(String(entity?.["@type"] || "")), sourceUrl,
      latitude, longitude, visitDate: null, visitOrder: null, note: "", visitTime: null },
    provider: "NAVER", providerPlaceId: placeId, resolvedUrl: url.href,
    warnings: [...(!address ? ["ADDRESS_UNAVAILABLE"] : []), ...(latitude === null ? ["COORDINATES_UNAVAILABLE"] : []),
      ...(category(String(entity?.["@type"] || "")) === "OTHER" ? ["CATEGORY_UNCONFIRMED"] : [])],
    requiresConfirmation: true,
  };
}

async function parsePlaces(data, { fetchImpl = fetch, timeoutMs = 20000 } = {}) {
  const urls = inputs(data);
  // Two workers keep at most two provider requests active; total network deadline < callable timeout.
  const signal = AbortSignal.timeout(timeoutMs);
  const results = new Array(urls.length);
  const cache = new Map();
  let next = 0;
  async function parse(sourceUrl) {
    try {
      let page = await readPage(safeUrl(sourceUrl), fetchImpl, signal);
      let extracted;
      try { extracted = extract(page.html, page.url, sourceUrl); }
      catch (error) {
        // Desktop maps often return only a JavaScript shell. Try the public place page once.
        const id = page.url.pathname.match(/\/entry\/place\/(\d+)(?:\/|$)/)?.[1];
        if (error.code !== "METADATA_UNAVAILABLE" || page.url.hostname !== "map.naver.com" || !id) throw error;
        page = await readPage(safeUrl(`https://m.place.naver.com/place/${id}/home`), fetchImpl, signal);
        extracted = extract(page.html, page.url, sourceUrl);
      }
      return { sourceUrl, status: "success", ...extracted };
    } catch (error) {
      return { sourceUrl, status: "error", error: {
        code: signal.aborted ? "TIMEOUT" : error instanceof ParseError ? error.code : "FETCH_FAILED",
        message: signal.aborted ? "지도 응답 시간이 초과됐어요." : error instanceof ParseError ? error.message : "지도 페이지를 불러오지 못했어요.",
      } };
    }
  }
  await Promise.all(Array.from({ length: Math.min(2, urls.length) }, async () => {
    while (next < urls.length) {
      const i = next++;
      if (!cache.has(urls[i])) cache.set(urls[i], parse(urls[i]));
      results[i] = await cache.get(urls[i]);
    }
  }));
  return { results, successCount: results.filter((r) => r.status === "success").length,
    errorCount: results.filter((r) => r.status === "error").length };
}

module.exports = { parsePlaces };
