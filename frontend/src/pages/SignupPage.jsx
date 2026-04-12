import { useState } from "react";
import { Link, Navigate, useNavigate } from "react-router-dom";
import PageShell from "../components/PageShell";
import StatusMessage from "../components/StatusMessage";
import { AUTH_API_BASE } from "../config/api";
import { parseError } from "../utils/parseError";

export default function SignupPage({ loggedIn, setLoggedIn, apiFetch }) {
  const [username, setUsername] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [status, setStatus] = useState("Create your account.");
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  if (loggedIn) {
    return <Navigate to="/dashboard" replace />;
  }

  async function handleSignup(event) {
    event.preventDefault();
    const normalizedUsername = username.trim();
    const normalizedEmail = email.trim().toLowerCase();

    if (!normalizedUsername) {
      setStatus("Username is required.");
      return;
    }

    if (!normalizedEmail) {
      setStatus("Email is required.");
      return;
    }

    if (!/^\S+@\S+\.\S+$/.test(normalizedEmail)) {
      setStatus("Please enter a valid email address.");
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

    if (!confirmPassword) {
      setStatus("Please confirm your password.");
      return;
    }

    if (password !== confirmPassword) {
      setStatus("Passwords do not match.");
      return;
    }

    setLoading(true);
    setStatus("Creating account...");

    try {
      const response = await apiFetch(
        `${AUTH_API_BASE}/signup`,
        {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ username: normalizedUsername, email: normalizedEmail, password })
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
      setStatus(error.message || "Signup failed.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <PageShell title="Signup" subtitle="Create your account to get started.">
      <form onSubmit={handleSignup} className="form" noValidate>
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
          Email
          <input
            type="email"
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            required
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

        <label>
          Confirm Password
          <input
            type="password"
            value={confirmPassword}
            onChange={(event) => setConfirmPassword(event.target.value)}
            required
            minLength={8}
            disabled={loading}
          />
        </label>

        <button type="submit" className="primary" disabled={loading}>
          {loading ? "Please wait..." : "Create account"}
        </button>
      </form>

      <StatusMessage message={status} />

      <p className="small-link">
        <Link to="/login">Already have an account? Login</Link>
      </p>
      <p className="small-link">
        <Link to="/">Back to start page</Link>
      </p>
    </PageShell>
  );
}