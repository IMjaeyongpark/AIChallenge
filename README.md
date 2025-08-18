# AI Challenge – Back-End (Spring Boot + JSP)

구직자의 **이력서/기술스택/목표 직무**를 바탕으로

* **맞춤형 면접 질문**을 생성하고
* **자기 개발·합격률 향상 학습 경로**를 추천하며
* 같은 정보로 **학습 경로 챗봇**과 질의응답

을 제공하는 백엔드 서비스입니다. (프론트는 JSP 한 장 + 정적 `css/js`)

---

## ✨ 주요 기능

* **면접 질문 생성**: `/api/v1/ai/questions`
* **학습 경로 전체 추천**: `/api/v1/ai/learning-path`
* **학습 경로 챗봇(Q\&A)**: `/api/v1/ai/learning-path/chat`
* **JSP UI**: 이력서/직무/스택 입력 → 결과 표시, 챗봇 대화

LLM은 Google **Gemini** API(1.5-flash) 사용.

---

## 🏗 아키텍처 개요

```
[JSP(뷰) + ai.js]  ──(fetch/JSON)──>  [Spring MVC Controller]
                                       └─> Service ──> Client(WebClient) ──> Gemini API
                                     ↘  PromptLoader(템플릿 로딩)
[ai.css]
```

---

## 📂 프로젝트 구조(요약)

```
src
├─ main
│  ├─ java/AIChallenge/AIChallenge
│  │  ├─ controller/   AiRestController.java, ViewController.java
│  │  ├─ service/      QuestionsService.java, LearningService.java
│  │  ├─ client/       QuestionsClient.java, LearningClient.java, ConversationMemory.java
│  │  ├─ util/         PromptLoader.java
│  │  ├─ config/       PropertyConfig.java
│  │  └─ DTO/          AiRequest.java, AiResponse.java
│  └─ resources
│     ├─ application.properties
│     ├─ prompts/
│     │  ├─ interview_questions_ko.txt
│     │  └─ learning-path_ko.txt
│     ├─ static/
│     │  ├─ css/ai.css
│     │  └─ js/ai.js
│     └─ WEB-INF/jsp/index.jsp
└─ test/...
```

---

## ⚙️ 요구 사항

* Java 17+
* Gradle 8+
* (선택) IntelliJ IDEA
* Google AI Studio 의 **Gemini API Key**

---

## 🚀 실행 방법

```bash
# 1) 환경변수로 API 키 주입 권장
export GEMINI_API_KEY=your_api_key_here

# 2) 애플리케이션 설정
# src/main/resources/application.properties
# 서버/JSP
spring.mvc.view.prefix=/WEB-INF/jsp/
spring.mvc.view.suffix=.jsp

# LLM
gemini.api.key=${GEMINI_API_KEY}

# 프롬프트 템플릿 경로
prompts.questions-template=prompts/interview_questions_ko.txt
prompts.learning-template=prompts/learning_path_ko.txt

# 3) 실행
./gradlew bootRun
```

실행 후: `http://localhost:8080/` 접속 (JSP UI)

---

## 📝 API 명세

### 공통 DTO

**AiRequest** (요청)

```json
{
  "resume": "경력/프로젝트 요약",
  "targetRole": "Backend",
  "techStack": ["Spring Boot", "AWS", "PostgreSQL"],
  "count": 5,
  "question": "학습 우선순위는?"
}
```

**AiResponse** (응답)

```json
{
  "questions": [
    "질문 또는 추천/답변 라인들..."
  ]
}
```

---

### 1) 면접 질문 생성

`POST /api/v1/ai/questions`

* 요청: `resume`, `targetRole`, `techStack`, `count(>0)`
* 응답: `questions: string[]`

**cURL**

```bash
curl -X POST http://localhost:8080/api/v1/ai/questions \
  -H "Content-Type: application/json" \
  -d '{
    "resume":"백엔드 3년, Spring, JPA, AWS 경험",
    "targetRole":"Backend",
    "techStack":["Spring Boot","JPA","AWS"],
    "count":5
  }'
```

---

### 2) 학습 경로 전체 추천

`POST /api/v1/ai/learning-path`

* 요청: `resume` (필수), `targetRole`, `techStack`
* 응답: `questions: string[]` (요약/주차별 라인)

**cURL**

```bash
curl -X POST http://localhost:8080/api/v1/ai/learning-path \
  -H "Content-Type: application/json" \
  -d '{
    "resume":"백엔드 3년, Spring, AWS",
    "targetRole":"Backend",
    "techStack":["Spring","AWS"]
  }'
```

---

### 3) 학습 경로 챗봇

`POST /api/v1/ai/learning-path/chat`

* 요청: `resume`(필수), `question`(필수) + 선택으로 `targetRole`, `techStack`
* 응답: `questions: string[]` (답변 라인)

**cURL**

```bash
curl -X POST http://localhost:8080/api/v1/ai/learning-path/chat \
  -H "Content-Type: application/json" \
  -d '{
    "resume":"백엔드 3년, Spring, AWS",
    "question":"Kafka와 Redis 중 뭐부터?"
  }'
```

---

## 🧩 템플릿(프롬프트) 관리

* `src/main/resources/prompts/interview_questions_ko.txt`
* `src/main/resources/prompts/learning-path_ko.txt`

변경 시 재시작만으로 반영(Loader 내부 캐시 사용).

---

## 🔐 보안 & 비용

* **API Key는 코드에 하드코딩하지 마세요.**
  OS 환경변수 → `application.properties` 에서 `${GEMINI_API_KEY}` 참조
* 서버 사이드에서만 LLM 호출 (프론트에서 키 노출 금지)
* 호출 횟수 제한/타임아웃 처리(WebClient + timeout). 필요 시 레이트리미터(Guava/Resilience4j) 적용 추천.

---

## 🧪 개발 팁

* **JSP 404/태그 오류**

    * `tomcat-embed-jasper`, `jakarta.servlet.jsp.jstl` 의존성 확인
    * `spring.mvc.view.prefix/suffix` 확인
* **템플릿 파일 에러**

    * `src/main/resources/prompts/*.txt` 경로/파일명 정확히 생성
* **MacOS Netty 경고**

    * 단순 경고. 무시 가능. 네이티브 의존성 추가로 제거 가능.

---

## 📸 화면

* `/WEB-INF/jsp/index.jsp` : 입력 폼, 질문 결과, **학습 경로 챗봇** UI
  정적 리소스: `/static/css/ai.css`, `/static/js/ai.js`

---

## 📦 빌드

```bash
./gradlew clean build
java -jar build/libs/AIChallenge-*.jar
```

---

## 📜 라이선스

과제 제출용 샘플. 필요 시 사내/개인 프로젝트 라이선스에 맞춰 조정.

---

## 🙋 지원/문의

* 이 레포 이슈 트래커 또는 커밋 메시지/PR로 기록
* 프롬프트/모델/토큰 정책 변경 시 README 업데이트 권장
