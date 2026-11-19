# 검증 결과

검증일: 2026-11-19

## 통과

- 최종 소스: Gradle assembleDebug + lintDebug BUILD SUCCESSFUL
- Android Lint: 오류 0, 경고 32, 힌트 1. Kotlin/Compose 문자열·접근성 및 의존성 권고 등이 남음. 경고를 무시하는 설정이나 baseline 추가로 숨기지 않음.
- Firebase Auth/Firestore/Functions 로컬 통합 시험: testbackend.js 전체 성공, 18개 통과 로그
- Kotlin TravelModelsTest: JUnitCore로 3개 시험 성공 (날짜 생성, Callable 숫자 타입 변환, 미배치 제외·순서, 선택 시간 및 메모 응답)
- Firestore 규칙 컴파일 및 운영 프로젝트 배포 성공
- git diff --check 검사

## 시험 환경과 한계

- 로컬 Firebase 시험은 demo-harudam 프로젝트를 사용하여 실제 데이터를 건드리지 않음.
- 이 PC의 Node는 24, 배포 runtime은 nodejs22. CLI가 호스트 Node 24를 사용한다는 경고가 있었음.
- Gradle의 단위 테스트 실행기는 Windows PATH의 잘못된 따옴표와 경로 문제로 실행 실패. 동일하게 컴파일된 모델/테스트 클래스를 임시 영문 경로에서 JUnitCore로 실행하여 3개 통과. Gradle testDebugUnitTest 성공으로 보고하지 않음.
- 현재 ADB 기기 없음. Android 가상 기기도 시작 중 멈추거나 종료되어 이번 APK의 실제 화면 조작·시각 검증은 하지 못함.
- Google 로그인/비밀번호 재설정 코드는 연결했으나 실제 계정 선택·메일 수신은 미검증.
- 운영 Functions 배포는 Firebase Spark 제한으로 실패. cloudbuild.googleapis.com 및 Artifact Registry 활성화에 Blaze가 필요하다는 CLI 오류 확인. 결제/요금제 설정을 변경하지 않음.
- 지도는 외부 앱/브라우저 연결이며 임베드 지도·장소 URL 자동 분석을 완료했다고 주장하지 않음.

## Firebase 상태

projectId: mobile-pg-b27d4
Firestore Rules: 배포 성공
travelApi(asia-northeast3): 배포 미완료

운영 사용 전 소유자가 Blaze 전환 조건을 결정하고 docs/INTEGRATION.md의 배포 절차를 진행해야 함. 이 제한은 코드 오류가 아닌 프로젝트 요금제 제한임.
