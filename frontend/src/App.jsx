import { useState, useRef, useEffect } from "react";
import axios from "axios";
import "./App.css";

// ── Config ────────────────────────────────────────────────────────────────────
const GROQ_MODEL = "llama-3.1-8b-instant";
const GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";
const GROQ_KEY = import.meta.env.VITE_GROQ_KEY || "";
const API_BASE =
  import.meta.env.VITE_API_URL || "http://localhost:8080/api/resume";

// ── Prompt ────────────────────────────────────────────────────────────────────
const buildPrompt = (resumeText, jobDesc, jobRole) => `
You are an advanced AI-powered ATS Resume Reviewer, Career Coach, and Recruiter.
Analyze the resume and respond ONLY in valid JSON — no markdown, no extra text.

Resume:
${resumeText.slice(0, 3000)}

Job Description:
${jobDesc || "Not provided"}

Target Role:
${jobRole || "Software Engineer"}

Return exactly this JSON structure:
{
  "ats_score": 0,
  "score_explanation": "",
  "parsed_data": {
    "name": "",
    "skills": [],
    "experience": [],
    "education": [],
    "projects": []
  },
  "keyword_analysis": {
    "matched_keywords": [],
    "missing_keywords": [],
    "suggested_keywords": []
  },
  "job_match": {
    "match_percentage": 0,
    "analysis": ""
  },
  "skill_gap": {
    "missing_skills": [],
    "recommended_learning": [],
    "priority_order": []
  },
  "section_feedback": {
    "summary": "",
    "skills": "",
    "experience": "",
    "projects": ""
  },
  "bullet_improvements": [],
  "ats_issues": [],
  "generated_summary": "",
  "cover_letter": "",
  "interview_questions": {
    "technical": [],
    "hr": [],
    "project_based": []
  },
  "career_insights": {
    "career_paths": [],
    "next_roles": []
  },
  "recruiter_decision": {
    "decision": "",
    "reasons": [],
    "strengths": [],
    "red_flags": []
  }
}
`;

// ── Helpers ───────────────────────────────────────────────────────────────────
function gradeInfo(score) {
  if (score >= 80) return { label: "Excellent", cls: "excellent" };
  if (score >= 60) return { label: "Good", cls: "good" };
  if (score >= 40) return { label: "Fair", cls: "fair" };
  return { label: "Needs Work", cls: "poor" };
}

function copyText(text) {
  navigator.clipboard.writeText(text).catch(() => {});
}

// ── Components ────────────────────────────────────────────────────────────────

function ScoreRing({ score }) {
  const r = 44;
  const circ = 2 * Math.PI * r;
  const off = circ - (score / 100) * circ;
  const { label, cls } = gradeInfo(score);
  return (
    <div className="score-ring-wrap">
      <svg width="104" height="104" viewBox="0 0 104 104">
        <circle className="sr-track" cx="52" cy="52" r={r} />
        <circle
          className={`sr-fill sr-${cls}`}
          cx="52"
          cy="52"
          r={r}
          strokeDasharray={circ}
          strokeDashoffset={off}
          style={{
            transition: "stroke-dashoffset 1.2s cubic-bezier(.4,0,.2,1)",
          }}
        />
      </svg>
      <div className="sr-inner">
        <span className="sr-num">{score}</span>
        <span className="sr-sub">/100</span>
      </div>
      <div className={`sr-label grade-${cls}`}>{label}</div>
    </div>
  );
}

function Tag({ text, variant = "default" }) {
  return <span className={`tag tag-${variant}`}>{text}</span>;
}

function Section({ title, icon, children, defaultOpen = true }) {
  const [open, setOpen] = useState(defaultOpen);
  return (
    <div className="result-section">
      <button className="rs-header" onClick={() => setOpen((o) => !o)}>
        <span className="rs-icon">{icon}</span>
        <span className="rs-title">{title}</span>
        <span className={`rs-chevron ${open ? "rs-open" : ""}`}>›</span>
      </button>
      {open && <div className="rs-body">{children}</div>}
    </div>
  );
}

function CopyBox({ text, label }) {
  const [copied, setCopied] = useState(false);
  return (
    <div className="copy-box">
      <div className="copy-box-header">
        <span className="copy-box-label">{label}</span>
        <button
          className="copy-btn"
          onClick={() => {
            copyText(text);
            setCopied(true);
            setTimeout(() => setCopied(false), 1800);
          }}
        >
          {copied ? "✓ Copied" : "Copy"}
        </button>
      </div>
      <pre className="copy-box-text">{text}</pre>
    </div>
  );
}

function ProgressBar({ value, cls = "" }) {
  return (
    <div className="progress-track">
      <div
        className={`progress-fill ${cls}`}
        style={{ width: `${value}%`, transition: "width 1s ease" }}
      />
    </div>
  );
}

// ── Upload Panel ──────────────────────────────────────────────────────────────
function UploadPanel({ onResult, loading, setLoading }) {
  const [file, setFile] = useState(null);
  const [jobDesc, setJobDesc] = useState("");
  const [jobRole, setJobRole] = useState("");
  const [drag, setDrag] = useState(false);
  const [step, setStep] = useState("");
  const fileRef = useRef();

  const handleFile = (f) => {
    if (!f) return;
    if (f.type !== "application/pdf") {
      alert("PDF files only.");
      return;
    }
    setFile(f);
  };

  const handleAnalyze = async () => {
    if (!file) {
      alert("Upload a PDF first.");
      return;
    }
    setLoading(true);
    onResult(null);

    try {
      // ── Step 1: Extract raw text from backend ──────────────────────────────
      setStep("📄 Extracting resume text…");
      const fd = new FormData();
      fd.append("file", file);

      console.log("Calling extract endpoint:", `${API_BASE}/extract`);
      const extractRes = await axios.post(`${API_BASE}/extract`, fd);
      console.log("Extract response:", extractRes.data);
      const resumeText = extractRes.data.text;
      const filename = extractRes.data.filename;

      if (!resumeText || resumeText.trim().length < 50) {
        throw new Error(
          "Could not extract text from PDF. Make sure it's not a scanned image.",
        );
      }

      // ── Step 2: Deep AI analysis via Groq ─────────────────────────────────
      setStep("🤖 Running 15-module AI analysis…");
      console.log("Groq Key available:", !!GROQ_KEY);

      if (!GROQ_KEY) {
        throw new Error(
          "Groq API key is not configured. Check your .env file.",
        );
      }

      const groqRes = await axios.post(
        GROQ_URL,
        {
          model: GROQ_MODEL,
          messages: [
            {
              role: "user",
              content: buildPrompt(resumeText, jobDesc, jobRole),
            },
          ],
          max_tokens: 4096,
          temperature: 0.4,
        },
        {
          headers: {
            Authorization: `Bearer ${GROQ_KEY}`,
            "Content-Type": "application/json",
          },
        },
      );

      console.log("Groq response:", groqRes.data);

      // ── Step 3: Parse JSON response ────────────────────────────────────────
      setStep("🔍 Parsing results…");
      let raw = groqRes.data.choices[0].message.content.trim();
      console.log("Raw Groq content:", raw.substring(0, 200));

      // Strip markdown fences if Groq wraps in ```json
      raw = raw
        .replace(/^```json\s*/i, "")
        .replace(/^```\s*/i, "")
        .replace(/```$/i, "")
        .trim();

      // Find first { and last } to isolate JSON
      const start = raw.indexOf("{");
      const end = raw.lastIndexOf("}");
      if (start === -1 || end === -1) {
        console.error("Could not find JSON in response:", raw);
        throw new Error("AI did not return valid JSON.");
      }
      raw = raw.slice(start, end + 1);

      const parsed = JSON.parse(raw);
      console.log("Parsed analysis result:", parsed);
      const resumeId = extractRes.data.id;
      console.log("Resume ID:", resumeId);
      onResult({ ...parsed, filename, id: resumeId });
    } catch (e) {
      console.error("Analysis error details:", e);
      const errorMsg =
        e.response?.data?.error?.message ||
        e.message ||
        "Analysis failed. Check your Groq key and that Spring Boot is running.";
      console.error("Error message:", errorMsg);
      alert(errorMsg);
    }

    setStep("");
    setLoading(false);
  };

  return (
    <div className="upload-panel">
      <div className="upload-brand">
        <div className="brand-glow" />
        <h1 className="upload-title">
          AI Resume
          <br />
          <span className="title-accent">Analyzer Pro</span>
        </h1>
        <p className="upload-sub">
          15-module deep analysis — ATS scoring, skill gaps, cover letter,
          interview prep & recruiter decision.
        </p>
      </div>

      {/* Drop zone */}
      <div
        className={`dropzone${drag ? " dz-over" : ""}${file ? " dz-done" : ""}`}
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
        onClick={() => !file && fileRef.current?.click()}
      >
        <input
          ref={fileRef}
          type="file"
          accept=".pdf"
          style={{ display: "none" }}
          onChange={(e) => handleFile(e.target.files[0])}
        />
        {file ? (
          <div className="dz-file-row">
            <span className="dz-file-ico">📄</span>
            <div>
              <p className="dz-file-name">{file.name}</p>
              <p className="dz-file-size">
                {(file.size / 1024).toFixed(0)} KB · PDF
              </p>
            </div>
            <button
              className="dz-clear"
              onClick={(e) => {
                e.stopPropagation();
                setFile(null);
              }}
            >
              ✕
            </button>
          </div>
        ) : (
          <div className="dz-idle">
            <div className="dz-ico">⬆</div>
            <p className="dz-lbl">
              Drop PDF or <u>browse</u>
            </p>
            <p className="dz-hint">Max 10 MB</p>
          </div>
        )}
      </div>

      {/* Optional inputs */}
      <div className="optional-inputs">
        <label className="opt-label">
          Target Job Role <span className="opt-badge">optional</span>
        </label>
        <input
          className="opt-input"
          placeholder="e.g. Full Stack Developer, Data Engineer…"
          value={jobRole}
          onChange={(e) => setJobRole(e.target.value)}
        />
        <label className="opt-label">
          Job Description{" "}
          <span className="opt-badge">optional — improves matching</span>
        </label>
        <textarea
          className="opt-textarea"
          placeholder="Paste the job description here for keyword matching and cover letter generation…"
          rows={4}
          value={jobDesc}
          onChange={(e) => setJobDesc(e.target.value)}
        />
      </div>

      {/* CTA */}
      <button
        className={`cta${loading ? " cta-loading" : ""}`}
        onClick={handleAnalyze}
        disabled={loading || !file}
      >
        {loading ? (
          <>
            <span className="cta-spin" />
            <span>{step || "Analyzing…"}</span>
          </>
        ) : (
          <>
            Run Full Analysis <span className="cta-arrow">↗</span>
          </>
        )}
      </button>

      <p className="upload-note">
        Powered by <strong>Groq · LLaMA 3.1</strong> · Text extraction via
        Apache Tika · Stored in MongoDB
      </p>
    </div>
  );
}

// ── Results Panel ─────────────────────────────────────────────────────────────
function ResultsPanel({ data, onReset }) {
  const [activeTab, setActiveTab] = useState("overview");
  const [resumeId, setResumeId] = useState(data?.id || null);
  const [recommendations, setRecommendations] = useState([]);
  const [recommendationsLoading, setRecommendationsLoading] = useState(false);

  // Update resumeId when data changes
  useEffect(() => {
    if (data?.id) {
      setResumeId(data.id);
      console.log("Resume ID set to:", data.id);
    }
  }, [data?.id]);

  const handleRecommendations = async () => {
    if (!resumeId) {
      alert("Resume ID not found. Please upload again.");
      return;
    }

    setRecommendationsLoading(true);
    try {
      const { data: jobs } = await axios.get(
        `${API_BASE}/${resumeId}/recommendations`,
      );
      setRecommendations(jobs || []);
    } catch (error) {
      console.error("Error fetching recommendations:", error);
      alert(
        "Failed to fetch job recommendations. Make sure the backend is running.",
      );
    } finally {
      setRecommendationsLoading(false);
    }
  };

  const tabs = [
    { id: "overview", label: "Overview", icon: "🎯" },
    { id: "keywords", label: "Keywords", icon: "🔑" },
    { id: "gaps", label: "Skill Gaps", icon: "📊" },
    { id: "feedback", label: "Feedback", icon: "✏️" },
    { id: "interview", label: "Interview", icon: "🎤" },
    { id: "coverletter", label: "Cover Letter", icon: "📝" },
    { id: "decision", label: "Recruiter", icon: "👔" },
    { id: "jobs", label: "Job Matches", icon: "💼" },
  ];

  const d = data;

  return (
    <div className="results-panel">
      {/* Topbar */}
      <div className="results-topbar">
        <div className="results-file">
          <span className="results-file-ico">📄</span>
          <span className="results-file-name">{d.filename}</span>
        </div>
        <button className="reset-btn" onClick={onReset}>
          ← New Analysis
        </button>
      </div>

      {/* Hero scores */}
      <div className="hero-scores">
        <div className="hero-card">
          <ScoreRing score={d.ats_score || 0} />
          <div className="hero-card-info">
            <h2 className="hero-card-title">ATS Score</h2>
            <p className="hero-card-desc">{d.score_explanation}</p>
          </div>
        </div>
        {d.job_match?.match_percentage != null && (
          <div className="hero-card">
            <ScoreRing score={d.job_match.match_percentage} />
            <div className="hero-card-info">
              <h2 className="hero-card-title">Job Match</h2>
              <p className="hero-card-desc">{d.job_match.analysis}</p>
            </div>
          </div>
        )}
      </div>

      {/* Tabs */}
      <div className="result-tabs">
        {tabs.map((t) => (
          <button
            key={t.id}
            className={`rtab${activeTab === t.id ? " rtab-on" : ""}`}
            onClick={() => setActiveTab(t.id)}
          >
            <span>{t.icon}</span>
            {t.label}
          </button>
        ))}
      </div>

      {/* ── Tab Panes ── */}
      <div className="tab-content">
        {/* OVERVIEW */}
        {activeTab === "overview" && (
          <div className="tab-pane">
            {d.parsed_data && (
              <Section title="Parsed Resume Data" icon="👤">
                <div className="parsed-grid">
                  {d.parsed_data.name && (
                    <div className="parsed-row">
                      <span className="parsed-key">Name</span>
                      <span className="parsed-val">{d.parsed_data.name}</span>
                    </div>
                  )}
                  {d.parsed_data.skills?.length > 0 && (
                    <div className="parsed-row">
                      <span className="parsed-key">Skills</span>
                      <div className="tag-group">
                        {d.parsed_data.skills.map((s, i) => (
                          <Tag key={i} text={s} variant="accent" />
                        ))}
                      </div>
                    </div>
                  )}
                  {d.parsed_data.education?.length > 0 && (
                    <div className="parsed-row">
                      <span className="parsed-key">Education</span>
                      <div className="list-items">
                        {d.parsed_data.education.map((e, i) => (
                          <p key={i} className="list-item">
                            {typeof e === "string"
                              ? e
                              : `${e.degree || ""} ${e.institution || ""} ${
                                  e.duration ? `(${e.duration})` : ""
                                } ${e.cgpa ? `${e.cgpa}` : ""}`.trim()}
                          </p>
                        ))}
                      </div>
                    </div>
                  )}
                  {d.parsed_data.experience?.length > 0 && (
                    <div className="parsed-row">
                      <span className="parsed-key">Experience</span>
                      <div className="list-items">
                        {d.parsed_data.experience.map((e, i) => (
                          <p key={i} className="list-item">
                            {typeof e === "string"
                              ? e
                              : `${e.title || ""} ${e.company || ""} ${
                                  e.duration ? `(${e.duration})` : ""
                                } ${e.description ? `- ${e.description}` : ""}`.trim()}
                          </p>
                        ))}
                      </div>
                    </div>
                  )}
                  {d.parsed_data.projects?.length > 0 && (
                    <div className="parsed-row">
                      <span className="parsed-key">Projects</span>
                      <div className="list-items">
                        {d.parsed_data.projects.map((p, i) => (
                          <p key={i} className="list-item">
                            {typeof p === "string"
                              ? p
                              : `${p.name || ""} ${
                                  p.description ? `- ${p.description}` : ""
                                } ${p.link ? `(${p.link})` : ""}`.trim()}
                          </p>
                        ))}
                      </div>
                    </div>
                  )}
                </div>
              </Section>
            )}

            {d.generated_summary && (
              <Section title="Generated Professional Summary" icon="✨">
                <CopyBox text={d.generated_summary} label="Summary" />
              </Section>
            )}

            {d.career_insights && (
              <Section title="Career Insights" icon="🚀">
                <div className="two-col">
                  <div>
                    <p className="col-title">Career Paths</p>
                    {d.career_insights.career_paths?.map((c, i) => (
                      <div key={i} className="insight-item">
                        🗺 {c}
                      </div>
                    ))}
                  </div>
                  <div>
                    <p className="col-title">Next Roles</p>
                    {d.career_insights.next_roles?.map((r, i) => (
                      <div key={i} className="insight-item">
                        💼 {r}
                      </div>
                    ))}
                  </div>
                </div>
              </Section>
            )}
          </div>
        )}

        {/* KEYWORDS */}
        {activeTab === "keywords" && d.keyword_analysis && (
          <div className="tab-pane">
            <Section title="Matched Keywords" icon="✅">
              <div className="tag-group">
                {d.keyword_analysis.matched_keywords?.map((k, i) => (
                  <Tag key={i} text={k} variant="success" />
                ))}
              </div>
            </Section>
            <Section title="Missing Keywords" icon="❌">
              <div className="tag-group">
                {d.keyword_analysis.missing_keywords?.map((k, i) => (
                  <Tag key={i} text={k} variant="danger" />
                ))}
              </div>
            </Section>
            <Section title="Suggested Keywords to Add" icon="💡">
              <div className="tag-group">
                {d.keyword_analysis.suggested_keywords?.map((k, i) => (
                  <Tag key={i} text={k} variant="accent" />
                ))}
              </div>
            </Section>
          </div>
        )}

        {/* SKILL GAPS */}
        {activeTab === "gaps" && d.skill_gap && (
          <div className="tab-pane">
            <Section title="Missing Skills" icon="🔴">
              <div className="tag-group">
                {d.skill_gap.missing_skills?.map((s, i) => (
                  <Tag key={i} text={s} variant="danger" />
                ))}
              </div>
            </Section>
            <Section title="Priority Learning Order" icon="📋">
              {d.skill_gap.priority_order?.map((item, i) => (
                <div key={i} className="priority-row">
                  <span className="priority-num">{i + 1}</span>
                  <span className="priority-text">{item}</span>
                  <ProgressBar
                    value={Math.max(20, 100 - i * 15)}
                    cls={i === 0 ? "pb-high" : i <= 2 ? "pb-mid" : "pb-low"}
                  />
                </div>
              ))}
            </Section>
            <Section title="Recommended Courses / Certifications" icon="🎓">
              {d.skill_gap.recommended_learning?.map((item, i) => (
                <div key={i} className="learn-item">
                  📚 {item}
                </div>
              ))}
            </Section>
          </div>
        )}

        {/* FEEDBACK */}
        {activeTab === "feedback" && (
          <div className="tab-pane">
            {d.section_feedback && (
              <Section title="Section-wise Feedback" icon="✏️">
                {Object.entries(d.section_feedback).map(
                  ([key, val]) =>
                    val && (
                      <div key={key} className="feedback-block">
                        <p className="feedback-key">
                          {key.charAt(0).toUpperCase() + key.slice(1)}
                        </p>
                        <p className="feedback-val">{val}</p>
                      </div>
                    ),
                )}
              </Section>
            )}
            {d.bullet_improvements?.length > 0 && (
              <Section title="Bullet Point Improvements" icon="🔁">
                {d.bullet_improvements.map((b, i) => (
                  <div key={i} className="bullet-block">
                    {typeof b === "string" ? (
                      <p className="bullet-text">{b}</p>
                    ) : (
                      <>
                        {b.original && (
                          <p className="bullet-before">❌ {b.original}</p>
                        )}
                        {b.improved && (
                          <p className="bullet-after">✅ {b.improved}</p>
                        )}
                      </>
                    )}
                  </div>
                ))}
              </Section>
            )}
            {d.ats_issues?.length > 0 && (
              <Section title="ATS Formatting Issues" icon="⚠️">
                {d.ats_issues.map((issue, i) => (
                  <div key={i} className="issue-item">
                    ⚠️ {issue}
                  </div>
                ))}
              </Section>
            )}
          </div>
        )}

        {/* INTERVIEW */}
        {activeTab === "interview" && d.interview_questions && (
          <div className="tab-pane">
            <Section title="Technical Questions" icon="💻">
              <ol className="q-list">
                {d.interview_questions.technical?.map((q, i) => (
                  <li key={i} className="q-item">
                    {q}
                  </li>
                ))}
              </ol>
            </Section>
            <Section title="HR Questions" icon="🤝">
              <ol className="q-list">
                {d.interview_questions.hr?.map((q, i) => (
                  <li key={i} className="q-item">
                    {q}
                  </li>
                ))}
              </ol>
            </Section>
            <Section title="Project-Based Questions" icon="🔨">
              <ol className="q-list">
                {d.interview_questions.project_based?.map((q, i) => (
                  <li key={i} className="q-item">
                    {q}
                  </li>
                ))}
              </ol>
            </Section>
          </div>
        )}

        {/* COVER LETTER */}
        {activeTab === "coverletter" && (
          <div className="tab-pane">
            <Section title="Generated Cover Letter" icon="📝">
              {d.cover_letter ? (
                <CopyBox text={d.cover_letter} label="Cover Letter" />
              ) : (
                <p className="empty-msg">
                  No cover letter generated. Add a job description for better
                  results.
                </p>
              )}
            </Section>
          </div>
        )}

        {/* RECRUITER DECISION */}
        {activeTab === "decision" && d.recruiter_decision && (
          <div className="tab-pane">
            <div
              className={`decision-banner ${
                d.recruiter_decision.decision?.toLowerCase().includes("no")
                  ? "decision-nohire"
                  : "decision-hire"
              }`}
            >
              <span className="decision-icon">
                {d.recruiter_decision.decision?.toLowerCase().includes("no")
                  ? "❌"
                  : "✅"}
              </span>
              <span className="decision-text">
                {d.recruiter_decision.decision}
              </span>
            </div>

            <div className="decision-grid">
              <Section title="Strengths" icon="💪">
                {d.recruiter_decision.strengths?.map((s, i) => (
                  <div key={i} className="strength-item">
                    ✅ {s}
                  </div>
                ))}
              </Section>
              <Section title="Red Flags" icon="🚩">
                {d.recruiter_decision.red_flags?.map((r, i) => (
                  <div key={i} className="redflag-item">
                    🚩 {r}
                  </div>
                ))}
              </Section>
            </div>

            <Section title="Detailed Reasons" icon="📋">
              {d.recruiter_decision.reasons?.map((r, i) => (
                <div key={i} className="reason-item">
                  • {r}
                </div>
              ))}
            </Section>
          </div>
        )}

        {/* JOB RECOMMENDATIONS */}
        {activeTab === "jobs" && (
          <div className="tab-pane">
            <Section title="AI-Powered Job Matches" icon="💼">
              <p className="section-desc">
                Based on your resume skills and experience, we've matched you
                with relevant job opportunities.
              </p>

              <button
                className="fetch-jobs-btn"
                onClick={handleRecommendations}
                disabled={recommendationsLoading}
              >
                {recommendationsLoading
                  ? "⏳ Fetching jobs..."
                  : "🔍 Find Job Matches"}
              </button>

              {recommendations.length > 0 ? (
                <div className="jobs-grid">
                  {recommendations.map((job, idx) => (
                    <div key={job.jobId || idx} className="job-card">
                      <div className="job-header">
                        <h3 className="job-title">{job.jobTitle}</h3>
                        <span className="job-score">
                          {Math.round(job.matchScore || 0)}% Match
                        </span>
                      </div>

                      <p className="job-company">
                        🏢 {job.companyName || "Company"}
                      </p>

                      <p className="job-location">
                        📍 {job.location || "Remote"}
                      </p>

                      {job.employmentType && (
                        <p className="job-type">💼 {job.employmentType}</p>
                      )}

                      {(job.salaryMin || job.salaryMax) && (
                        <p className="job-salary">
                          💰 ${job.salaryMin?.toLocaleString() || "N/A"} - $
                          {job.salaryMax?.toLocaleString() || "N/A"}
                        </p>
                      )}

                      <div className="job-skills">
                        <span className="skills-label">
                          Skills Match: {job.matchedSkillsCount || 0}/
                          {job.totalRequiredSkills || 0}
                        </span>
                      </div>

                      {job.matchReason && (
                        <p className="job-reason">
                          <em>"{job.matchReason}"</em>
                        </p>
                      )}

                      {job.jobUrl && (
                        <a
                          href={job.jobUrl}
                          target="_blank"
                          rel="noopener noreferrer"
                          className="job-link"
                        >
                          View Job →
                        </a>
                      )}
                    </div>
                  ))}
                </div>
              ) : (
                <p className="empty-msg">
                  {recommendationsLoading
                    ? "Loading..."
                    : "Click 'Find Job Matches' to see recommendations"}
                </p>
              )}
            </Section>
          </div>
        )}
      </div>
    </div>
  );
}

// ── Root ──────────────────────────────────────────────────────────────────────
export default function App() {
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);

  return (
    <div className="app-wrap">
      <div className="noise" aria-hidden="true" />
      {!result ? (
        <UploadPanel
          onResult={setResult}
          loading={loading}
          setLoading={setLoading}
        />
      ) : (
        <ResultsPanel data={result} onReset={() => setResult(null)} />
      )}
    </div>
  );
}
