const { initializeApp } = require("firebase-admin/app");
const {
  getFirestore,
  FieldPath,
} = require("firebase-admin/firestore");
const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { createHash, randomUUID } = require("node:crypto");

initializeApp();

const db = getFirestore();

const REGION = "asia-northeast3";
const MAX_PLACES = 100;
const DAY_MS = 24 * 60 * 60 * 1000;

function fail(code, message, details) {
  throw new HttpsError(code, message, details);
}

function text(value, label, max, optional = false) {
  if (optional && (value === undefined || value === null)) {
    return "";
  }

  if (typeof value !== "string") {
    fail("invalid-argument", `${label} 형식을 확인해 주세요.`);
  }

  const result = value.trim();

  if ((!optional && !result) || result.length > max) {
    fail(
      "invalid-argument",
      `${label}은 ${optional ? 0 : 1}~${max}자로 입력해 주세요.`
    );
  }

  return result;
}

function documentId(value, label) {
  const result = text(value, label, 128);

  if (!/^[A-Za-z0-9_-]+$/.test(result)) {
    fail("invalid-argument", `${label} 형식이 잘못됐어요.`);
  }

  return result;
}

function date(value, label) {
  if (
    typeof value !== "string" ||
    !/^\d{4}-\d{2}-\d{2}$/.test(value)
  ) {
    fail("invalid-argument", `${label}은 YYYY-MM-DD 형식이에요.`);
  }

  const timestamp = Date.parse(`${value}T00:00:00.000Z`);

  if (
    !Number.isFinite(timestamp) ||
    new Date(timestamp).toISOString().slice(0, 10) !== value
  ) {
    fail("invalid-argument", `${label}이 실제 날짜가 아니에요.`);
  }

  return value;
}

function validatePeriod(start, end) {
  if (start > end) {
    fail("invalid-argument", "종료일은 시작일보다 빠를 수 없어요.");
  }

  const duration =
    Date.parse(`${end}T00:00:00Z`) -
    Date.parse(`${start}T00:00:00Z`);

  if (duration > 365 * DAY_MS) {
    fail("invalid-argument", "여행 기간은 최대 366일이에요.");
  }
}

function assertOwner(trip, uid) {
  if (trip.ownerId !== uid) {
    fail("permission-denied", "이 여행에 접근할 권한이 없어요.");
  }
}

function assertVersion(trip, expectedVersion) {
  if (
    !Number.isInteger(expectedVersion) ||
    expectedVersion < 1
  ) {
    fail("invalid-argument", "expectedVersion을 입력해 주세요.");
  }

  if (trip.version !== expectedVersion) {
    fail(
      "aborted",
      "다른 수정이 반영됐어요. 최신 데이터를 다시 조회해 주세요.",
      {
        code: "VERSION_CONFLICT",
        currentVersion: trip.version,
      }
    );
  }
}

function normalizeCategory(value) {
  const category = value ?? "OTHER";

  if (!["FOOD", "TOUR", "STAY", "OTHER"].includes(category)) {
    fail(
      "invalid-argument",
      "분류는 FOOD, TOUR, STAY, OTHER 중 하나예요."
    );
  }

  return category;
}

function coordinate(value, label, min, max) {
  if (value === undefined || value === null) {
    return null;
  }

  if (
    typeof value !== "number" ||
    !Number.isFinite(value) ||
    value < min ||
    value > max
  ) {
    fail("invalid-argument", `${label} 범위를 확인해 주세요.`);
  }

  return value;
}

function normalizePlace(data) {
  const latitude = coordinate(
    data.latitude,
    "위도",
    -90,
    90
  );

  const longitude = coordinate(
    data.longitude,
    "경도",
    -180,
    180
  );

  if ((latitude === null) !== (longitude === null)) {
    fail("invalid-argument", "위도와 경도는 함께 입력해 주세요.");
  }

  const sourceUrl = text(
    data.sourceUrl,
    "장소 링크",
    500,
    true
  );

  if (sourceUrl) {
    let parsed;

    try {
      parsed = new URL(sourceUrl);
    } catch {
      fail("invalid-argument", "올바른 장소 링크를 입력해 주세요.");
    }

    if (!["http:", "https:"].includes(parsed.protocol)) {
      fail("invalid-argument", "http 또는 https 링크만 가능해요.");
    }
  }

  return {
    name: text(data.name, "장소 이름", 100),
    address: text(data.address, "주소", 300, true),
    category: normalizeCategory(data.category),
    latitude,
    longitude,
    sourceUrl,
  };
}

function findPlace(trip, placeId) {
  const id = documentId(placeId, "장소 ID");
  const place = trip.places.find((item) => item.id === id);

  if (!place) {
    fail("not-found", "장소를 찾지 못했어요.");
  }

  return place;
}

function validateVisitDate(trip, value) {
  const result = date(value, "방문 날짜");

  if (result < trip.startDate || result > trip.endDate) {
    fail("invalid-argument", "방문 날짜가 여행 기간을 벗어났어요.");
  }

  return result;
}

function compactOrders(trip) {
  const dates = new Set(
    trip.places
      .filter((place) => place.visitDate !== null)
      .map((place) => place.visitDate)
  );

  for (const visitDate of dates) {
    const places = trip.places
      .filter((place) => place.visitDate === visitDate)
      .sort((a, b) => a.visitOrder - b.visitOrder);

    places.forEach((place, index) => {
      place.visitOrder = index + 1;
    });
  }
}

function summary(id, trip) {
  const days =
    Math.round(
      (
        Date.parse(`${trip.endDate}T00:00:00Z`) -
        Date.parse(`${trip.startDate}T00:00:00Z`)
      ) / DAY_MS
    ) + 1;

  return {
    id,
    title: trip.title,
    startDate: trip.startDate,
    endDate: trip.endDate,
    days,
    nights: days - 1,
    placeCount: trip.places.length,
    placesConfirmed: trip.placesConfirmed,
    version: trip.version,
    createdAt: trip.createdAt,
    updatedAt: trip.updatedAt,
  };
}

function detail(id, trip) {
  const schedules = {};

  for (
    let current = Date.parse(`${trip.startDate}T00:00:00Z`);
    current <= Date.parse(`${trip.endDate}T00:00:00Z`);
    current += DAY_MS
  ) {
    const day = new Date(current).toISOString().slice(0, 10);

    schedules[day] = trip.places
      .filter((place) => place.visitDate === day)
      .sort((a, b) => a.visitOrder - b.visitOrder);
  }

  return {
    ...summary(id, trip),
    places: trip.places,
    schedules,
    unscheduledPlaces: trip.places.filter(
      (place) => place.visitDate === null
    ),
  };
}

const WRITE_ACTIONS = new Set([
  "createTrip",
  "updateTrip",
  "deleteTrip",
  "addPlace",
  "updatePlace",
  "deletePlace",
  "confirmPlaces",
  "setSchedule",
  "removeSchedule",
  "reorderSchedule",
]);

exports.travelApi = onCall(
  {
    region: REGION,
    maxInstances: 3,
    timeoutSeconds: 60,
  },
  async (request) => {
    if (!request.auth) {
      fail("unauthenticated", "로그인이 필요해요.");
    }

    const uid = request.auth.uid;
    const data = request.data;

    if (
      data === null ||
      typeof data !== "object" ||
      Array.isArray(data)
    ) {
      fail("invalid-argument", "요청 데이터 형식이 잘못됐어요.");
    }

    const action = text(data.action, "작업 이름", 40);

    // 여행 목록: 한 번에 최대 50개
    if (action === "listTrips") {
      const limit = data.limit ?? 20;

      if (
        !Number.isInteger(limit) ||
        limit < 1 ||
        limit > 50
      ) {
        fail("invalid-argument", "limit은 1~50 사이예요.");
      }

      let query = db.collection("trips")
        .where("ownerId", "==", uid)
        .orderBy(FieldPath.documentId());

      if (data.cursor) {
        query = query.startAfter(
          documentId(data.cursor, "페이지 커서")
        );
      }

      const snapshot = await query.limit(limit + 1).get();
      const page = snapshot.docs.slice(0, limit);
      const hasMore = snapshot.docs.length > limit;

      return {
        trips: page.map((doc) => summary(doc.id, doc.data())),
        nextCursor: hasMore ? page[page.length - 1].id : null,
      };
    }

    // 여행 상세
    if (action === "getTrip") {
      const tripId = documentId(data.tripId, "여행 ID");
      const snapshot = await db.collection("trips")
        .doc(tripId).get();

      if (!snapshot.exists) {
        fail("not-found", "여행을 찾지 못했어요.");
      }

      const trip = snapshot.data();
      assertOwner(trip, uid);

      return detail(tripId, trip);
    }

    if (!WRITE_ACTIONS.has(action)) {
      fail("invalid-argument", "지원하지 않는 작업이에요.");
    }

    // 같은 requestId의 재전송은 저장을 반복하지 않음
    const requestId = text(data.requestId, "요청 ID", 100);

    const requestKey = createHash("sha256")
      .update(`${uid}:${requestId}`)
      .digest("hex");

    const payloadHash = createHash("sha256")
      .update(JSON.stringify(data))
      .digest("hex");

    const requestRef = db.collection("requests").doc(requestKey);

    const tripId = action === "createTrip"
      ? db.collection("trips").doc().id
      : documentId(data.tripId, "여행 ID");

    const tripRef = db.collection("trips").doc(tripId);

    return db.runTransaction(async (transaction) => {
      const previous = await transaction.get(requestRef);

      if (previous.exists) {
        const saved = previous.data();

        if (saved.payloadHash !== payloadHash) {
          fail(
            "already-exists",
            "같은 requestId를 다른 요청에 사용할 수 없어요."
          );
        }

        return saved.result;
      }

      const now = Date.now();
      let trip;
      let result;
      let deleted = false;

      if (action === "createTrip") {
        const startDate = date(data.startDate, "시작일");
        const endDate = date(data.endDate, "종료일");

        validatePeriod(startDate, endDate);

        trip = {
          title: text(data.title, "여행 이름", 60),
          startDate,
          endDate,
          ownerId: uid,
          places: [],
          placesConfirmed: false,
          version: 1,
          createdAt: now,
          updatedAt: now,
        };

        result = { tripId, version: 1 };
      } else {
        const snapshot = await transaction.get(tripRef);

        if (!snapshot.exists) {
          fail("not-found", "여행을 찾지 못했어요.");
        }

        trip = snapshot.data();

        assertOwner(trip, uid);
        assertVersion(trip, data.expectedVersion);

        switch (action) {
          case "updateTrip": {
            const startDate = data.startDate === undefined
              ? trip.startDate
              : date(data.startDate, "시작일");

            const endDate = data.endDate === undefined
              ? trip.endDate
              : date(data.endDate, "종료일");

            validatePeriod(startDate, endDate);

            const outside = trip.places.some(
              (place) =>
                place.visitDate !== null &&
                (
                  place.visitDate < startDate ||
                  place.visitDate > endDate
                )
            );

            if (outside) {
              fail(
                "failed-precondition",
                "새 여행 기간 밖에 일정이 있어요. 먼저 일정을 옮겨 주세요."
              );
            }

            if (data.title !== undefined) {
              trip.title = text(data.title, "여행 이름", 60);
            }

            trip.startDate = startDate;
            trip.endDate = endDate;
            break;
          }

          case "deleteTrip": {
            deleted = true;
            break;
          }

          case "addPlace": {
            if (trip.places.length >= MAX_PLACES) {
              fail(
                "resource-exhausted",
                "여행당 장소는 최대 100개까지 저장할 수 있어요."
              );
            }

            const placeId = randomUUID();

            trip.places.push({
              id: placeId,
              ...normalizePlace(data),
              visitDate: null,
              visitOrder: null,
              note: "",
            });

            trip.placesConfirmed = false;
            result = { placeId };
            break;
          }

          case "updatePlace": {
            const place = findPlace(trip, data.placeId);

            Object.assign(
              place,
              normalizePlace({
                ...place,
                ...data,
              })
            );

            trip.placesConfirmed = false;
            break;
          }

          case "deletePlace": {
            const place = findPlace(trip, data.placeId);

            trip.places = trip.places.filter(
              (item) => item.id !== place.id
            );

            compactOrders(trip);
            trip.placesConfirmed = false;
            break;
          }

          case "confirmPlaces": {
            if (trip.places.length === 0) {
              fail(
                "failed-precondition",
                "장소를 한 개 이상 추가해 주세요."
              );
            }

            trip.placesConfirmed = true;
            break;
          }

          // 새 일정 지정 또는 다른 날로 이동
          case "setSchedule": {
            const place = findPlace(trip, data.placeId);
            const visitDate = validateVisitDate(
              trip,
              data.visitDate
            );

            const note = data.note === undefined
              ? place.note
              : text(data.note, "메모", 1000, true);

            // 이동 전 위치에서 먼저 제거
            place.visitDate = null;
            place.visitOrder = null;
            compactOrders(trip);

            const target = trip.places.filter(
              (item) => item.visitDate === visitDate
            );

            // 지정한 날짜의 맨 마지막에 배치
            place.visitDate = visitDate;
            place.visitOrder = target.length + 1;
            place.note = note;
            break;
          }

          // 장소는 유지하고 일정 배치만 취소
          case "removeSchedule": {
            const place = findPlace(trip, data.placeId);

            place.visitDate = null;
            place.visitOrder = null;

            compactOrders(trip);
            break;
          }

          case "reorderSchedule": {
            const visitDate = validateVisitDate(
              trip,
              data.visitDate
            );

            if (!Array.isArray(data.placeIds)) {
              fail(
                "invalid-argument",
                "placeIds 배열을 입력해 주세요."
              );
            }

            const ids = data.placeIds.map(
              (id) => documentId(id, "장소 ID")
            );

            const current = trip.places.filter(
              (place) => place.visitDate === visitDate
            );

            const currentIds = new Set(
              current.map((place) => place.id)
            );

            if (
              new Set(ids).size !== ids.length ||
              ids.length !== current.length ||
              ids.some((id) => !currentIds.has(id))
            ) {
              fail(
                "invalid-argument",
                "해당 날짜의 모든 장소 ID를 중복 없이 보내 주세요."
              );
            }

            ids.forEach((id, index) => {
              findPlace(trip, id).visitOrder = index + 1;
            });

            break;
          }

          default:
            fail("invalid-argument", "지원하지 않는 작업이에요.");
        }

        trip.version += 1;
        trip.updatedAt = now;

        result = {
          ...result,
          tripId,
          version: trip.version,
          ...(deleted ? { deleted: true } : {}),
        };
      }

      if (deleted) {
        // 이 버전은 하위 컬렉션을 사용하지 않음
        transaction.delete(tripRef);
      } else {
        transaction.set(tripRef, trip);
      }

      transaction.set(requestRef, {
        uid,
        action,
        payloadHash,
        result,
        createdAt: now,
      });

      return result;
    });
  }
);