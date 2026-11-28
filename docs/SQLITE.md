# SQLite 저장 전환 — 2026-11-28

## 동작

여행 관련 모든 앱 요청은 `TripRepository`에서 `LocalTravelStore`로 전달된다. Firebase Functions는 여행 조회·생성·수정·삭제, 장소 저장, 일정 배치/순서/방문 정보에 사용하지 않는다. Android 내장 SQLiteOpenHelper를 사용하고, 디스크 작업은 Dispatchers.IO에서 처리한다.

- 기본 앱: Firebase 로그인/프로필은 mobile-pg-b27d4, 여행 저장은 앱 전용 `harudam.db`.
- 테스트 앱(`-PuseFirebaseEmulators=true`): demo-harudam 인증, 별도 `harudam-demo.db`.
- 현재 로그인 UID를 저장 호출 시 고정해 전달하며, 조회/변경 시 소유자를 확인한다. 계정이 바뀌면 다른 계정의 여행은 목록에서 제외된다.
- 앱 화면 상단에 기기 저장 모드를 표시한다. 첫 로그인·회원가입·비밀번호 재설정은 인터넷이 필요하다. 이미 로그인된 앱의 여행 관리는 오프라인으로 가능하다.
- 기기 간 동기화, 공동 편집, Firestore 여행의 자동 이관은 구현하지 않는다. 기존 클라우드 여행을 삭제하거나 변경하지 않는다.
- 네이버 자동 메타데이터 파싱은 SQLite 자체의 기능이 아니다. 이 모드에서는 Functions 호출 대신 즉시 수동 이름·주소 입력으로 안내하며 원본 링크를 함께 저장한다. 외부 지도 열기는 기존대로 유지한다.

## 저장 구조와 보장 범위

`trips(id, owner_id, payload)`에 여행과 최대 100개 장소/일정을 JSON 묶음으로 저장하고 owner_id/id 인덱스로 계정별 목록을 페이지 조회한다. `requests(owner_id, request_id, hash, result)`에 요청 결과를 저장한다. 두 테이블의 변경은 한 SQLite 트랜잭션으로 커밋한다.

- 입력 검증, 실제 날짜/기간, 좌표 쌍, 시간, 장소 수 제한을 유지한다.
- expectedVersion이 일치하는 수정만 반영한다. 같은 버전으로 경쟁하는 수정 두 건 중 하나만 성공한다.
- 정렬된 요청 내용의 SHA-256으로 같은 requestId 재전송을 구분한다. 같은 ID의 다른 내용은 거부한다.
- 삭제 뒤에도 요청 결과는 보관해 재전송으로 여행이 부활하지 않게 한다.
- 잘못된 장소 일괄 추가는 전체 롤백한다. 날짜별 순서는 이동/삭제 후 다시 연속 번호로 정리한다.
- SQL에는 값을 바인딩하며, 앱 전용 저장 위치를 사용한다. 서버 보안 규칙의 대체물이 아니며 루팅 기기의 DB 변조를 막는 보장은 없다.
- 현재 DB 스키마는 버전 1이다. 향후 업그레이드 시 데이터를 삭제하는 자동 초기화 대신 명시적인 이관이 필요하다.
- 로컬 여행 DB는 자동 클라우드 백업과 기기 전송 대상에서 제외했다. 앱 삭제/데이터 초기화 시 여행이 삭제된다. 별도 내보내기/복원 기능은 아직 없다.

Android 저장 방식 참고: https://developer.android.com/training/data-storage/sqlite

## 검증

Repair_API36 / emulator-5554에서 실제 Android SQLite 엔진을 사용해 다음 8개 테스트가 통과했다.

1. 데이터베이스 닫기/재열기 후 유지, 키 순서가 다른 동일 요청 재전송, 여행 수정·삭제, 삭제 후 재전송.
2. 비로그인/다른 계정 접근 차단, 계정별 요청 ID 분리.
3. 일괄 저장 실패 롤백, 실패 요청 수정 후 재사용, 중복 URL 거부.
4. 일정 배치·순서·시간·메모·다른 날 이동·취소, 장소 수정·삭제·확정, 기간 밖 일정 거부.
5. 별도 SQLite 연결 2개가 동일 버전으로 동시 수정할 때 1개만 성공.
6. 페이지 조회, 잘못된 날짜/좌표 거부, URL 수동 입력 안내가 자동 저장하지 않음.
7. SQLite로 FE 참고 여행 저장/조회.
8. 실제 앱의 홈 → 저장된 여행 → 일정 → 결과 화면에서 주소·시간·메모 확인.

추가로 가상 기기의 Wi-Fi/모바일 데이터를 끄고 앱을 강제 종료·재실행했다. `Active default network: none` 상태에서도 저장된 여행이 홈에 표시되는 것을 캡처로 확인했고 네트워크는 원래대로 켰다.

`assembleDebug`, `assembleDebugAndroidTest`, `lintDebug` 통과. 기본 Firebase 설정(-PuseFirebaseEmulators=false) 앱 빌드도 통과했다. 실제 휴대폰 설치 및 운영 계정의 신규 로그인은 이번 검증에 포함하지 않았다.

## 실행/재현

Android Studio에서는 `Harudam2`를 열고 기본 앱을 실행한다. 기본 앱은 Firebase 운영 로그인과 SQLite를 사용하며 여행 저장용 서버는 필요 없다.

이 PC에서 사용한 명령(PowerShell):

```powershell
Set-Location Harudam2
# 기본 앱: 운영 Firebase 로그인 + SQLite
.\gradlew.bat --offline -I ..\..\harudam-delivery\green-build.init.gradle '-PuseFirebaseEmulators=false' assembleDebug

# 테스트용 인증/프로필만 실행하는 별도 터미널 (Node 22, Firebase CLI 필요)
firebase emulators:start --config backend/firebase.json --project demo-harudam --only auth,firestore

# SQLite 테스트/화면 테스트 빌드
.\gradlew.bat --offline -I ..\..\harudam-delivery\green-build.init.gradle '-PuseFirebaseEmulators=true' assembleDebug assembleDebugAndroidTest lintDebug
& C:\AndroidStudioSDK\platform-tools\adb.exe install -r C:\Android_Projects\HarudamGreenBuild\app\outputs\apk\debug\app-debug.apk
& C:\AndroidStudioSDK\platform-tools\adb.exe install -r C:\Android_Projects\HarudamGreenBuild\app\outputs\apk\androidTest\debug\app-debug-androidTest.apk
& C:\AndroidStudioSDK\platform-tools\adb.exe shell am instrument -w -e class com.example.harudam.SQLiteTravelStoreTest,com.example.harudam.LocalDesignFixtureTest,com.example.harudam.LocalTravelUiTest com.example.harudam.test/androidx.test.runner.AndroidJUnitRunner
```

SQLiteTravelStoreTest 자체는 Firebase 에뮬레이터 없이 실행할 수 있다. 나머지 두 테스트만 demo Auth에 테스트 계정을 만든다. 새 PC에서는 설치된 SDK/Node 경로를 사용하고 로컬 출력 변경용 `-I`와 캐시용 `--offline`은 생략한다. 테스트 DB는 고유 이름을 사용하고 테스트 후 자기 DB만 삭제한다.

## 브랜치

시작 BE는 6e6d10ca05a9cb4b2a6c56041d6dec7e1d7a9f16이다. FE/main은 읽기 전용을 유지한다.

- FE: 0ea0e763a40674c105335fc6ac467ec5a79eba2f
- main: 852b4077049b4b74096617cb8c98956719786a39

이전 BE_VERIFICATION_2026-11-28.md는 전환 이전 Functions 연결 검증 기록이다. 현재 앱 실행 방법은 이 문서를 따른다.
