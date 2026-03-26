import { useState, useEffect, useRef } from "react";
import axios from "axios";
import "./App.css";

// ── Helpers ───────────────────────────────────────────────────────────────────

function parseAnalysis(text) {
  const scoreMatch = text.match(/Score:\s*(\d+)/i);
  const score = scoreMatch ? parseInt(scoreMatch[1]) : 0;

  const tips = [];
  text.split("\n").forEach((line) => {
    const rich = line.match(/^\d+\.\s+\*{0,2}(.+?)\*{0,2}:\s*(.+)/);
    if (rich) {
      tips.push(rich[1].trim() + ": " + rich[2].trim());
      return;
    }
    const plain = line.match(/^\d+\.\s+(.+)/);
    if (plain) tips.push(plain[1].replace(/\*\*/g, "").trim());
  });

  return { score, tips };
}

function gradeInfo(score) {
  if (score >= 80) return { label: "Excellent", cls: "excellent", emoji: "🏆" };
  if (score >= 60) return { label: "Good", cls: "good", emoji: "👍" };
  if (score >= 40) return { label: "Fair", cls: "fair", emoji: "📈" };
  return { label: "Needs Work", cls: "poor", emoji: "🔧" };
}

// ── Score Ring ────────────────────────────────────────────────────────────────

function ScoreRing({ score }) {
  const r = 54;
  const circ = 2 * Math.PI * r;
  const [offset, setOffset] = useState(circ);
  const [displayScore, setDisplayScore] = useState(0);
  const { label, cls, emoji } = gradeInfo(score);

  useEffect(() => {
    const t1 = setTimeout(() => setOffset(circ - (score / 100) * circ), 200);
    let cur = 0;
    const step = score / 60;
    const t2 = setInterval(() => {
      cur = Math.min(cur + step, score);
      setDisplayScore(Math.round(cur));
      if (cur >= score) clearInterval(t2);
    }, 16);
    return () => {
      clearTimeout(t1);
      clearInterval(t2);
    };
  }, [score, circ]);

  return (
    <div className={`score-hero score-${cls}`}>
      <div className="ring-wrap">
        <svg width="128" height="128" viewBox="0 0 128 128">
          <circle className="ring-track" cx="64" cy="64" r={r} />
          <circle
            className="ring-fill"
            cx="64"
            cy="64"
            r={r}
            strokeDasharray={circ}
            strokeDashoffset={offset}
          />
        </svg>
        <div className="ring-center">
          <span className="ring-num">{displayScore}</span>
          <span className="ring-max">/100</span>
        </div>
      </div>

      <div className="score-info">
        <span className={`grade-badge grade-${cls}`}>
          {emoji} {label}
        </span>
        <h2 className="score-title">ATS Match Score</h2>
        <p className="score-desc">
          {score >= 80
            ? "Your resume is well-optimized for applicant tracking systems."
            : score >= 60
              ? "Good base — a few tweaks will boost your pass rate."
              : "Significant improvements needed to clear ATS filters."}
        </p>
        <div className="bar-track">
          <div className="bar-fill" style={{ width: `${score}%` }} />
        </div>
      </div>
    </div>
  );
}

// ── Tip Card ──────────────────────────────────────────────────────────────────

function TipCard({ tip, index }) {
  return (
    <div className="tip-card" style={{ animationDelay: `${index * 0.07}s` }}>
      <span className="tip-num">{String(index + 1).padStart(2, "0")}</span>
      <p className="tip-text">{tip}</p>
    </div>
  );
}

// ── History Row ───────────────────────────────────────────────────────────────

function HistoryRow({ entry, onClick }) {
  const { cls } = gradeInfo(entry.score);
  return (
    <button className="history-row" onClick={onClick}>
      <span className={`history-score score-num-${cls}`}>{entry.score}</span>
      <span className="history-name">{entry.filename}</span>
      <span className="history-time">{entry.time}</span>
      <span className="history-chevron">›</span>
    </button>
  );
}

// ── App ───────────────────────────────────────────────────────────────────────

export default function App() {
  const [file, setFile] = useState(null);
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const [drag, setDrag] = useState(false);
  const [tab, setTab] = useState("tips");
  const fileInputRef = useRef();

  const [history, setHistory] = useState(() => {
    try {
      return JSON.parse(localStorage.getItem("ra_v2") || "[]");
    } catch {
      return [];
    }
  });

  const pushHistory = (entry) => {
    const next = [entry, ...history].slice(0, 8);
    setHistory(next);
    localStorage.setItem("ra_v2", JSON.stringify(next));
  };

  const handleFile = (f) => {
    if (!f) return;
    if (f.type !== "application/pdf") {
      alert("Please upload a PDF.");
      return;
    }
    setFile(f);
    setResult(null);
  };

  const handleAnalyze = async () => {
    if (!file) {
      alert("Select a PDF first.");
      return;
    }
    setLoading(true);
    setResult(null);

    const fd = new FormData();
    fd.append("file", file);

    try {
      const res = await axios.post(
        "http://localhost:8080/api/resume/analyze",
        fd,
      );
      const { score, tips } = parseAnalysis(res.data.analysis);
      const entry = {
        score,
        tips,
        raw: res.data.analysis,
        filename: res.data.filename,
        time: new Date().toLocaleTimeString([], {
          hour: "2-digit",
          minute: "2-digit",
        }),
      };
      setResult(entry);
      pushHistory(entry);
      setTab("tips");
    } catch (e) {
      console.error(e);
      alert("Backend error — make sure Spring Boot is running on :8080.");
    }
    setLoading(false);
  };

  const bestScore = history.length
    ? Math.max(...history.map((h) => h.score))
    : null;
  const avgScore = history.length
    ? Math.round(history.reduce((a, h) => a + h.score, 0) / history.length)
    : null;

  return (
    <div className="app">
      <div className="grain" aria-hidden="true" />

      {/* HEADER */}
      <header className="header">
        <div className="brand">
          <div className="brand-dot" />
          <span className="brand-name">ResumeAI</span>
        </div>

        {history.length > 0 && (
          <div className="header-stats">
            <div className="hstat">
              <b>{history.length}</b>
              <span>Scanned</span>
            </div>
            <div className="hstat-sep" />
            <div className="hstat">
              <b>{avgScore}</b>
              <span>Avg</span>
            </div>
            <div className="hstat-sep" />
            <div className="hstat">
              <b>{bestScore}</b>
              <span>Best</span>
            </div>
          </div>
        )}

        <div className="header-pill">Groq · LLaMA 3</div>
      </header>

      {/* MAIN */}
      <div className="layout">
        {/* ── LEFT SIDEBAR ── */}
        <aside className="sidebar">
          <div className="sidebar-hero">
            <h1 className="sidebar-title">
              Know your
              <br />
              <em>resume's</em>
              <br />
              ATS score.
            </h1>
            <p className="sidebar-sub">
              Instant PDF analysis. Powered by open-source AI.
            </p>
          </div>

          {/* Drop zone */}
          <div
            className={`dropzone${drag ? " dz-drag" : ""}${file ? " dz-filled" : ""}`}
            onDragOver={(e) => {
              e.preventDefault();
              setDrag(true);
            }}
            onDragLeave={() => setDrag(false)}
            onDrop={(e) => {
              e.preventDefault();
              setDrag(false);
              handleFile(e.dataTransfer.files[0]);
            }}
            onClick={() => !file && fileInputRef.current?.click()}
          >
            <input
              ref={fileInputRef}
              type="file"
              accept=".pdf"
              style={{ display: "none" }}
              onChange={(e) => handleFile(e.target.files[0])}
            />
            {file ? (
              <div className="dz-file">
                <span className="dz-file-icon">📄</span>
                <div className="dz-file-meta">
                  <span className="dz-file-name">{file.name}</span>
                  <span className="dz-file-size">
                    {(file.size / 1024).toFixed(0)} KB
                  </span>
                </div>
                <button
                  className="dz-remove"
                  onClick={(e) => {
                    e.stopPropagation();
                    setFile(null);
                    setResult(null);
                  }}
                >
                  ✕
                </button>
              </div>
            ) : (
              <div className="dz-idle">
                <div className="dz-arrow">↑</div>
                <p className="dz-label">
                  Drop PDF or <u>browse</u>
                </p>
                <p className="dz-hint">PDF · max 10 MB</p>
              </div>
            )}
          </div>

          {/* Analyze button */}
          <button
            className={`analyze-btn${loading ? " btn-loading" : ""}`}
            onClick={handleAnalyze}
            disabled={loading || !file}
          >
            {loading ? (
              <>
                <span className="btn-spinner" />
                Analyzing…
              </>
            ) : (
              <>
                Analyze Resume <span className="btn-arrow">↗</span>
              </>
            )}
          </button>

          {/* Recent scans */}
          <div className="history-section">
            <p className="history-label">Recent Scans</p>
            {history.length === 0 ? (
              <p className="history-empty">No scans yet.</p>
            ) : (
              <div className="history-list">
                {history.map((h, i) => (
                  <HistoryRow
                    key={i}
                    entry={h}
                    onClick={() => {
                      setResult(h);
                      setTab("tips");
                    }}
                  />
                ))}
              </div>
            )}
          </div>
        </aside>

        {/* ── RIGHT PANEL ── */}
        <section className="panel">
          {!result ? (
            <div className="empty-state">
              <div className="empty-orb" />
              <div className="empty-text">
                <span className="empty-emoji">🎯</span>
                <h3>Ready to analyze</h3>
                <p>
                  Upload a resume on the left and click <em>Analyze Resume</em>.
                </p>
              </div>
            </div>
          ) : (
            <div className="result-wrap">
              <ScoreRing score={result.score} />

              {/* Tabs */}
              <div className="tabs">
                <button
                  className={`tab${tab === "tips" ? " tab-on" : ""}`}
                  onClick={() => setTab("tips")}
                >
                  💡 Tips ({result.tips.length})
                </button>
                <button
                  className={`tab${tab === "raw" ? " tab-on" : ""}`}
                  onClick={() => setTab("raw")}
                >
                  🖥 Raw Output
                </button>
              </div>

              {tab === "tips" && (
                <div className="tips-grid">
                  {result.tips.length > 0 ? (
                    result.tips.map((tip, i) => (
                      <TipCard key={i} tip={tip} index={i} />
                    ))
                  ) : (
                    <p className="tips-empty">
                      No structured tips — check Raw Output.
                    </p>
                  )}
                </div>
              )}

              {tab === "raw" && <pre className="raw-box">{result.raw}</pre>}
            </div>
          )}
        </section>
      </div>
    </div>
  );
}
