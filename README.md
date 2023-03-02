[ 삼성 청년 SW 아카데미 (SSAFY) 8기 - 2학기 공통프로젝트 ]

# LEARNWAY

## I. 서비스 소개

### 개요

- **전 세계 사람들**과 서로 **언어**를 **교환**하고 배울 수 있는 플랫폼 
  Fly over the **RUNWAY** with our **LEARNWAY**

### 타겟

- **세계 여러 나라**의 **언어**와 **문화**를 **배우거나 공유**하고 싶은 사람  
- **스피킹 시험 성적 향상**을 목표로 다양한 **외국인과 소통**하면서 **회화 실력을 키우고자** 하는 사람  

## II. 기획 배경

### 배경

![image](https://user-images.githubusercontent.com/57744586/219523863-b4d600ad-0672-4da2-b7fe-ea430d890b4b.png)

- **점점 증가하는 회화 능력의 중요성**에 비해 **한국 대학생들의 영어회화 능력은 평균치 이하**  
- 세계 여러 나라 사람들과 **언어적, 문화적 교류를 원하는 사용자의 니즈 충족** 필요  

### 목적

- 중요해지는 영어 능력에 따른 회화공부 수요 증가, 글로벌화 시대에 따라 전세계 사람들과의 원할한 교류의 필요성에 대한트렌드에 맞춰, 이러한 니즈를 충족하고자 **LEARNWAY** 서비스를 기획  

### 의의

- 넓은 언어의 선택폭(12개 언어 지원)  
- 대화 중 유튜브 콘텐츠, 번역 등의 부가 기능 제공으로 대화 소재 제공 및 언어 구사 능력 격차 해소  

## III. 주요 기능

- 회원 가입 및 일반 로그인/ 구글 소셜 로그인  

- 대화 하고 싶은 언어 선택 가능  

- **유저 취향, 선택 언어 등을 고려**하여 최적의 대화 가능한 유저들 중 **랜덤 매칭**  

- 화상 통화 중 **여러 부가서비스 제공**  
  
  - YouTube 동시 시청  
  - 실시간 번역 기능  
  - 대화 중 친구의 프로필 조회 및 친구 추가 기능  
  - 악성 유저 신고 기능  
  - 화상 통화를 안전하게 끝낼 수 있는 기능  

- 친구로 등록된 사용자와의 **채팅** 기능  

- 마이페이지에서 **친구와의 대화 스크립트 제공**으로 언어 복습 및 다시 들어보기 기능  

## IV. 기술

- **Back End**
  
  - nginx
  - docker
  - Openvidu
  - Spring Boot
  - Spring Security
  - Sprinng Data
  - JPA
  - Flask
  - MySQL
  - Rades
  - Rabbit MQ
  - Stomp
  - Sweager

- **Front End**  
  
  - React  
  - Redux  
  - react-Stomp  
  - Papago API  
  - Youtube API  
  - Google TTS API  
  - OpenVidu  

## VI. 협업 툴 및 협업 환경

- **Notion**
  - 기획 및 회의록 작성  
  - API문서, 기능 명세서 등 팀원 모두가 공유하는 산출물 기록  
- **JIRA**  
  - 매주 목표량 설정하여 sprint 진행  
  - 업무 할당량 정하여 Story Point 설정, In Progress > Done 순으로 작성  
- **GitLab**  
  - 코드 버전 관리  
  - 이슈 발행 및 관리  

## VII. 역할 분담

- **BackEnd**
  
  - 박미희
    - Spring Secutiry 초기 설정
    - 일반 로그인 및 소셜로그인(jwt, O-Auth 등)
    - User 컨트롤러 구현
    - 매칭 서버 (rabbitMQ, 매칭 알고리즘 수정)
  - 이지수
    - Spring JPA : Entity 설계
    - API 구현 (회화, 학습, 신고, 채팅, 메일인증)
    - WebSocket & Stomp & Redis
    - 매칭 서버 구현
  - 조현민
    - CI/CD
    - 화상 통화에서 음성 스크립트 생성 로직 구현
    - 서버 로그 관리
    - Friend 컨트롤러 구현

- **FrontEnd**
  
  - **이예진** 
    
    - 마이페이지(프로필 수정, 친구관리, 스크립트)
    - 친구관리 기능(추가, 관리, 채팅리스트 등록)
    - 스크립트 관리 컴포넌트(TTS 재생, 날짜별 정렬 기능)
    - 번역 기능
  
  - **이한빈**
    
    - 최종 발표
    - Intro 페이지
    - 로그인(Google 소셜 로그인, 일반 로그인)페이지
    - 보안 처리(Access Token, Refresh Token, 라우터 접근)
    - 회원가입 페이지
  
  - **차영후**
    
    - 팀장
    - 메인페이지
    - Web Socket (채팅, Youtube 동시시청, 로딩페이지)
    - WebRTC (OpenVidu)
    - 화상통화 부가서비스(신고, 나가기, 상대 프로필 보기 및 친구추가)

## VIII. 프로젝트 결과물

- [발표 자료](https://cs-study-0518.notion.site/38133d2eceb9422c87f023cbe03dd80e)

- [기능 명세서](https://cs-study-0518.notion.site/29f26892f3b64059a75d9caba544d222)

- [목업-Figma](https://www.figma.com/file/TgiYj3wQQ1m4K1quMS5pE4/SSAFY-A408%ED%8C%80-%EA%B3%B5%ED%86%B5%ED%94%84%EB%A1%9C%EC%A0%9D%ED%8A%B8(%EC%9B%B9%EA%B8%B0%EC%88%A0)-UI-%EC%84%A4%EA%B3%84%EC%84%9C?node-id=60%3A250&t=hjYO3ipFXPY2ZwcN-0)

- Flow Chart![image](https://user-images.githubusercontent.com/57744586/222173269-3efdda78-5f48-48f8-8196-f5c48e539aed.png)

- ERD![image](https://user-images.githubusercontent.com/57744586/222172914-9d9db926-e42f-4400-acd4-00994a96de97.png)

- 시스템 구조 다이어그램![image](https://user-images.githubusercontent.com/57744586/222172612-3e663052-d010-4103-a7d5-c7db9a0b2807.png)


## VIII. 서비스 데모 영상

- **Demo Video [LEARNWAY 시연영상 - YouTube]**  

  [![](http://i.ytimg.com/vi/FVc0pLRAJI8/hqdefault.jpg)](https://youtu.be/FVc0pLRAJI8)
  

- **회원가입**
  ![signup](https://user-images.githubusercontent.com/57744586/219590929-328e7163-d821-4e9e-b470-566f2dad0ea7.gif)
  
  - 이메일 주소 인증 기반 회원가입  
  - 영문, 숫자, 특수문자 포함 8문자 이상 패스워드 규칙 설정  
  - 대화와 매칭에 필요한 취향 최소 3개 이상 설정 가능  
  - 나의 언어, 취향 설정 가능  
