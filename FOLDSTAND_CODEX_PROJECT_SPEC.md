# FoldStand — Galaxy Fold 탁상형 스탠바이·무드등 앱

> 이 문서는 Codex가 새 Android 프로젝트를 처음부터 생성하고, 실제 Galaxy Z Fold 계열 기기에서 설치·실행·검증 가능한 MVP를 완성하기 위한 작업 지시서다. 질문만 하거나 예제 코드 일부만 제시하지 말고, 프로젝트 파일을 직접 생성·수정하고 빌드까지 완료한다.

## 0. 프로젝트 한 줄 정의

Galaxy Z Fold를 반쯤 접어 탁자에 놓았을 때 한쪽 영역에는 시계·날짜·배터리를, 다른 영역에는 부드러운 무드등을 보여 주는 Android 앱이다. **충전 여부와 관계없이 사용자가 언제든 직접 시작할 수 있어야 한다.**

앱 이름은 임시로 `FoldStand`, 패키지명은 `com.blossom.foldstand`로 한다. 기존 프로젝트가 있으면 기존 패키지명과 구조를 유지한다.

## 1. 반드시 지켜야 할 현실적 범위

### 1.1 필수 동작: 모든 일반 Galaxy Z Fold 대상

- 앱 내부의 `스탠바이 시작` 버튼으로 충전 중이 아니어도 즉시 실행한다.
- 펼친 내부 디스플레이에서 기기가 `HALF_OPENED`일 때 힌지/접힘선을 기준으로 두 영역을 만든다.
- 탁상 자세에서는 위쪽 영역을 시계, 아래쪽 영역을 무드등으로 사용한다.
- 기기를 회전하거나 접는 방향이 달라지면 가로/세로 분할을 자동 전환한다.
- 완전히 펼치거나 폴더블 정보가 없는 일반 스마트폰에서도 단일 화면용 대체 레이아웃으로 동작한다.
- 폴드 상태가 바뀌어 Activity가 재생성돼도 선택한 시계·색상·밝기·실행 상태를 복원한다.

### 1.2 선택 동작: 내부 화면 + 커버 화면 동시 표시

사진처럼 내부 화면과 커버 화면에 서로 다른 내용을 동시에 표시하는 기능을 기본 전제로 하드코딩하지 않는다.

- `WindowAreaController`로 `TYPE_REAR_FACING` 영역과 `OPERATION_PRESENT_ON_AREA` 능력을 런타임에 조회한다.
- 상태가 `AVAILABLE`일 때만 `듀얼 화면 실험 기능` 버튼을 활성화한다.
- 사용자가 누른 경우에만 시스템 승인 흐름을 거쳐 `presentContentOnWindowArea()`를 시도한다.
- 지원 시: 기본 내부 화면에는 무드등, 보조/외부 화면에는 시계 UI를 표시한다.
- `UNSUPPORTED` 또는 `UNAVAILABLE`이면 버튼을 비활성화하고 `이 기기에서는 커버 화면 동시 표시를 지원하지 않습니다. 내부 화면 분할 모드를 사용해 주세요.`라고 안내한다.
- 오류·세션 종료 시 즉시 내부 화면 분할 모드로 복귀한다. 앱이 종료되거나 백그라운드로 가면 세션 참조를 정리한다.
- 특정 Galaxy 모델명을 검사해 지원한다고 가정하지 않는다. 오직 런타임 capability 결과를 신뢰한다.

공식 Android 문서상 듀얼 스크린 모드는 Pixel Fold/Android 14 이상에서 명시적으로 제공된다. Galaxy Z Fold에서 동일한 capability가 반환된다는 보장이 없으므로 이 기능은 베타/실험 기능으로 취급한다.

## 2. 권장 기술 스택

- Kotlin
- Jetpack Compose + Material 3
- 단일 Activity
- MVVM 또는 단순 UDF(Unidirectional Data Flow)
- Kotlin Coroutines + StateFlow
- DataStore Preferences
- Jetpack WindowManager
- Compose Material 3 Adaptive의 폴더블 API를 사용할 수 있으면 `collectFoldingFeaturesAsState()` 사용
- 최소 SDK: 26 이상
- target/compile SDK: 설치된 최신 안정 버전
- Gradle Version Catalog(`libs.versions.toml`)
- JDK 17

의존성 버전은 작성 시점의 안정 버전을 사용하고, 알파/베타 버전은 꼭 필요한 API 외에는 피한다. 듀얼 화면 API 때문에 사전 출시 버전이 필요하다면 그 부분만 격리하고 이유를 README에 기록한다.

## 3. 화면 구조

### 3.1 홈 화면

상단:

- 앱 이름 `FoldStand`
- 짧은 설명 `폴드를 탁자 위의 시계와 무드등으로`

중앙 미리보기 카드:

- 현재 선택된 시계 스타일과 무드등 효과를 축소 표시
- 기기가 폴더블이면 현재 자세를 `접힘`, `반접힘`, `펼침`으로 표시
- 일반 기기에서는 `일반 화면 모드` 표시

주요 버튼:

- 가장 크고 눈에 띄는 `스탠바이 시작`
- 이 버튼은 충전 상태와 무관하게 항상 활성화
- 보조 버튼 `스타일 설정`
- capability가 있을 때만 `듀얼 화면 실험 기능`

하단 상태 문구:

- 충전 중: `충전 중 · 배터리 72%`
- 미충전: `배터리 사용 중 · 72%`
- 충전 중이 아니어도 시작 가능하다는 문구를 최초 1회 안내

### 3.2 StandBy 화면

전체 화면 immersive UI로 표시한다. 상태바·내비게이션 바는 숨기되, 사용자가 제스처로 다시 불러올 수 있어야 한다.

시계 영역:

- 큰 시간 `23:48`
- 작은 날짜 `9월 18일 금요일`
- 선택 사항: 초 표시, 12/24시간제, 배터리 퍼센트, 충전 아이콘
- 기본 시계는 굵고 읽기 쉬운 산세리프 디지털 시계
- 시계 스타일: Digital Bold, Minimal, Flip, Analog의 4개. MVP에서는 Digital Bold와 Minimal을 완성하고 나머지는 기능 플래그로 숨겨도 된다.

무드등 영역:

- 단색, 그라데이션, 오로라, 선셋, 촛불의 5개 프리셋
- MVP 필수: 단색, 그라데이션, 오로라
- 화면 전체를 덮는 효과이며 별도 카드 테두리를 두지 않는다.
- 애니메이션은 갑작스럽게 변하지 않고 6~14초 주기로 천천히 이동한다.
- 기본 30fps 이하, 절전 모드 15fps 이하를 목표로 한다.
- 검정 배경 위에 저채도/저휘도 색을 사용하여 야간 눈부심을 줄인다.

조작:

- 한 번 탭: 최소 조작 패널 표시/숨김
- 좌우 스와이프: 시계 스타일 전환
- 위아래 스와이프: 무드등 프리셋 전환
- 길게 누르기: 밝기·색상·종료 버튼이 있는 설정 시트
- 뒤로가기 1회: 조작 패널 표시, 다시 누르기: 종료 확인

### 3.3 설정 화면

- 기본 시계 스타일
- 12/24시간제
- 초 표시
- 날짜 표시
- 배터리 표시
- 무드등 프리셋과 사용자 지정 색상 2~3개
- 앱 내부 밝기 5~100%
- 화면 계속 켜기: 켬/끔
- 자동 어둡게: 5분/15분/30분/사용 안 함
- 절전 애니메이션
- 번인 방지
- 시작 방식: 수동 실행(기본), 충전 시작 시 제안, 반접힘 감지 시 제안
- `충전해야만 실행`되는 옵션은 만들지 않는다.
- 실험실: 듀얼 화면 capability와 현재 상태를 사람이 읽을 수 있는 문구로 표시

## 4. 폴더블 자세 감지와 레이아웃 규칙

`FoldingFeature`에서 다음 값을 읽는다.

- `state`: `FLAT` / `HALF_OPENED`
- `orientation`: `HORIZONTAL` / `VERTICAL`
- `bounds`: 실제 접힘 영역
- `occlusionType`
- `isSeparating`

레이아웃 규칙:

1. `HALF_OPENED + HORIZONTAL`: 접힘선 위=시계, 아래=무드등.
2. `HALF_OPENED + VERTICAL`: 좌측=시계, 우측=무드등. 사용자가 설정에서 좌우 반전 가능.
3. `FLAT`: 화면 크기에 따라 50:50 분할. 가로 화면은 좌우, 세로 화면은 위아래.
4. 폴더블 정보 없음: 같은 단일 화면 대체 레이아웃.
5. 중요한 텍스트·버튼을 `bounds`와 겹치지 않게 한다.
6. 힌지 좌표를 dp로 임의 추정하지 말고 현재 window 좌표를 기준으로 계산한다.
7. `isSeparating == true` 또는 물리적으로 의미 있는 bounds가 있으면 두 pane 사이 간격에 반영한다.

자세 전환 시 화면이 번쩍이지 않도록 250~400ms의 부드러운 crossfade/placement animation을 적용한다. 시간 텍스트 자체에는 과한 이동 애니메이션을 넣지 않는다.

## 5. 충전과 수동 실행 정책

가장 중요한 요구 사항:

> `스탠바이 시작`은 배터리 상태, 케이블 연결, 무선 충전 여부를 검사해 차단해서는 안 된다.

- 충전 상태는 표시와 선택적 자동 제안에만 사용한다.
- 기본 시작 방식은 수동이다.
- 앱이 백그라운드에서 임의로 Activity를 강제 실행하지 않는다.
- `충전 시작 시 제안`은 알림 또는 앱 내부 안내로 구현하고 기본값은 끔이다.
- `반접힘 감지 시 제안` 역시 기본값은 끔이며 OS 제한을 우회하지 않는다.

## 6. 화면 유지, 밝기, 배터리, 번인 방지

- StandBy Activity가 전경일 때만 `FLAG_KEEP_SCREEN_ON`을 조건부 설정한다.
- 설정에서 끄면 즉시 flag를 해제한다.
- 서비스나 WakeLock으로 화면을 억지로 계속 켜지 않는다.
- 앱 자체 밝기는 해당 Activity의 `WindowManager.LayoutParams.screenBrightness`로만 조절한다. 시스템 전체 밝기를 바꾸는 권한을 요청하지 않는다.
- 배터리 사용 중 기본 밝기 25%, 충전 중 40%를 권장하되 사용자가 변경할 수 있다.
- 일정 시간 무조작 후 밝기를 천천히 낮추고, 화면을 터치하면 원래 값으로 복원한다.
- 번인 방지 활성화 시 시계·날짜 묶음을 60~120초 간격으로 2~6dp 범위에서 미세 이동한다.
- 완전한 흰색의 넓은 영역, 고정된 고휘도 요소, 항상 켜진 내비게이션 UI를 피한다.
- 배터리 부족(예: 15% 이하) 시 앱을 강제 종료하지 말고 `밝기 낮추기` 제안만 표시한다.

## 7. 아키텍처와 권장 파일 구조

```text
app/src/main/java/com/blossom/foldstand/
├── MainActivity.kt
├── FoldStandApp.kt
├── data/
│   ├── SettingsRepository.kt
│   └── SettingsDataStore.kt
├── domain/
│   ├── ClockStyle.kt
│   ├── AmbientPreset.kt
│   ├── FoldPosture.kt
│   └── StandbySettings.kt
├── fold/
│   ├── FoldStateObserver.kt
│   ├── FoldLayoutCalculator.kt
│   └── DualScreenController.kt
├── ui/
│   ├── navigation/FoldStandNavGraph.kt
│   ├── home/HomeScreen.kt
│   ├── standby/StandbyScreen.kt
│   ├── standby/ClockPane.kt
│   ├── standby/AmbientPane.kt
│   ├── settings/SettingsScreen.kt
│   ├── components/
│   └── theme/
└── viewmodel/
    ├── HomeViewModel.kt
    └── StandbyViewModel.kt
```

작은 프로젝트라면 지나치게 많은 추상화는 피하되, `DualScreenController`는 반드시 별도 파일로 격리한다. 지원되지 않는 기기에서도 해당 클래스 때문에 앱이 크래시하지 않아야 한다.

## 8. 상태 모델

최소 상태:

```kotlin
data class StandbyUiState(
    val isRunning: Boolean = false,
    val clockStyle: ClockStyle = ClockStyle.DigitalBold,
    val ambientPreset: AmbientPreset = AmbientPreset.Aurora,
    val use24Hour: Boolean = true,
    val showSeconds: Boolean = false,
    val showDate: Boolean = true,
    val showBattery: Boolean = true,
    val batteryPercent: Int? = null,
    val isCharging: Boolean = false,
    val brightness: Float = 0.25f,
    val keepScreenOn: Boolean = true,
    val burnInProtection: Boolean = true,
    val powerSavingAnimation: Boolean = true,
    val foldPosture: FoldPosture = FoldPosture.Unknown,
    val dualScreenStatus: DualScreenStatus = DualScreenStatus.Unsupported
)
```

시간은 StateFlow에 초마다 무조건 저장하지 않는다. 초 미표시 시 분 경계에서만 갱신하고, 초 표시 시에만 초 단위 ticker를 사용한다. 화면을 벗어나면 ticker를 취소한다.

## 9. 권한과 개인정보

MVP에는 계정, 네트워크, 위치, 카메라, 마이크, 저장소 권한이 필요하지 않다.

- 배터리/충전 상태는 공개 시스템 상태 API를 사용한다.
- 자동 제안 알림을 실제 구현하는 경우에만 Android 13+ 알림 권한을 맥락 속에서 요청한다.
- 접근성 서비스, 화면 오버레이, 기기 관리자 권한을 사용하지 않는다.
- 분석 SDK와 광고 SDK를 넣지 않는다.
- 인터넷 권한도 필요 없으면 Manifest에 넣지 않는다.

## 10. 디자인 가이드

전체 분위기는 iPhone StandBy를 그대로 복제하지 말고 One UI와 어울리는 차분한 자체 디자인으로 만든다.

- 배경: OLED black `#000000`
- 카드 배경: 필요할 때만 `#141414`
- 기본 글자: `#F5F5F5`
- 보조 글자: `#A6A6A6`
- 강조색: 선택한 무드등 색상에서 자동 추출하되 대비 확보
- 모서리: 홈 카드 24dp, 버튼 18~22dp
- 터치 영역: 최소 48dp
- 시계 숫자는 화면에서 가장 큰 시각 요소
- 조작 UI가 숨겨진 상태에서는 시계와 빛 외에는 거의 보이지 않게 한다.
- 기기 크기에 따라 글자 크기를 단순 고정 sp가 아니라 available bounds에 맞춰 제한 범위 내 계산한다.
- TalkBack 설명, 적절한 contentDescription, 색상 외 상태 표현을 제공한다.

## 11. 애니메이션 구현 원칙

- 무드등은 Compose `Canvas`와 그라데이션을 우선 사용한다.
- 오로라는 2~4개의 radial/linear gradient 중심을 매우 천천히 이동시켜 구현한다.
- 매 프레임 큰 객체나 Brush를 불필요하게 생성하지 않는다.
- `withFrameNanos` 또는 Compose animation API를 사용하되 화면 수명주기와 함께 정지한다.
- 앱이 background로 가면 애니메이션과 시간 ticker를 즉시 중지한다.
- `Animator duration scale = 0` 및 접근성의 모션 감소 환경에서도 기능이 깨지지 않게 정적 프레임으로 대체한다.

## 12. 구현 순서

1. 프로젝트 생성, Gradle 동기화, 빈 홈 화면 빌드.
2. 테마와 Navigation 구성.
3. DataStore 설정 저장/복원.
4. 시계 pane과 무드등 pane 구현.
5. StandBy 화면의 수동 시작/종료, immersive UI, 밝기와 화면 유지 구현.
6. Jetpack WindowManager로 폴드 상태를 관찰하고 자세별 분할 적용.
7. 번인 방지와 절전 애니메이션 적용.
8. 설정 화면 완성.
9. `DualScreenController` capability 검사와 안전한 fallback 구현.
10. 테스트, 실기기 확인 가이드, README 작성.

각 단계 후 컴파일 오류를 바로 해결한다. 마지막에 한꺼번에 해결하려 하지 않는다.

## 13. 테스트 요구사항

### 단위 테스트

- `FoldLayoutCalculator`: 가로/세로 fold, flat, unknown, bounds가 0인 경우
- 설정 기본값과 DataStore 직렬화
- 배터리 퍼센트 null/0/100 처리
- dual-screen 상태 매핑

### Compose/UI 테스트

- 홈에서 충전 여부와 무관하게 시작 버튼이 활성화되는지
- StandBy 진입과 종료
- 설정 변경 반영 및 재실행 후 복원
- 상태바/내비게이션 인셋 때문에 UI가 잘리지 않는지
- 큰 글꼴 배율에서도 핵심 버튼이 접근 가능한지

### 실기기 테스트 체크리스트

- Galaxy Z Fold를 완전히 펼친 상태
- 70~120도 정도 반접힘 상태
- 세로/가로 회전
- 충전 중 / 미충전 상태에서 각각 수동 시작
- 접었다가 다시 펼쳤을 때 상태 복원
- 화면 꺼짐 설정 켬/끔
- 15분 이상 실행해 발열과 배터리 소모 확인
- 듀얼 화면 capability 미지원 시 비활성 안내 확인
- capability 지원 기기에서는 승인, 시작, 종료, 실패 fallback 확인

에뮬레이터만으로 완료 판정하지 않는다. 다만 Codex가 실기기에 접근할 수 없으면 빌드와 자동 테스트를 끝내고, 정확한 실기기 체크리스트를 README에 남긴다.

## 14. 완료 기준(Definition of Done)

- `./gradlew assembleDebug` 성공
- 가능한 경우 `./gradlew test`와 lint 성공
- 설치 가능한 debug APK 생성 위치를 README에 기록
- 앱 첫 화면에서 1회 탭으로 StandBy 시작 가능
- **충전하지 않은 상태에서도 시작 가능**
- Fold 반접힘을 감지해 힌지와 겹치지 않는 두 영역 표시
- 일반 스마트폰/지원하지 않는 Fold에서 안전한 대체 레이아웃 표시
- 설정이 앱 재시작 뒤에도 유지
- 화면 유지·밝기 변경이 StandBy 화면에만 적용
- 듀얼 화면 미지원 상태에서 크래시 없음
- 주요 UI에 다크 모드, 접근성 라벨, 번인 방지 적용
- README에 설치법, 조작법, 실제 제약, 테스트법 수록

## 15. Codex 작업 지시

다음 규칙을 지킨다.

1. 나에게 파일을 하나씩 복사하라고 시키지 말고 프로젝트 안의 파일을 직접 만든다.
2. 모호한 사소한 부분은 합리적인 기본값을 선택하고 계속 진행한다.
3. 기존 파일이 있으면 먼저 전체 구조를 확인하고 사용자 변경을 덮어쓰지 않는다.
4. 최신 안정 Android/Kotlin/Compose 조합을 선택하고 버전 호환성을 확인한다.
5. 가짜 동작이나 눌러도 반응 없는 버튼을 남기지 않는다. 미완성 기능은 숨기거나 명확히 `준비 중`으로 표시한다.
6. Galaxy 전용 비공개 API나 reflection에 의존하지 않는다.
7. 특정 모델에서만 되는 기능을 모든 Fold에서 된다고 표시하지 않는다.
8. 빌드와 테스트를 직접 실행하고 오류가 있으면 수정한다.
9. 완료 후 초보자가 이해할 수 있게 아래만 간단히 보고한다.
   - 무엇을 만들었는지
   - APK 위치
   - 휴대폰 설치 방법
   - 아직 기기에서 확인해야 할 항목

## 16. 후속 버전 후보(MVP 완료 후)

- 사진 앨범 슬라이드(권한은 사용자 선택 시에만)
- 달력, 타이머, 알람
- 음악 재생 정보(사용자 승인과 Notification Listener 검토 필요)
- 날씨(네트워크/API 필요, MVP에서는 제외)
- 자동 색상 팔레트
- 침대 옆 야간 적색 모드
- 여러 StandBy 프리셋 저장
- 홈 화면 위젯/빠른 설정 타일
- Samsung Routine과 연동하기 위한 명시적 Activity shortcut

후속 기능은 MVP 안정화 전에는 구현하지 않는다.

## 17. 공식 참고 문서

- Android foldable 개념과 자세: https://developer.android.com/develop/ui/compose/layouts/adaptive/foldables/learn-about-foldables
- Compose에서 fold-aware 구현: https://developer.android.com/develop/ui/compose/layouts/adaptive/foldables/make-your-app-fold-aware
- Foldable display modes와 capability 검사: https://developer.android.com/develop/ui/compose/layouts/adaptive/foldables/support-foldable-display-modes
- 화면 켜짐 유지: https://developer.android.com/develop/background-work/background-tasks/awake/screen-on
- Samsung Flex mode: https://developer.samsung.com/galaxy-z/flex-mode.html

---

## 바로 사용할 Codex 시작 명령

아래 문장을 이 파일과 함께 Codex에 전달한다.

> 이 `FOLDSTAND_CODEX_PROJECT_SPEC.md`를 프로젝트의 최상위 요구사항으로 사용해. 전체 저장소를 먼저 확인한 뒤 앱을 실제 실행 가능한 상태로 구현하고, `assembleDebug`와 테스트를 수행해. 충전 여부와 관계없는 수동 시작, 안전한 폴더블 자세 감지, 지원되지 않는 기기에서의 fallback을 MVP 최우선으로 해. 작업 중 사소한 선택은 묻지 말고 합리적으로 결정해서 끝까지 진행해. 완료 후 초보자가 따라 할 수 있는 설치 방법과 APK 위치를 알려줘.
