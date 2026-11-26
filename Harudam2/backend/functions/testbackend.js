const assert = require("node:assert/strict");
const { randomUUID } = require("node:crypto");
const { parsePlaces } = require("./parse-places");

const projectId = process.env.TEST_PROJECT_ID;

if (!projectId || !projectId.startsWith("demo-")) {
  throw new Error(
    "TEST_PROJECT_ID에 demo-로 시작하는 테스트 프로젝트 ID를 넣어 주세요."
  );
}

const functionUrl =
  `http://127.0.0.1:5001/${projectId}` +
  "/asia-northeast3/travelApi";

async function makeUser() {
  const response = await fetch(
    "http://127.0.0.1:9099/" +
      "identitytoolkit.googleapis.com/v1/accounts:signUp?key=fake-key",
    {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        email: `test-${randomUUID()}@example.com`,
        password: "TestPass123!",
        returnSecureToken: true,
      }),
    }
  );

  const body = await response.json();

  assert.ok(response.ok, JSON.stringify(body));
  return body.idToken;
}

async function call(token, data) {
  const response = await fetch(functionUrl, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: JSON.stringify({ data }),
  });

  const body = await response.json();

  if (body.error) {
    const error = new Error(body.error.message);
    error.code = body.error.status;
    throw error;
  }

  assert.ok(response.ok, JSON.stringify(body));
  return body.result;
}

function write(action, tripId, expectedVersion, extra = {}) {
  return {
    action,
    requestId: randomUUID(),
    tripId,
    expectedVersion,
    ...extra,
  };
}

async function main() {
  const userA = await makeUser();
  const userB = await makeUser();

  await assert.rejects(() => call(null, { action: "listTrips" }),
    (error) => error.code === "UNAUTHENTICATED");
  console.log("통과: 비로그인 요청 차단");

  await assert.rejects(() => call(null, { action: "parsePlaces", url: "https://naver.me/example" }),
    (error) => error.code === "UNAUTHENTICATED");
  await assert.rejects(() => call(userA, { action: "parsePlaces", urls: [] }),
    (error) => error.code === "INVALID_ARGUMENT");
  const preview = await call(userA, { action: "parsePlaces", urls: ["https://example.com", "http://127.0.0.1"] });
  assert.equal(preview.errorCount, 2);
  assert.equal(preview.successCount, 0);
  assert.ok(preview.results.every((r) => r.status === "error"));
  assert.equal((await call(userA, { action: "listTrips" })).trips.length, 0);
  console.log("통과: parsePlaces 인증, 입력 검증, 부분 오류, 자동 저장 없음");

  const creation = {
    action: "createTrip",
    requestId: randomUUID(),
    title: "부산여행",
    startDate: "2026-12-01",
    endDate: "2026-12-03",
  };

  let result = await call(userA, creation);
  const tripId = result.tripId;
  let version = result.version;

  console.log("통과: 여행 생성");

  const repeated = await call(userA, creation);
  assert.equal(repeated.tripId, tripId);
  assert.equal(repeated.version, version);

  console.log("통과: 같은 요청 재전송 시 중복 생성 방지");

  const reordered = Object.fromEntries(Object.entries(creation).reverse());
  assert.deepEqual(await call(userA, reordered), repeated);
  await assert.rejects(() => call(userA, { ...creation, title: "변경된 요청" }),
    (error) => error.code === "ALREADY_EXISTS");
  console.log("통과: 키 순서가 다른 재전송과 같은 ID의 다른 내용 구분");

  await assert.rejects(
    () => call(userB, { action: "getTrip", tripId }),
    (error) => error.code === "PERMISSION_DENIED"
  );

  console.log("통과: 다른 사용자 접근 차단");

  await assert.rejects(
    () => call(userA, {
      action: "createTrip",
      requestId: randomUUID(),
      title: "잘못된 날짜",
      startDate: "2026-02-30",
      endDate: "2026-03-03",
    }),
    (error) => error.code === "INVALID_ARGUMENT"
  );

  console.log("통과: 실제로 존재하지 않는 날짜 차단");

  result = await call(
    userA,
    write("addPlace", tripId, version, {
      name: "광안리해수욕장",
      category: "TOUR",
      address: "부산 수영구",
    })
  );

  const beachId = result.placeId;
  version = result.version;

  // Provider HTML is deterministic here; persistence still uses real emulated Callable + Firestore.
  const parsedFood = await parsePlaces({ url: "https://m.place.naver.com/restaurant/13572738/home" }, {
    fetchImpl: async () => new Response('<script type="application/ld+json">' + JSON.stringify({
      "@type": "Restaurant", name: "초량밀면", address: "부산 동구 중앙대로 225",
      geo: { latitude: 35.115, longitude: 129.04 },
    }) + '</script>', { headers: { "content-type": "text/html" } }),
  });
  assert.equal(parsedFood.successCount, 1);

  result = await call(
    userA,
    write("addPlace", tripId, version, {
      ...parsedFood.results[0].place,
    })
  );

  const foodId = result.placeId;
  version = result.version;

  const savedFood = (await call(userA, { action: "getTrip", tripId })).places.find((p) => p.id === foodId);
  for (const key of ["name", "address", "category", "sourceUrl", "latitude", "longitude"]) {
    assert.equal(savedFood[key], parsedFood.results[0].place[key]);
  }
  console.log("통과: 파싱 fixture 결과를 기존 addPlace로 저장·재조회");

  console.log("통과: 장소 두 개 추가");
  const batch = write("addPlaces", tripId, version, { places: [
    { name: "장소 A", sourceUrl: "https://example.com/a" },
    { name: "장소 B", sourceUrl: "https://example.com/b" }
  ] });
  result = await call(userA, batch);
  assert.equal(result.placeIds.length, 2);
  assert.equal(result.version, version + 1);
  assert.deepEqual(await call(userA, batch), result);
  version = result.version;
  for (const [token, places, code] of [
    [userA, [{name:"중복",sourceUrl:"https://example.com/a"}], "ALREADY_EXISTS"],
    [userA, [{name:"정상"},{name:""}], "INVALID_ARGUMENT"],
    [userB, [{name:"권한 없음"}], "PERMISSION_DENIED"]
  ]) {
    await assert.rejects(() => call(token, write("addPlaces", tripId, version, {places})), e => e.code === code);
  }
  const afterBatch = await call(userA, {action:"getTrip",tripId});
  assert.equal(afterBatch.places.length, 4);
  assert.equal(afterBatch.version, version);
  console.log("통과: 일괄 저장, 재시도, 중복 및 원자성 검증");


  result = await call(
    userA,
    write("confirmPlaces", tripId, version)
  );
  version = result.version;

  for (const placeId of [beachId, foodId]) {
    result = await call(
      userA,
      write("setSchedule", tripId, version, {
        placeId,
        visitDate: "2026-12-01",
      })
    );
    version = result.version;
  }

  result = await call(
    userA,
    write("reorderSchedule", tripId, version, {
      visitDate: "2026-12-01",
      placeIds: [foodId, beachId],
    })
  );
  version = result.version;

  let trip = await call(userA, { action: "getTrip", tripId });

  assert.equal(
    trip.schedules["2026-12-01"][0].id,
    foodId
  );

  console.log("통과: 일정 추가와 순서 변경");

  await assert.rejects(
    () => call(
      userA,
      write("updateTrip", tripId, version - 1, {
        title: "오래된 버전 수정",
      })
    ),
    (error) => error.code === "ABORTED"
  );

  console.log("통과: 오래된 버전의 덮어쓰기 차단");

  result = await call(
    userA,
    write("setSchedule", tripId, version, {
      placeId: beachId,
      visitDate: "2026-12-02",
    })
  );
  version = result.version;

  trip = await call(userA, { action: "getTrip", tripId });

  assert.equal(trip.schedules["2026-12-01"].length, 1);
  assert.equal(trip.schedules["2026-12-02"][0].id, beachId);

  console.log("통과: 다른 날짜로 장소 이동");

  await assert.rejects(
    () => call(
      userA,
      write("setSchedule", tripId, version, {
        placeId: beachId,
        visitDate: "2026-12-10",
      })
    ),
    (error) => error.code === "INVALID_ARGUMENT"
  );

  console.log("통과: 여행 기간 밖 일정 차단");

  const list = await call(userA, { action: "listTrips" });

  assert.ok(list.trips.some((item) => item.id === tripId));

  console.log("통과: 내 여행 목록 조회");

  const baseVersion = version;
  const concurrent = await Promise.allSettled([
    call(userA, write("updateTrip", tripId, baseVersion, { title: "동시 수정 A" })),
    call(userA, write("updateTrip", tripId, baseVersion, { title: "동시 수정 B" })),
  ]);
  assert.equal(concurrent.filter((x) => x.status === "fulfilled").length, 1);
  assert.equal(concurrent.filter((x) => x.status === "rejected" && x.reason.code === "ABORTED").length, 1);
  version = concurrent.find((x) => x.status === "fulfilled").value.version;
  console.log("통과: 실제 동시 수정 1건만 성공");

  await assert.rejects(() => call(userA, write("setSchedule", tripId, version, {
    placeId: foodId, visitDate: "2026-12-01", visitTime: "25:90",
  })), (error) => error.code === "INVALID_ARGUMENT");
  result = await call(userA, write("setSchedule", tripId, version, {
    placeId: foodId, visitDate: "2026-12-01", visitTime: "12:30", note: "점심 메모",
  }));
  version = result.version;
  trip = await call(userA, { action: "getTrip", tripId });
  assert.equal(trip.schedules["2026-12-01"][0].visitTime, "12:30");
  assert.equal(trip.schedules["2026-12-01"][0].note, "점심 메모");

  result = await call(userA, write("setSchedule", tripId, version, {
    placeId: beachId, visitDate: "2026-12-01",
  }));
  version = result.version;
  result = await call(userA, write("setSchedule", tripId, version, {
    placeId: foodId, visitDate: "2026-12-01", note: "메모만 수정",
  }));
  version = result.version;
  trip = await call(userA, { action: "getTrip", tripId });
  assert.deepEqual(trip.schedules["2026-12-01"].map((p) => p.id), [foodId, beachId]);
  assert.equal(trip.schedules["2026-12-01"][0].visitTime, "12:30");
  console.log("통과: 시간 검증, 메모 저장, 메모 수정 시 순서 유지");

  result = await call(userA, write("updatePlace", tripId, version, {
    placeId: foodId, name: "수정한 식당", sourceUrl: "https://example.com/place",
  }));
  version = result.version;
  trip = await call(userA, { action: "getTrip", tripId });
  assert.equal(trip.places.find((p) => p.id === foodId).name, "수정한 식당");
  assert.equal(trip.placesConfirmed, false);
  await assert.rejects(() => call(userB, write("deleteTrip", tripId, version)),
    (error) => error.code === "PERMISSION_DENIED");
  result = await call(userA, write("removeSchedule", tripId, version, { placeId: foodId }));
  version = result.version;
  trip = await call(userA, { action: "getTrip", tripId });
  assert.equal(trip.unscheduledPlaces.some((p) => p.id === foodId), true);
  assert.equal(trip.schedules["2026-12-01"][0].visitOrder, 1);
  result = await call(userA, write("deletePlace", tripId, version, { placeId: beachId }));
  version = result.version;
  console.log("통과: 장소 수정, 일정 취소, 장소 삭제, 비소유자 변경 차단");

  const documentUrl = `http://127.0.0.1:8080/v1/projects/${projectId}/databases/(default)/documents/trips/${tripId}`;
  for (const [token, expected] of [[userA, 200], [userB, 403], [null, 403]]) {
    const response = await fetch(documentUrl, { headers: token ? { Authorization: `Bearer ${token}` } : {} });
    assert.equal(response.status, expected);
  }
  const directWrite = await fetch(documentUrl, {
    method: "PATCH", headers: { Authorization: `Bearer ${userA}`, "Content-Type": "application/json" },
    body: JSON.stringify({ fields: { ownerId: { stringValue: "forged" } } }),
  });
  assert.equal(directWrite.status, 403);
  console.log("통과: Firestore 규칙의 소유자 읽기, 비참여자 차단, 직접 쓰기 차단");

  const identity = JSON.parse(Buffer.from(userA.split(".")[1], "base64url").toString());
  const uid = identity.sub;
  const profilePath = `projects/${projectId}/databases/(default)/documents/users/${uid}`;
  const commitUrl = `http://127.0.0.1:8080/v1/projects/${projectId}/databases/(default)/documents:commit`;
  const profile = await fetch(commitUrl, {
    method: "POST", headers: { Authorization: `Bearer ${userA}`, "Content-Type": "application/json" },
    body: JSON.stringify({ writes: [{
      update: { name: profilePath, fields: { username: { stringValue: "여행자" }, email: { stringValue: identity.email } } },
      updateTransforms: [{ fieldPath: "createdAt", setToServerValue: "REQUEST_TIME" }],
    }] }),
  });
  assert.equal(profile.status, 200, await profile.text());
  const profileUrl = `http://127.0.0.1:8080/v1/${profilePath}`;
  assert.equal((await fetch(profileUrl, { headers: { Authorization: `Bearer ${userA}` } })).status, 200);
  assert.equal((await fetch(profileUrl, { headers: { Authorization: `Bearer ${userB}` } })).status, 403);
  console.log("통과: 회원가입 프로필 생성 규칙과 본인만 조회");

  await call(
    userA,
    write("deleteTrip", tripId, version)
  );

  await assert.rejects(
    () => call(userA, { action: "getTrip", tripId }),
    (error) => error.code === "NOT_FOUND"
  );

  console.log("통과: 여행 삭제");
  console.log("모든 테스트 완료");
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
