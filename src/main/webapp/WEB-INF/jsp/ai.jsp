<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8" />
    <title>AI 질문 + 학습 경로 챗봇</title>

    <!-- CSRF 처리 -->
    <c:if test="${_csrf != null}">
        <meta name="_csrf" content="${_csrf.token}" />
        <meta name="_csrf_header" content="${_csrf.headerName}" />
    </c:if>

    <!-- 외부 CSS & JS -->
    <link rel="stylesheet" href="/css/ai.css" />
    <script defer src="/js/ai.js"></script>
</head>
<body>
<h1>AI 기반 면접 질문 & 학습 경로 챗봇</h1>

<div class="card">
    <!-- 이력서 입력 -->
    <div class="row full">
        <label for="resume">이력서 내용</label>
        <textarea id="resume" placeholder="경력/프로젝트/역할 등을 붙여넣으세요"></textarea>
    </div>

    <!-- 직무, 기술스택, 질문 개수 -->
    <div class="row" style="margin-top:12px">
        <div>
            <label for="targetRole">목표 직무</label>
            <input id="targetRole" placeholder="예: Backend, MLOps, DevOps 등" />
        </div>
        <div>
            <label for="techStack">기술 스택 (콤마로 구분)</label>
            <input id="techStack" placeholder="Spring Boot, AWS, PostgreSQL" />
        </div>
    </div>

    <div class="row" style="margin-top:12px">
        <div>
            <label for="count">질문 개수</label>
            <input id="count" type="number" min="1" max="20" value="5" />
        </div>
        <div>
            <label for="weeks">학습 기간(주)</label>
            <input id="weeks" type="number" min="1" max="52" placeholder="예: 12" />
        </div>
        <div>
            <label for="hoursPerWeek">주당 학습 시간(시간)</label>
            <input id="hoursPerWeek" type="number" min="1" max="100" placeholder="예: 10" />
        </div>
    </div>

    <!-- 버튼 -->
    <div class="actions">
        <button id="btnGenerate">질문 생성</button>
        <button id="btnLearning">학습 경로 추천</button>
        <span id="loading" class="loading" style="display:none">처리 중…</span>
    </div>

    <div id="error" class="error" style="display:none"></div>

    <!-- 질문 결과 -->
    <h3>🧠 AI가 생성한 면접 질문</h3>
    <ol id="resultQuestions" class="list"></ol>

    <!-- 학습 경로 챗봇 -->
    <h3>📘 학습 경로 추천 챗봇</h3>
    <div id="chatBox" class="chat-box"></div>
    <div class="chat-input">
        <input id="chatQuestion" placeholder="AI에게 학습 경로 관련 질문하기..." />
        <button id="btnSendChat">전송</button>
    </div>
</div>
</body>
</html>
