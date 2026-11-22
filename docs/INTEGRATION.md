# 하루담 프론트·백엔드 연동

## 구현 범위

기존 남색·흰색 카드 화면 흐름을 유지하면서 정적 예시 화면을 실제 상태를 받는 TravelScreens로 교체했습니다.

- 이메일 회원가입·로그인·로그아웃·비밀번호 재설정
- Credential Manager를 통한 Google 로그인/가입 호출과 Firebase 사용자 프로필 저장
- 로그인 세션 복원, 로그아웃 시 여행 화면 상태 제거
- 여행 생성·기간/이름 수정·목록·상세·삭제 확인
- 장소 이름·주소·링크·분류 입력과 수정·삭제·확정
- 미배치 장소 선택, 날짜별 배치·날짜 이동·배치 취소
- 일정의 방문 시간·메모, 방문 순서 올리기/내리기
- 실제 장소 링크 또는 지도 앱 검색 열기, 일자별 일정 텍스트 공유
- 로딩·입력 검증·권한 오류·버전 충돌·중복 저장 방지·동일 요청 재시도

지도는 외부 지도 앱/브라우저를 엽니다. 앱 안의 지도 SDK 임베드, 동행자 공동 편집, 사진·후기는 이번 프론트에 포함되지 않아 구현 범위에 넣지 않았습니다. 이름 없는 링크 입력에는 장소 이름을 직접 입력하도록 안내합니다.

2026년 11월 22일 BE에 `travelApi`의 `parsePlaces` 작업을 추가했습니다. 네이버 개별 장소 링크 1~10개의 공개 메타데이터를 확인하는 백엔드 미리보기이며 Android 입력 화면에는 아직 연결되지 않았습니다. 실제 네이버 HTTP 429 접근 제한을 확인했으므로 자동 추출 성공은 보장하지 않습니다. [API 연결 방법](PARSE_PLACES_API.md)과 [BE 및 FE 작업 보고서](BE_FE_백엔드_작업보고서_2026-11-22.md)를 참고하세요.

## 프로젝트 열기

Android Studio에서 저장소 안의 **Harudam2**를 엽니다. applicationId는 `com.example.harudam`입니다.

핵심 파일:
- `app/.../ui/screens/TravelScreens.kt`: 여행 목록→입력→장소→날짜 배치→일정 화면
- `app/.../viewmodel/TravelViewModel.kt`: 비동기 요청, 선택 여행, 실패/재시도 상태
- `app/.../repository/TripRepository.kt`: asia-northeast3의 travelApi Callable 호출 및 응답 변환
- `app/.../model/TravelModels.kt`: 응답 모델과 날짜/순서 표시
- `backend/functions/index.js`: 인증·소유권·기간·버전·장소/일정 처리
- `backend/functions/testbackend.js`: Firebase 로컬 통합 시험

## 실제 Firebase 상태

- 앱에 연결된 프로젝트: `mobile-pg-b27d4` (Harudam)
- 함수 리전: `asia-northeast3`, 함수 이름: `travelApi`
- Firestore 규칙: 이번 작업에서 실제 배포 성공
- Functions: 기존 배포된 함수 없음. 이번 배포는 Spark 요금제로 Cloud Build API를 활성화하지 못해 실패
- 결제 계정 연결이나 Blaze 업그레이드는 수행하지 않았음

따라서 현재 일반 APK로 여행 서버 기능을 사용하려면 프로젝트 소유자가 Blaze 전환 조건을 확인하고 진행한 뒤 함수를 배포해야 합니다. 앱 코드 존재와 운영 서버 가동을 구분해야 합니다.

```powershell
cd Harudam2/backend
firebase deploy --only functions:travelApi,firestore --project mobile-pg-b27d4
```

Google 로그인은 코드 연결을 완료했지만 실제 계정 로그인 시험은 하지 못했습니다. Firebase 콘솔에서 Google 제공자, 사용하는 빌드 서명의 SHA-1/SHA-256, 최신 google-services.json을 확인해야 합니다. 운영 개인정보 처리방침은 개발용 데이터 안내 문구와 별도로 확정해야 합니다.

## 로컬 백엔드 테스트

Node.js 22, Java 및 Firebase CLI를 준비합니다. 실사용 데이터와 분리된 demo 프로젝트만 사용합니다.

```powershell
cd Harudam2/backend/functions
npm ci
cd ..
$env:TEST_PROJECT_ID='demo-harudam'
firebase emulators:exec --project demo-harudam --only auth,firestore,functions "node functions/testbackend.js"
```

현재 로컬 PC는 Node 24를 사용하여 CLI에서 Node 22와 다른 버전이라는 경고가 발생했습니다. 배포 런타임은 nodejs22로 고정되어 있습니다.

시험 항목: 여행 생성·수정·삭제, 페이지 조회, 중복 재전송, 순서가 다른 키의 동일 요청, 같은 ID의 다른 요청 거부, 실제 동시 수정 충돌, 날짜·시간 검증, 장소 수정·삭제, 날짜 이동·배치 취소, 시간·메모 수정 시 순서 유지, 비로그인·비소유자 접근 차단, Firestore 직접 쓰기 차단, 회원가입 프로필 보안 규칙.

## Android 앱을 로컬 Firebase에 연결

로컬 테스트 전용 APK는 다음 속성으로 빌드합니다. 일반 빌드에는 기본적으로 운영 프로젝트를 사용합니다.

```powershell
cd Harudam2
.\gradlew.bat assembleDebug -PuseFirebaseEmulators=true -PfirebaseEmulatorHost=10.0.2.2
```

- Android 가상 기기: `10.0.2.2`
- USB 실기기: `adb reverse tcp:5001 tcp:5001`, `adb reverse tcp:8080 tcp:8080`, `adb reverse tcp:9099 tcp:9099` 후 host를 `127.0.0.1`로 빌드
- Firebase CLI 실행 프로젝트 ID와 Android 설정의 프로젝트 ID가 일치해야 합니다. Android E2E에서는 테스트용 Firebase 설정 파일을 별도로 준비하거나 앱 프로젝트 ID와 같은 ID로 로컬 서비스를 실행하고 세 서비스 모두 로컬로 연결했는지 확인합니다.
- 테스트 빌드의 계정은 Auth Emulator에서 별도로 만듭니다. 운영 계정과 섞지 않습니다.
- HTTP 테스트 연결은 debug manifest에만 허용됩니다. release에는 허용하지 않습니다.

## 저장과 실패 처리

변경 요청은 UUID requestId와 expectedVersion을 사용합니다. 실패 후 동일 작업 재시도에는 요청 객체를 그대로 유지합니다. 서버는 정규화된 키 순서로 요청을 비교합니다.

저장 응답을 받기 전에는 완료로 표시하지 않습니다. 결과가 불명확하면 같은 요청 재시도 또는 최신 내용 확인을 선택합니다. 버전 충돌은 최신 여행을 불러와 확인하도록 안내하며 자동 덮어쓰기하지 않습니다. 서버 저장 성공 후 재조회만 실패한 경우 저장 완료와 갱신 실패를 구분하고 입력창을 닫아 중복 생성 가능성을 줄입니다.

현재 데이터 구조는 여행 문서 안의 places 배열을 사용하며 여행당 최대 100개, 소유자만 접근하는 MVP입니다. 기존 문서 구조는 변경하지 않았고 선택형 visitTime 필드만 추가했습니다. 향후 공동 편집은 별도 권한·모델 변경이 필요합니다.

## Android 검증

```powershell
cd Harudam2
.\gradlew.bat assembleDebug lintDebug testDebugUnitTest
```

이 PC의 PATH에 포함된 잘못된 따옴표와 한글 사용자/소스 경로 때문에 Gradle 테스트 작업자 실행에 문제가 있었습니다. Gradle 테스트 실행기는 성공하지 못했습니다. 같은 컴파일 결과의 모델·테스트 클래스를 임시 영문 경로에서 JUnitCore로 직접 실행하여 TravelModelsTest 3개를 통과했습니다. 전역 PATH나 사용자 시스템 설정은 변경하지 않았습니다. 자세한 결과는 VERIFICATION.md에 기록했습니다.

실제 휴대폰이 연결되지 않았고 Android 가상 기기는 부팅에 실패했습니다. 따라서 화면의 실제 조작·레이아웃 승인·Google 계정 선택·외부 지도/공유 대상 전달은 별도 기기 검증이 남아 있습니다.

