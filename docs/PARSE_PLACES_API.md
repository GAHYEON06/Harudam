# parsePlaces API 사용 안내

`asia-northeast3`의 기존 Firebase Callable `travelApi`에 `action: "parsePlaces"`를 추가했습니다. 로그인한 사용자만 호출할 수 있습니다. [Firebase Callable 공식 문서](https://firebase.google.com/docs/functions/callable)에 따라 Android에서는 `getHttpsCallable("travelApi").call(data)`를 사용합니다.

## 요청

```json
{ "action": "parsePlaces", "url": "https://map.naver.com/p/entry/place/13572738" }
```

```json
{
  "action": "parsePlaces",
  "urls": [
    "https://map.naver.com/p/entry/place/13572738",
    "https://m.place.naver.com/restaurant/13572738/home"
  ]
}
```

`url`과 `urls` 중 하나만 전송합니다. 배열은 1~10개, 각 URL은 공백 제거 후 1~500자입니다. `tripId`, `requestId`, `expectedVersion`은 필요하지 않습니다. 잘못된 요청 구조는 Callable `invalid-argument`, 비로그인은 `unauthenticated`로 거부합니다.

## 응답

다음은 공개 페이지가 구조화된 장소 정보를 제공할 때의 응답 형식 예시이며 실제 네이버 호출 성공 기록은 아닙니다.

```json
{
  "results": [{
    "sourceUrl": "https://map.naver.com/p/entry/place/13572738",
    "status": "success",
    "place": {
      "name": "확인된 장소 이름",
      "address": "확인된 주소",
      "category": "FOOD",
      "sourceUrl": "https://map.naver.com/p/entry/place/13572738",
      "latitude": null,
      "longitude": null,
      "visitDate": null,
      "visitOrder": null,
      "note": "",
      "visitTime": null
    },
    "provider": "NAVER",
    "providerPlaceId": "13572738",
    "resolvedUrl": "https://m.place.naver.com/place/13572738/home",
    "warnings": ["COORDINATES_UNAVAILABLE"],
    "requiresConfirmation": true
  }],
  "successCount": 1,
  "errorCount": 0
}
```

`results`는 입력 순서와 개수를 유지합니다. 동일한 원본 URL은 요청 안에서 한 번만 조회하지만 결과 위치는 유지하므로 저장 전 중복 장소 여부를 확인하세요. `successCount`는 성공한 결과 항목 수이며 고유 장소 수가 아닙니다.

실패 항목은 다음 형태이며 다른 URL의 성공을 취소하지 않습니다.

```json
{
  "sourceUrl": "https://naver.me/example",
  "status": "error",
  "error": {
    "code": "PROVIDER_RATE_LIMITED",
    "message": "네이버에서 접근을 제한했어요. 잠시 후 다시 시도하거나 직접 입력해 주세요."
  }
}
```

| 코드 | 의미 |
| --- | --- |
| INVALID_URL | URL 문법 또는 길이 오류 |
| UNSUPPORTED_URL | 허용하지 않는 호스트·프로토콜·포트·사용자 정보 |
| REDIRECT_ERROR | 이동 경로 누락 또는 최대 이동 횟수 초과 |
| FETCH_FAILED | 네트워크 오류 또는 HTTP 실패 |
| PROVIDER_RATE_LIMITED | 제공자가 HTTP 429로 접근 제한 |
| UNSUPPORTED_CONTENT | HTML 이외의 응답 |
| PAGE_TOO_LARGE | 페이지 본문이 1 MiB 초과 |
| UNSUPPORTED_PAGE | 개별 장소를 식별할 수 없는 목록·검색 등 |
| METADATA_UNAVAILABLE | 확인 가능한 장소 이름 없음 |
| TIMEOUT | 배치 전체 20초 제한 초과 |

## 추출과 저장

허용 호스트는 `naver.me`, `map.naver.com`, `m.map.naver.com`, `place.naver.com`, `m.place.naver.com`, `pcmap.place.naver.com`입니다. HTTP 공유 링크는 HTTPS로 올리고 이동할 때마다 호스트를 다시 검사합니다. 개별 장소 경로 또는 숫자 `id` 쿼리를 식별하며 장소 ID만으로 이름을 만들지 않습니다.

공개 HTML의 JSON-LD에서 이름·주소·유형·위경도를 읽고, JSON-LD 이름이 없으면 `og:title`을 확인합니다. [Schema.org LocalBusiness](https://schema.org/LocalBusiness)의 주소와 좌표 구조를 따릅니다. 데스크톱 `map.naver.com/p/entry/place/{id}`가 빈 화면 틀만 제공하면 공개 모바일 장소 페이지를 한 번 조회합니다. 로그인, 브라우저 실행, 비공개 API 호출, 접근 제한 우회는 하지 않습니다.

주소를 확인하지 못하면 `""`, 좌표 중 하나라도 유효하지 않으면 둘 다 `null`, 유형을 확정하지 못하면 `OTHER`를 반환합니다. 경고 코드는 `ADDRESS_UNAVAILABLE`, `COORDINATES_UNAVAILABLE`, `CATEGORY_UNCONFIRMED`입니다. 이름만 추출되어도 `success`일 수 있으므로 저장 전에 사용자 확인이 필요합니다.

파싱은 Firestore를 변경하지 않습니다. 결과의 `place`를 확인·수정한 다음 기존 저장 동작에 전달합니다.

```kotlin
// 기존 TripRepository.call 사용 예시. 이 예시가 FE에 적용되어 있지는 않습니다.
val preview = repository.call(mapOf("action" to "parsePlaces", "url" to inputUrl))
// results 중 status == success인 항목의 place를 사용자에게 보여 줍니다.
// 확인된 필드를 기존 createTrip의 places 배열 또는 addPlace 요청에 전달합니다.
```

`createTrip`의 `places`에는 `place` 객체들을 넣습니다. `addPlace`는 `action`, `tripId`, 새 `requestId`, 최신 `expectedVersion`과 함께 `place`의 필드를 최상위에 펼쳐 전송합니다. `id`는 기존 서버가 생성하고, 일정은 기존 `setSchedule`로 지정합니다. 저장 결과의 새 `version`을 다음 쓰기에 사용합니다. 이 과정에서 기존 소유권·최대 100개 장소·중복 요청·버전 충돌 검증이 그대로 적용됩니다.

FE 표시 분류 `음식·카페`, `관광`, `숙박`, `기타`는 각각 `FOOD`, `TOUR`, `STAY`, `OTHER`에 대응합니다. FE의 좌표 타입은 `Double`이지만 BE는 미확인 좌표를 `null`로 반환하므로 화면에서 미확인 상태를 처리해야 합니다.

## 지원 한계

여러 개의 개별 장소 URL을 처리하며, 하나의 네이버 저장 목록 URL에서 모든 장소를 가져오는 기능은 지원하지 않습니다. 카카오맵·구글맵도 미지원입니다. 네이버의 동적 페이지·메타데이터 변경·HTTP 429 때문에 실제 공유 링크의 자동 추출을 보장할 수 없습니다. 2026년 11월 22일 실제 공개 링크 시험에서는 HTTP 429를 확인했으며 성공은 검증하지 못했습니다. 실패하면 원본 링크와 직접 입력한 이름을 기존 저장 API로 저장할 수 있습니다.

## 테스트 실행

Node.js 22, Java, Firebase CLI를 준비합니다.

```powershell
cd Harudam2/backend/functions
npm ci
npm run test:parser
cd ..
$env:TEST_PROJECT_ID='demo-harudam-parser'
firebase emulators:exec --project demo-harudam-parser --only auth,firestore,functions "node functions/testbackend.js"
```

파서 단위 테스트는 합성 HTML과 모의 네트워크를 사용합니다. 통합 테스트의 성공 파싱 결과 역시 합성 HTML이며, 그 결과의 저장·재조회는 실제 로컬 Functions와 Firestore를 사용합니다. 운영 배포 및 실제 네이버 추출 성공과 구분해야 합니다.
