# FoldStand

Galaxy Z Fold를 반쯤 접어 탁자에 두었을 때 시계와 무드등으로 사용하는 Android 앱입니다. 충전 여부와 관계없이 홈의 **스탠바이 시작** 버튼으로 즉시 실행할 수 있습니다.

## 구현된 기능

- 수동 StandBy 시작(충전 상태로 차단하지 않음)과 실행 상태 복원
- `FoldingFeature`의 상태, 방향, 실제 `bounds`, occlusion, separating 값을 사용하는 폴더블 자세 감지
- 가로 힌지: 위 시계/아래 무드등, 세로 힌지: 왼쪽 시계/오른쪽 무드등
- 펼침 또는 일반 스마트폰: 현재 창 비율에 맞춘 안전한 50:50 대체 레이아웃
- Apple StandBy에서 영감을 받은 큰 저휘도 시계, 날짜·배터리 표시와 켬/끔/자동 야간 모드
- 단색/그라데이션/오로라 무드등과 세 가지 사용자 색상, 기본 엠비언트 색상 선택
- 좌우 스와이프 시계 → 위젯 → 달력 → 알림 페이지, 상하 스와이프 무드등 변경
- 둥근 위젯 카드에 실제 기기 캘린더 일정과 최근 알림 표시(전체 기능본)
- 탭 조작 패널, 길게 눌러 빠른 설정, 2단계 뒤로가기 종료
- StandBy에서만 적용되는 immersive UI, 앱 내부 밝기, `FLAG_KEEP_SCREEN_ON`
- 자동 어둡게, 15/30fps 애니메이션, 90초 주기 번인 방지 이동, 조도센서 기반 자동 야간 모드
- DataStore를 통한 모든 설정과 실행 상태 영속화
- GitHub Releases 기반 업데이트 확인·APK 다운로드·Android 설치 화면 연결
- `WindowAreaController`의 `TYPE_REAR_FACING` + `OPERATION_PRESENT_ON_AREA` capability 기반 듀얼 화면 실험 기능
- 네트워크, 계정, 위치, 카메라, 마이크, 저장소 권한 없음

캘린더를 처음 열 때만 `READ_CALENDAR`를 요청합니다. 알림은 Android의 알림 접근 설정에서 사용자가 직접 FoldStand를 허용한 경우에만 최근 12개를 기기 안에 보관합니다. 두 권한을 거부해도 시계와 무드등은 정상 실행됩니다.

## 빌드

필요 환경:

- JDK 17
- Android SDK Platform 37.0
- Android SDK Build Tools 36.0.0 이상

Android Studio에서 이 폴더를 열거나 터미널에서 다음을 실행합니다.

직접 설치본은 브라우저 다운로드를 막는 Play 프로텍트의 알림 접근 차단을 피하기 위해 다른 앱의 알림 리스너 서비스를 포함하지 않습니다. 시계·무드등·캘린더·업데이트는 그대로 사용할 수 있습니다. 알림 접근까지 필요하면 전체 기능본을 ADB나 Play 스토어 배포로 설치합니다.

```bash
./gradlew assembleDirectDebug
./gradlew assembleFullDebug
```

생성된 설치 파일:

```text
app/build/outputs/apk/direct/debug/app-direct-debug.apk  # 브라우저 직접 설치용
app/build/outputs/apk/full/debug/app-full-debug.apk      # 알림 접근 포함
```

이 저장소에서 검증한 명령:

```bash
./gradlew testDirectDebugUnitTest testFullDebugUnitTest assembleDirectDebug assembleFullDebug
./gradlew lintDirectDebug lintFullDebug
```

단위 테스트는 폴드 레이아웃 5종, 설정 기본값/영속화, 배터리 경계값, 듀얼 화면 상태 매핑을 포함합니다. UI 테스트 APK에는 미충전 상태에서도 시작 버튼이 활성화되는 검사가 포함됩니다.

## 앱 안에서 업데이트

앱은 `https://api.github.com/repos/blossom0948/foldstandby/releases/latest`를 확인합니다. 새 Release에 배포판에 맞는 APK asset이 있으면 홈 화면에 업데이트 카드가 나타나고, HTTPS 리다이렉트 확인·임시 파일 저장·재시도·파일 크기 검증을 거친 뒤 다운로드 후 Android 설치 화면을 엽니다. Android 8 이상에서는 최초 1회 FoldStand의 **알 수 없는 앱 설치 허용**이 필요합니다.

현재 Release에는 `foldstand-safe.apk`(직접 설치본)와 `foldstand-full.apk`(알림 접근 포함)가 함께 제공됩니다. 브라우저에서 설치가 차단되면 직접 설치본을 사용하고, 전체 기능본은 `adb install -r foldstand-full.apk` 또는 Play 스토어 배포 경로를 사용합니다.

현재 배포 방식은 GitHub Release APK입니다. Google Play에 게시하게 되면 Play In-App Updates로 교체할 수 있지만, GitHub에서 직접 설치한 앱에는 Play Core 업데이트가 적용되지 않으므로 현재 방식이 이 프로젝트에 맞는 업데이트 경로입니다.

## 휴대폰 설치

1. 휴대폰의 개발자 옵션에서 **USB 디버깅**을 켭니다.
2. USB로 휴대폰을 연결하고 디버깅 허용 창을 승인합니다.
3. 프로젝트 루트에서 다음을 실행합니다.

```bash
adb install -r app/build/outputs/apk/direct/debug/app-direct-debug.apk
```

또는 `app-direct-debug.apk`를 휴대폰으로 복사해 파일 앱에서 열 수 있습니다. 이 경우 Android가 요청하면 해당 파일 앱의 **알 수 없는 앱 설치**를 일시적으로 허용합니다. 알림까지 필요하면 전체 기능본을 ADB로 설치하세요.

## 사용법

- 홈에서 **스탠바이 시작**을 누릅니다. 충전 중이 아니어도 동작합니다.
- StandBy 화면 한 번 탭: 조작 패널 표시/숨김
- 좌우 스와이프: 시계 스타일 변경
- 위아래 스와이프: 무드등 프리셋 변경
- 길게 누르기: 밝기·색상·종료 빠른 설정
- 뒤로가기: 첫 번째는 조작 패널, 두 번째는 종료 확인

## 듀얼 화면 실험 기능의 실제 제약

앱은 특정 Galaxy 모델명을 보고 지원 여부를 추측하지 않습니다. AndroidX WindowManager가 런타임에 capability를 `AVAILABLE`로 반환한 경우에만 버튼이 활성화되며, 시스템 승인을 받은 뒤 보조 화면 세션을 시작합니다. 지원되면 내부 화면에는 무드등, 보조 화면에는 시계를 표시합니다. 세션 실패·종료 또는 앱 백그라운드 진입 시 즉시 일반 내부 화면 분할 모드로 돌아갑니다.

공식 Android 문서가 듀얼 화면 모드를 명시적으로 보장하는 대상은 Pixel Fold/Android 14 이상입니다. Galaxy Z Fold에서는 대부분 **지원하지 않음** 또는 **현재 사용할 수 없음**으로 표시될 수 있으며, 이는 오류가 아니라 안전한 fallback입니다.

## 실제 기기에서 확인할 체크리스트

자동 빌드와 테스트는 완료했지만 이 개발 환경에는 Galaxy Z Fold 실기기가 없으므로 다음 항목은 기기에서 최종 확인해야 합니다.

- 완전히 펼친 상태와 약 70~120도 반접힘 상태의 실제 힌지 정렬
- 세로/가로 회전과 접었다 다시 펼친 뒤 상태 복원
- 충전 중/미충전 상태에서 각각 수동 시작
- 화면 계속 켜기 설정을 켜고 끈 직후의 동작
- 15분 이상 실행했을 때 발열, 번인 방지 이동, 배터리 소모
- TalkBack 및 큰 글꼴 배율에서 핵심 조작 접근성
- capability 지원 기기의 듀얼 화면 승인/시작/종료/실패 fallback
- 캘린더 권한 허용/거부, 전체 기능본의 알림 접근 허용/거부, GitHub Release 업데이트 다운로드·설치
- 설정에서 야간 모드 켬/끔/자동 전환, 자동 모드에서 조도센서에 따른 밝기·붉은 글자 변화

## 프로젝트 구조

```text
app/src/main/java/com/blossom/foldstand/
├── data/       # DataStore, 배터리 상태
├── domain/     # 설정/UI/폴드 상태 모델
├── fold/       # WindowManager 관찰, 레이아웃 계산, 듀얼 화면
├── ui/         # 홈, StandBy, 설정, 테마
└── viewmodel/  # 앱 전체 UDF 상태와 사용자 이벤트
```

패키지명은 `com.blossom.foldstand`, 최소 지원 버전은 Android 8.0(API 26)입니다.
