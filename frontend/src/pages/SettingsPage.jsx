import { useState } from "react";
import AppSidebarLayout from "../components/AppSidebarLayout";
import { AUTH_API_BASE } from "../config/api";
import { parseError } from "../utils/parseError";

export default function SettingsPage({ apiFetch }) {
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [status, setStatus] = useState("");
  const [loading, setLoading] = useState(false);

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
      setStatus("Password changed successfully.");
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
        <form className="profile-form" onSubmit={handleChangePassword}>
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

      {status && <p className="status">{status}</p>}
    </AppSidebarLayout>
  );
}