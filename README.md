# 하루담

Kotlin Android 여행 일정 앱입니다. 현재 BE 앱은 **여행·장소·일정을 기기 내부 SQLite에 저장**하며, 로그인·회원가입은 기존 Firebase Auth와 사용자 프로필 연결을 유지합니다.

Android Studio에서 `Harudam2` 폴더를 열어 주세요.

- [SQLite 실행 방법과 검증 결과](docs/SQLITE.md)
- [기존 Firebase 서버 구현과 배포 기록](docs/INTEGRATION.md)

여행 CRUD와 일정 편집은 Functions 배포 없이 사용할 수 있습니다. 같은 계정의 다른 기기로 여행을 동기화하지 않으며, 기존 Firestore 여행을 자동으로 옮기지 않습니다. SQLite 모드의 장소 링크는 직접 확인한 이름·주소와 함께 저장합니다. 네이버 자동 추출은 현재 이 모드에서 사용하지 않습니다.

기존 서버 코드는 `Harudam2/backend`에 보존했습니다. 운영 Firebase 규칙·결제·데이터는 이번 전환에서 변경하지 않았습니다.
