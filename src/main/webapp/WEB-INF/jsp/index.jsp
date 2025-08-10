<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8" />
    <title>AI 질문 생성</title>

    <!-- (선택) Spring Security CSRF 사용 시 메타로 전달 -->
    <c:if test="${_csrf != null}">
        <meta name="_csrf" content="${_csrf.token}" />
        <meta name="_csrf_header" content="${_csrf.headerName}" />
    </c:if>

    <style>
        :root { --pri:#2d6cdf; --bg:#f7f8fa; --fg:#1f2937; --muted:#6b7280; }
        *{box-sizing:border-box} body{margin:0;padding:24px;font-family:system-ui,Segoe UI,Apple SD Gothic Neo,sans-serif;background:var(--bg);color:var(--fg)}
        h1{margin:0 0 16px}
        .card{background:#fff;border:1px solid #e5e7eb;border-radius:14px;box-shadow:0 6px 16px rgba(0,0,0,.04);padding:18px;max-width:880px}
        .row{display:grid;grid-template-columns:1fr 1fr;gap:14px}
        .row.full{grid-template-columns:1fr}
        label{display:block;font-size:13px;color:var(--muted);margin-bottom:6px}
        textarea,input{width:100%;padding:10px 12px;border:1px solid #d1d5db;border-radius:10px;background:#fff;font-size:14px}
        textarea{min-height:120px;resize:vertical}
        .hint{font-size:12px;color:var(--muted);margin-top:6px}
        .actions{display:flex;gap:10px;margin-top:14px}
        button{appearance:none;border:0;border-radius:10px;padding:10px 16px;background:var(--pri);color:#fff;font-weight:600;cursor:pointer}
        button[disabled]{opacity:.6;cursor:not-allowed}
        .list{margin:14px 0 0;padding-left:18px}
        .error{margin-top:10px;color:#b91c1c;font-size:13px}
        .loading{margin-left:6px;font-size:12px;color:var(--muted)}
    </style>
</head>
<body>
<h1>AI 질문 생성</h1>

<div class="card">
    <div class="row full">
        <div>
            <label for="resume">이력서 내용</label>
            <textarea id="resume" placeholder="경력/프로젝트/역할 등을 붙여넣으세요"></textarea>
            <div class="hint">가능하면 상세히 작성할수록 질문이 좋아집니다.</div>
        </div>
    </div>

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
    </div>

    <div class="actions">
        <button id="btn">질문 생성</button>
        <span id="loading" class="loading" style="display:none">생성 중…</span>
    </div>

    <div id="error" class="error" style="display:none"></div>

    <h3 style="margin-top:18px">생성된 질문</h3>
    <ol id="result" class="list"></ol>
</div>

<script>
    const $ = (id) => document.getElementById(id);

    function getCsrfHeader() {
        const token = document.querySelector('meta[name="_csrf"]')?.content;
        const headerName = document.querySelector('meta[name="_csrf_header"]')?.content;
        return token && headerName ? { [headerName]: token } : {};
    }

    function validateForm(resume, targetRole, count) {
        if (!resume.trim()) return '이력서 내용을 입력해 주세요.';
        if (!targetRole.trim()) return '목표 직무를 입력해 주세요.';
        if (!Number.isFinite(count) || count <= 0) return '질문 개수는 1 이상이어야 합니다.';
        return null;
    }

    $('btn').addEventListener('click', async () => {
        const resume = $('resume').value;
        const targetRole = $('targetRole').value;
        const techStack = $('techStack').value
            .split(',')
            .map(s => s.trim())
            .filter(Boolean);
        const count = parseInt($('count').value, 10);

        const err = validateForm(resume, targetRole, count);
        if (err) {
            $('error').textContent = err;
            $('error').style.display = 'block';
            return;
        }
        $('error').style.display = 'none';

        const payload = { resume, targetRole, techStack, count };

        const headers = {
            'Content-Type': 'application/json',
            ...getCsrfHeader()
        };

        $('btn').disabled = true;
        $('loading').style.display = 'inline';

        try {
            const res = await fetch('/api/v1/ai/questions', {
                method: 'POST',
                headers,
                body: JSON.stringify(payload)
            });

            if (!res.ok) {
                const text = await res.text();
                throw new Error(`HTTP ${res.status} ${res.statusText} - ${text}`);
            }

            const data = await res.json();
            const resultEl = $('result');
            resultEl.innerHTML = '';

            if (Array.isArray(data.questions) && data.questions.length > 0) {
                data.questions.forEach(q => {
                    const li = document.createElement('li');
                    li.textContent = q;
                    resultEl.appendChild(li);
                });
            } else {
                const li = document.createElement('li');
                li.textContent = '질문을 생성하지 못했습니다.';
                $('result').appendChild(li);
            }
        } catch (e) {
            $('error').textContent = '요청 실패: ' + e.message;
            $('error').style.display = 'block';
        } finally {
            $('btn').disabled = false;
            $('loading').style.display = 'none';
        }
    });
</script>
</body>
</html>
