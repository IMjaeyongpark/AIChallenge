<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8" />
    <title>AI 질문 + 챗봇</title>

    <c:if test="${_csrf != null}">
        <meta name="_csrf" content="${_csrf.token}" />
        <meta name="_csrf_header" content="${_csrf.headerName}" />
    </c:if>

    <link rel="stylesheet" href="/css/ai.css" />
    <script defer src="/js/ai.js"></script>
</head>
<body>
<h1>AI 기반 면접 질문 및 챗봇</h1>

<!-- 공통 입력 -->
<div class="card">
    <div class="row full">
        <div>
            <label for="resume">이력서</label>
            <textarea id="resume" placeholder="경력/프로젝트/역할 등을 붙여넣으세요"></textarea>
        </div>
    </div>

    <div class="row">
        <div>
            <label for="targetRole">목표 직무</label>
            <input id="targetRole" placeholder="예: Backend, MLOps 등" />
        </div>
        <div>
            <label for="techStack">기술 스택 (콤마 구분)</label>
            <input id="techStack" placeholder="Spring Boot, AWS 등" />
        </div>
    </div>

    <div class="row">
        <div>
            <label for="count">질문 개수</label>
            <input id="count" type="number" min="1" max="20" value="5" />
        </div>
    </div>

    <div class="actions">
        <button id="btnGenerate">면접 질문 생성</button>
        <span id="loadingGenerate" class="loading" style="display:none">생성 중…</span>
    </div>

    <div id="errorGenerate" class="error" style="display:none"></div>
    <h3>🧠 생성된 면접 질문</h3>
    <ol id="resultQuestions" class="list"></ol>
</div>

<!-- 챗봇 -->
<div class="card" style="margin-top:20px">
    <h3>🤖 챗봇과 대화</h3>
    <div class="row full">
        <div>
            <label for="chatInput">질문 입력</label>
            <textarea id="chatInput" placeholder="예: 어떤 프로젝트를 강조해야 할까요?"></textarea>
        </div>
    </div>

    <div class="actions">
        <button id="btnChat">챗봇에게 질문</button>
        <span id="loadingChat" class="loading" style="display:none">답변 중…</span>
    </div>

    <div id="errorChat" class="error" style="display:none"></div>
    <div id="chatResponse" class="list" style="white-space: pre-line;"></div>
</div>
</body>
</html>
