<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<html>
<head>
    <meta charset="UTF-8">
    <title>AI 질문 생성</title>
    <style>
        body { font-family: Arial, sans-serif; padding: 20px; }
        #result li { margin-bottom: 8px; }
    </style>
</head>
<body>
<h1>AI 질문 생성</h1>

<button id="btn">질문 생성</button>

<h3>생성된 질문</h3>
<ul id="result"></ul> <!-- 결과 표시 영역 -->

<script>
    document.getElementById('btn').addEventListener('click', async () => {
        const res = await fetch('/api/v1/ai/questions', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                resume: "이력서 내용",
                targetRole: "Backend",
                techStack: ["Spring"],
                count: 5
            })
        });

        const data = await res.json();

        const resultEl = document.getElementById('result');
        resultEl.innerHTML = ''; // 기존 결과 지우기

        if (data.questions && data.questions.length > 0) {
            data.questions.forEach(q => {
                const li = document.createElement('li');
                li.textContent = q;
                resultEl.appendChild(li);
            });
        } else {
            const li = document.createElement('li');
            li.textContent = '질문을 생성하지 못했습니다.';
            resultEl.appendChild(li);
        }
    });
</script>
</body>
</html>
