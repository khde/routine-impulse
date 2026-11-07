import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { Monitor, Smartphone, Trash2 } from "lucide-react";
import AppSidebarLayout from "../components/AppSidebarLayout";
import { AUTH_API_BASE } from "../config/api";
import { parseError } from "../utils/parseError";

export default function SettingsPage({ apiFetch, setLoggedIn }) {
  const navigate = useNavigate();
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [status, setStatus] = useState("");
  const [loading, setLoading] = useState(false);
  const [sessions, setSessions] = useState([]);
  const [sessionsLoading, setSessionsLoading] = useState(true);
  const [sessionsStatus, setSessionsStatus] = useState("");
  const [revokingSessionId, setRevokingSessionId] = useState(null);

  useEffect(() => {
    loadSessions();
  }, []);

  async function loadSessions() {
    setSessionsLoading(true);
    setSessionsStatus("");
    try {
      const response = await apiFetch(`${AUTH_API_BASE}/sessions`);
      if (!response.ok) {
        throw new Error(await parseError(response));
      }
      setSessions(await response.json());
    } catch (error) {
      setSessionsStatus(error.message || "Failed to load sessions.");
    } finally {
      setSessionsLoading(false);
    }
  }

  async function handleRevokeSession(session) {
    setRevokingSessionId(session.id);
    setSessionsStatus("");
    try {
      const response = await apiFetch(`${AUTH_API_BASE}/sessions/${session.id}`, {
        method: "DELETE"
      });
      if (!response.ok) {
        throw new Error(await parseError(response));
      }

      if (session.current) {
        setLoggedIn(false);
        navigate("/login", { replace: true });
        return;
      }

      setSessions((currentSessions) => currentSessions.filter((item) => item.id !== session.id));
      setSessionsStatus("Session logged out.");
    } catch (error) {
      setSessionsStatus(error.message || "Failed to log out session.");
    } finally {
      setRevokingSessionId(null);
    }
  }

  async function handleChangePassword(event) {
    event.preventDefault();

    if (!currentPassword || !newPassword || !confirmPassword) {
      setStatus("Please fill in all password fields.");
      return;
    }

    if (newPassword.length < 8) {
      setStatus("New password must be at least 8 characters.");
      return;
    }

    if (newPassword !== confirmPassword) {
      setStatus("New password and confirm password do not match.");
      return;
    }

    setLoading(true);
    setStatus("");

    try {
      const response = await apiFetch(`${AUTH_API_BASE}/change-password`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          currentPassword,
          newPassword
        })
      });

      if (!response.ok) {
        throw new Error(await parseError(response));
      }

      setCurrentPassword("");
      setNewPassword("");
      setConfirmPassword("");
      setLoggedIn(false);
      navigate("/login", { replace: true });
    } catch (error) {
      setStatus(error.message || "Failed to change password.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <AppSidebarLayout title="Settings">
      <section className="panel-card">
        <h3 className="panel-title">Change Password</h3>
        <form className="account-form" onSubmit={handleChangePassword}>
          <label>
            Current Password
            <input
              type="password"
              value={currentPassword}
              onChange={(event) => setCurrentPassword(event.target.value)}
              disabled={loading}
              required
            />
          </label>

          <label>
            New Password
            <input
              type="password"
              value={newPassword}
              onChange={(event) => setNewPassword(event.target.value)}
              minLength={8}
              disabled={loading}
              required
            />
          </label>

          <label>
            Confirm New Password
            <input
              type="password"
              value={confirmPassword}
              onChange={(event) => setConfirmPassword(event.target.value)}
              minLength={8}
              disabled={loading}
              required
            />
          </label>

          <button type="submit" className="primary" disabled={loading}>
            {loading ? "Saving..." : "Update Password"}
          </button>
        </form>
      </section>

      <section className="panel-card sessions-panel">
        <div className="panel-header-row">
          <div>
            <h3 className="panel-title">Sessions</h3>
            <p className="sessions-description">Devices currently signed in to your account.</p>
          </div>
        </div>

        {sessionsLoading ? (
          <p className="sessions-empty">Loading sessions...</p>
        ) : sessions.length === 0 ? (
          <p className="sessions-empty">No active sessions found.</p>
        ) : (
          <ul className="session-list">
            {sessions.map((session) => {
              const deviceName = describeUserAgent(session.userAgent);
              const DeviceIcon = /mobile|tablet/i.test(session.userAgent || "") ? Smartphone : Monitor;
              return (
                <li className="session-row" key={session.id}>
                  <span className="session-device-icon" aria-hidden="true">
                    <DeviceIcon size={20} />
                  </span>
                  <div className="session-details">
                    <div className="session-heading">
                      <strong>{deviceName} - {describeOperatingSystem(session.userAgent)}</strong>
                      {session.current && <span className="current-session-badge">This device</span>}
                    </div>
                    <p>Last active {formatSessionDate(session.lastUsedAt)}</p>
                    <p>Signed in {formatSessionDate(session.createdAt)}</p>
                  </div>
                  <button
                    type="button"
                    className="session-revoke-button"
                    onClick={() => handleRevokeSession(session)}
                    disabled={revokingSessionId !== null}
                    aria-label={`Remove session for ${session.current ? "this device" : deviceName}`}
                    title={revokingSessionId === session.id ? "Removing session..." : "Remove session"}
                  >
                    <Trash2 size={16} aria-hidden="true" />
                  </button>
                </li>
              );
            })}
          </ul>
        )}
        {sessionsStatus && <p className="status">{sessionsStatus}</p>}
      </section>

      {status && <p className="status">{status}</p>}
    </AppSidebarLayout>
  );
}

function describeUserAgent(userAgent) {
  if (!userAgent) {
    return "Unknown device";
  }
  if (/Edg\//i.test(userAgent)) return "Microsoft Edge";
  if (/Firefox\//i.test(userAgent)) return "Firefox";
  if (/Chrome\//i.test(userAgent)) return "Google Chrome";
  if (/Safari\//i.test(userAgent)) return "Safari";
  return "Unknown browser";
}

function describeOperatingSystem(userAgent) {
  if (!userAgent) return "Unknown OS";
  if (/Windows NT/i.test(userAgent)) return "Windows";
  if (/Android/i.test(userAgent)) return "Android";
  if (/iPhone|iPad|iPod/i.test(userAgent)) return "iOS";
  if (/CrOS/i.test(userAgent)) return "ChromeOS";
  if (/Mac OS X|Macintosh/i.test(userAgent)) return "macOS";
  if (/Linux/i.test(userAgent)) return "Linux";
  return "Unknown OS";
}

function formatSessionDate(value) {
  if (!value) return "Unknown";
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? "Unknown" : date.toLocaleString();
}