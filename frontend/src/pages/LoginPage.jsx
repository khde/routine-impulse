import { useState } from "react";
import { Link, Navigate, useNavigate } from "react-router-dom";
import PageShell from "../components/PageShell";
import StatusMessage from "../components/StatusMessage";
import { AUTH_API_BASE } from "../config/api";
import { parseError } from "../utils/parseError";

export default function LoginPage({ loggedIn, setLoggedIn, apiFetch }) {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [status, setStatus] = useState("Enter your login credentials.");
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  if (loggedIn) {
    return <Navigate to="/dashboard" replace />;
  }

  async function handleLogin(event) {
    event.preventDefault();
    const normalizedUsername = username.trim();

    if (!normalizedUsername) {
      setStatus("Username is required.");
      return;
    }

    if (!password) {
      setStatus("Password is required.");
      return;
    }

    if (password.length < 8) {
      setStatus("Password must be at least 8 characters.");
      return;
    }

    setLoading(true);
    setStatus("Logging in...");

    try {
      const response = await apiFetch(
        `${AUTH_API_BASE}/login`,
        {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ username: normalizedUsername, password })
        },
        {
          retryOnUnauthorized: false
        }
      );

      if (!response.ok) {
        throw new Error(await parseError(response));
      }

      setLoggedIn(true);
      navigate("/dashboard", { replace: true });
    } catch (error) {
      setStatus(error.message || "Login failed.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <PageShell title="Login" subtitle="Use your account to continue.">
      <form onSubmit={handleLogin} className="form" noValidate>
        <label>
          Username
          <input
            type="text"
            value={username}
            onChange={(event) => setUsername(event.target.value)}
            required
            maxLength={20}
            disabled={loading}
          />
        </label>

        <label>
          Password
          <input
            type="password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            required
            minLength={8}
            disabled={loading}
          />
        </label>

        <button type="submit" className="primary" disabled={loading}>
          {loading ? "Please wait..." : "Login"}
        </button>
      </form>

      <StatusMessage message={status} />

      <p className="small-link">
        <Link to="/signup">No account? Signup</Link>
      </p>
      <p className="small-link">
        <Link to="/">Back to start page</Link>
      </p>
    </PageShell>
  );
}