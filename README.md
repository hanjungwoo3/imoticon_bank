# KakaoLink Widget

## 프로젝트 개요
이 앱은 https://2244.tistory.com/rss 피드에서 카카오톡 친구 링크(`http://pf.kakao.com/*/friend`)를 추출하여 안드로이드 위젯으로 표시하는 애플리케이션입니다.

## 주요 기능
- RSS 피드 자동 파싱
- 카카오톡 친구 링크 자동 추출
- 홈 화면 위젯으로 링크 표시
- 링크 클릭 시 카카오톡 친구 추가 페이지 열기
- 자동 새로고침 (기본 30분 간격)
- 수동 새로고침 버튼
- 새로고침 간격 설정 가능

## 기술 스택
- **언어**: Kotlin
- **UI**: Android Views (위젯)
- **네트워크**: OkHttp + Retrofit
- **RSS 파싱**: Rome Tools
- **백그라운드 작업**: WorkManager
- **정규 표현식**: 카카오톡 친구 링크 추출

## 프로젝트 구조
```
app/
├── src/main/
│   ├── java/com/kakaolinkwidget/
│   │   ├── MainActivity.kt              # 메인 액티비티
│   │   ├── KakaoLinkWidgetProvider.kt   # 위젯 프로바이더
│   │   ├── KakaoLinkWidgetService.kt    # 위젯 서비스
│   │   ├── RssParser.kt                 # RSS 파싱 클래스
│   │   ├── KakaoLink.kt                 # 데이터 클래스
│   │   ├── WidgetUpdateWorker.kt        # 백그라운드 업데이트 워커
│   │   └── BootReceiver.kt              # 부팅 시 자동 시작 리시버
│   ├── res/
│   │   ├── layout/
│   │   │   ├── activity_main.xml        # 메인 액티비티 레이아웃
│   │   │   ├── widget_layout.xml        # 위젯 레이아웃
│   │   │   └── widget_link_item.xml     # 위젯 링크 아이템 레이아웃
│   │   ├── drawable/                    # 드로어블 리소스
│   │   ├── values/                      # 문자열, 색상, 테마 리소스
│   │   └── xml/
│   │       └── kakaolink_widget_info.xml # 위젯 정보
│   └── AndroidManifest.xml
└── build.gradle
```

## 설치 및 빌드
1. Android Studio에서 프로젝트 열기
2. 의존성 동기화 대기
3. 빌드 및 실행

## 사용 방법
1. **앱 실행**: 앱을 실행하여 설정 확인
2. **위젯 추가**: 
   - 앱 내 "위젯 추가하기" 버튼 클릭
   - 또는 홈 화면에서 수동으로 위젯 추가
3. **설정 변경**: 새로고침 간격 조정 가능 (최소 15분)
4. **RSS 테스트**: "RSS 테스트" 버튼으로 파싱 결과 확인

## 위젯 기능
- **자동 업데이트**: 설정된 간격으로 RSS 피드 확인
- **수동 새로고침**: 위젯 우상단 새로고침 버튼
- **링크 클릭**: 카카오톡 친구 링크 클릭 시 해당 페이지 열기
- **상태 표시**: 마지막 업데이트 시간 및 상태 표시

## 권한
- `INTERNET`: RSS 피드 접근
- `ACCESS_NETWORK_STATE`: 네트워크 상태 확인
- `WAKE_LOCK`: 백그라운드 작업
- `RECEIVE_BOOT_COMPLETED`: 부팅 시 자동 시작

## 주요 컴포넌트
- **KakaoLinkWidgetProvider**: 위젯 생명주기 관리
- **RssParser**: RSS 피드 파싱 및 링크 추출
- **WidgetUpdateWorker**: 주기적 백그라운드 업데이트
- **KakaoLinkWidgetService**: 위젯 리스트 데이터 제공

## 카카오톡 링크 추출 로직
정규 표현식을 사용하여 RSS 피드의 제목과 설명에서 다음 패턴을 찾습니다:
```regex
http://pf\.kakao\.com/[^/]+/friend
```

## 주의사항
- RSS 피드 URL은 `https://2244.tistory.com/rss`로 고정
- 위젯 업데이트 최소 간격은 15분
- 네트워크 연결이 필요한 기능
- 백그라운드 작업 제한이 있는 기기에서는 자동 업데이트가 제한될 수 있음

## 개발자 정보
- 안드로이드 위젯 개발
- RSS 파싱 및 정규 표현식 활용
- WorkManager를 이용한 백그라운드 작업 관리 