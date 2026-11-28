# 하루담 BE 검증 기록 — 2026-11-28 (한국 시간)

## 완료 범위와 운영 제한

BE에서 FE 실제 화면 소스를 통합하고 인증, 여행 CRUD, 장소 URL 미리보기/확인 저장, 일정 배치/순서 변경, 결과 조회를 연결했다. 무료 로컬 환경에서 백엔드 통합 테스트와 Android 단말 테스트를 통과했다. **운영 서비스 전체 완료 상태는 아니다.** 운영 Functions가 비어 있고 결제가 연결되지 않아 현재 Callable 기반 여행 기능은 운영에서 사용할 수 없다. Spark를 유지했고 결제 연결, 유료 전환, Functions 배포, 운영 데이터 쓰기는 수행하지 않았다.

직접 Firestore 쓰기로 우회하지 않았다. 현재 서버 전용 쓰기 규칙과 소유권/버전/중복 요청 보장을 유지했다. Spark에서 운영 여행 저장을 제공하려면 별도의 검증된 직접 쓰기 보안 설계 또는 운영 서버가 필요하며, 이번 커밋에 그 전환은 포함되지 않는다. Cloud Functions 배포에는 Blaze가 필요하다: https://firebase.google.com/docs/functions/faq-and-troubleshooting

## 브랜치

- 시작 BE: e9224949be4aa3abbb0683f5fb1468339c72fa4b
- 읽기 전용 최신 FE: 0ea0e763a40674c105335fc6ac467ec5a79eba2f
- 읽기 전용 main: 852b4077049b4b74096617cb8c98956719786a39
- 작업 전후 `git ls-remote origin refs/heads/BE refs/heads/FE refs/heads/main`으로 FE/main 동일함 확인. FE는 커밋 객체만 fetch해 읽었으며 해당 브랜치를 체크아웃/커밋/푸시하지 않았다.
- 시작 작업 트리에 남아 있던 FE 화면 통합 및 createTrip 초기 장소 저장 변경을 보존해 완성·검증했다.

## 구현

- 민트/아이보리 홈과 생성 화면, 최신 FE 일정 배정 진행률/장소 선택/드래그 배치, 결과 카드와 날짜 선택을 실제 서버 응답에 연결했다.
- 초기 장소 목록을 여행과 함께 하나의 서버 트랜잭션으로 저장한다. 최대 100개 검증, 서버 ID 발급, nullable 좌표, requestId 중복 방지와 기존 버전/소유권 검사를 유지한다.
- 여행/장소 수정 화면과 비밀번호 재설정 이메일 요청을 연결했다. 일정 결과에서 날짜·시간·메모 수정, 배치 취소, 실제 장소 지도 링크 열기를 지원한다.
- URL 미리보기는 자동 저장하지 않는다. 사용자가 장소 정보를 확인한 후 저장하며 추출 실패 시 수동 입력이 가능하다.
- 로컬 opt-in 빌드는 `demo-harudam` 전용 Firebase 앱 설정과 Auth/Firestore/Functions 에뮬레이터를 사용한다. 네트워크 실패 시 운영으로 대체하지 않는다. 합성 키는 유효한 형식만 갖춘 테스트 값이며 운영 인증 정보가 아니다.
- 로컬 화면에 운영 데이터와 분리된 테스트임을 표시한다. 기본 빌드는 기존 mobile-pg-b27d4 설정을 유지한다.
- 런타임 내장 지도/경로 API는 구현하지 않았다. 지도 영역을 실제 지도인 것처럼 표시하지 않고 외부 지도 링크를 제공한다.

## 운영 읽기 전용 확인

Firebase CLI 로그인 계정으로 Google 관리 API를 조회했고 토큰/비밀 값은 출력하거나 저장하지 않았다.

| 확인 | 관측 |
|---|---|
| 프로젝트 목록 | mobile-pg-b27d4 ACTIVE, 표시명 Harudam |
| 결제 | Cloud Billing API HTTP 200, billingEnabled=false |
| Functions | `firebase functions:list --project mobile-pg-b27d4 --json` 결과 [] |
| 이메일/비밀번호 | Identity Toolkit 관리 설정 enabled=true |
| Google 공급자 | google.com 설정 enabled=true |
| Firestore | (default), asia-northeast3, FIRESTORE_NATIVE |
| 배포 규칙 | cloud.firestore release 2026-11-19T04:10:31.200509Z |
| 규칙 소스 | 운영 ruleset 소스가 로컬 firestore.rules와 줄바꿈 정규화 후 일치 |
| Android 등록 | 로컬 google-services.json의 appId/package가 운영 Android 등록과 일치 |

운영 로그인 성공, 실제 이메일 수신, Google 계정 선택/서명 SHA 설정, 운영 CRUD는 실행 검증하지 않았다. 공급자 활성화와 앱 등록 일치는 로그인 전체 성공을 증명하지 않는다.

## 검증 결과

- Node 22.23.5 parser/Callable 단위 테스트: 29개 통과.
- Node 22 무료 Auth/Firestore/Functions 통합 테스트: 전체 통과. 비로그인/비소유자 차단, 프로필 규칙, 직접 쓰기 거부, 여행 CRUD, 장소 일괄 저장/수정/삭제, 날짜 배치/이동/순서/메모/시간, 동시 수정 1건 성공, 오래된 버전 차단, 동일 requestId 재전송/다른 내용 거부, 초기 장소 원자적 저장 검증.
- Android `assembleDebug`, `assembleDebugAndroidTest`, `lintDebug`: 통과.
- `-PuseFirebaseEmulators=false` 기본 운영 설정 `assembleDebug`: 통과. 운영 연결 성공을 의미하지 않는다.
- Repair_API36 / emulator-5554: 앱 설치/실행 성공.
- `LocalDesignFixtureTest`: 로컬 프로젝트 ID 확인, Firebase 인증, FE 참고 여행 3개와 일정 저장/조회 통과.
- `LocalTravelUiTest`: 별도 로컬 계정/여행 생성 → 홈의 저장된 제목 클릭 → 일정 진행률/장소 확인 → 일정 확정 → 실제 주소/시간/메모/지도 링크 표시 통과.
- 실제 휴대폰 연결 없음. 화면 테스트 데이터는 demo 프로젝트에서만 생성했다.

## 재현 명령 (PowerShell)

다른 에뮬레이터 인스턴스를 종료하고 실행한다. Node 22, Java 21 이상, Firebase CLI, Android SDK 및 설치된 Gradle 의존성이 필요하다.

```powershell
# 저장소 루트에서: Node 22가 PATH에 있으면 -NodeDirectory 생략
.\Harudam2\backend\local.ps1 -Action test -NodeDirectory 'C:\Users\정가현\AppData\Local\npm-cache\_npx\52027bd8fc0022aa\node_modules\node\bin'

# Android 실행 동안 열어 둘 별도 터미널
.\Harudam2\backend\local.ps1 -Action start -NodeDirectory 'C:\Users\정가현\AppData\Local\npm-cache\_npx\52027bd8fc0022aa\node_modules\node\bin'

Set-Location .\Harudam2
.\gradlew.bat --offline -I ..\..\harudam-delivery\green-build.init.gradle '-PuseFirebaseEmulators=true' '-PfirebaseEmulatorHost=10.0.2.2' assembleDebug lintDebug assembleDebugAndroidTest

# 이 PC에서 사용한 출력 위치; 위 init script가 C:/Android_Projects/HarudamGreenBuild로 출력만 이동
& C:\AndroidStudioSDK\platform-tools\adb.exe install -r C:\Android_Projects\HarudamGreenBuild\app\outputs\apk\debug\app-debug.apk
& C:\AndroidStudioSDK\platform-tools\adb.exe install -r C:\Android_Projects\HarudamGreenBuild\app\outputs\apk\androidTest\debug\app-debug-androidTest.apk
& C:\AndroidStudioSDK\platform-tools\adb.exe shell am instrument -w -e class com.example.harudam.LocalDesignFixtureTest,com.example.harudam.LocalTravelUiTest com.example.harudam.test/androidx.test.runner.AndroidJUnitRunner
& C:\AndroidStudioSDK\platform-tools\adb.exe shell am start -n com.example.harudam/.MainActivity

# 운영 설정 빌드만 검사. 로컬 테스트 APK로 운영 테스트를 실행하면 보호 검사에서 실패해야 함.
.\gradlew.bat --offline -I ..\..\harudam-delivery\green-build.init.gradle '-PuseFirebaseEmulators=false' assembleDebug
```

새 PC에서는 위 캐시 Node 경로와 init script 대신 설치한 Node 22와 일반 `gradlew.bat` 명령을 사용한다. `--offline`은 의존성이 이미 내려받아진 이 PC용이다. 로컬 서버가 재시작되면 휘발성 인증/여행 데이터가 초기화되므로 테스트를 다시 실행한다.

## 실행 중 실패 및 남은 검증

- 최초 로컬 통합 테스트는 TEST_PROJECT_ID 미지정으로 안전하게 중단됐다. demo-harudam을 명시해 재실행 통과했으며 local.ps1에 고정했다.
- Node 22 재검증 중 두 에뮬레이터를 겹쳐 시작한 실행은 포트 충돌로 실패했다. 종료 후 단독 실행으로 전체 통과했다.
- Android 초기 합성 키의 형식 검증 오류를 단말에서 발견해 유효 형식의 가짜 키/앱 ID로 수정했고 최종 단말 테스트가 통과했다.
- 중간 lint 실행에서 분석기 내부 오류가 발생했다. 소스 확정 후 다시 실행해 통과했다.
- 네이버 실제 페이지의 현재 파싱 성공률, 운영 인증 전체, 실제 이메일 수신, Google 단말 로그인, 내장 지도 렌더링/길찾기, 실제 휴대폰은 미검증 또는 미구현이다. 기존에 관측된 JS 전용 페이지/429 등의 공급자 제한은 수동 확인 입력으로 처리한다.
- 최종 화면 캡처와 APK는 저장소 밖 같은 워크스페이스 harudam-delivery/2026-11-28에 보관한다. 테스트가 표시한 데이터는 운영 사용자 데이터가 아니다.
