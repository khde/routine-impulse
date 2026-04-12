import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import PageShell from "../components/PageShell";
import StatusMessage from "../components/StatusMessage";
import { AUTH_API_BASE, USER_PROFILE_API } from "../config/api";
import { parseError } from "../utils/parseError";

export default function DashboardPage({ setLoggedIn, apiFetch }) {
  const [status, setStatus] = useState("You are logged in.");
  const [loading, setLoading] = useState(false);
  const [profile, setProfile] = useState(null);
  const [profileLoading, setProfileLoading] = useState(true);
  const [profileError, setProfileError] = useState("");
  const navigate = useNavigate();

  useEffect(() => {
    loadProfile();
  }, []);

  async function loadProfile() {
    setProfileLoading(true);
    setProfileError("");

    try {
      const response = await apiFetch(USER_PROFILE_API, {
        method: "GET"
      });

      if (!response.ok) {
        throw new Error(await parseError(response));
      }

      const profileData = await response.json();
      setProfile(profileData);
    } catch (error) {
      setProfileError(error.message || "Failed to load profile.");
    } finally {
      setProfileLoading(false);
    }
  }

  async function handleLogout() {
    setLoading(true);
    setStatus("Logging out...");

    try {
      const response = await apiFetch(
        `${AUTH_API_BASE}/logout`,
        {
          method: "POST"
        },
        {
          retryOnUnauthorized: false
        }
      );

      if (!response.ok) {
        throw new Error(await parseError(response));
      }

      setLoggedIn(false);
      navigate("/login", { replace: true });
    } catch (error) {
      setStatus(error.message || "Logout failed.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <PageShell title="Dashboard" subtitle="Dashboard here.">
      {profileLoading ? (
        <p className="info-text">Loading profile...</p>
      ) : profileError ? (
        <StatusMessage message={profileError} />
      ) : (
        <>
          <p className="info-text">
            Signed in as <strong>{profile?.username}</strong>
          </p>
          <p className="info-text">Email: {profile?.email}</p>
          <p className="info-text">
            Joined: {profile?.creationDate ? new Date(profile.creationDate).toLocaleString() : "-"}
          </p>
        </>
      )}

      <button type="button" className="secondary" onClick={loadProfile} disabled={profileLoading || loading}>
        {profileLoading ? "Loading..." : "Reload profile"}
      </button>

      <button type="button" className="secondary" onClick={handleLogout} disabled={loading}>
        {loading ? "Please wait..." : "Logout"}
      </button>

      <StatusMessage message={status} ok />
    </PageShell>
  );
}