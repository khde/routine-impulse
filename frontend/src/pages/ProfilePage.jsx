import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import AppSidebarLayout from "../components/AppSidebarLayout";
import { AUTH_API_BASE, USER_PROFILE_API } from "../config/api";
import { parseError } from "../utils/parseError";

export default function ProfilePage({ setLoggedIn, apiFetch }) {
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);
  const [status, setStatus] = useState("");
  const [showDeleteModal, setShowDeleteModal] = useState(false);
  const [deleteStep, setDeleteStep] = useState("confirm");
  const [deleteConfirmText, setDeleteConfirmText] = useState("");
  const [deleteLoading, setDeleteLoading] = useState(false);
  const navigate = useNavigate();

  useEffect(() => {
    loadProfile();
  }, []);

  async function loadProfile() {
    setLoading(true);
    setStatus("");

    try {
      const response = await apiFetch(USER_PROFILE_API, { method: "GET" });
      if (!response.ok) {
        throw new Error(await parseError(response));
      }

      const profileData = await response.json();
      setProfile(profileData);
    } catch (error) {
      setStatus(error.message || "Failed to load profile.");
    } finally {
      setLoading(false);
    }
  }

  async function handleDeleteAccount() {
    if (deleteConfirmText !== "DELETE") {
      setStatus("Type DELETE to confirm account deletion.");
      return;
    }

    setDeleteLoading(true);
    setStatus("");

    try {
      const response = await apiFetch(`${AUTH_API_BASE}/delete-account`, {
        method: "DELETE"
      });

      if (!response.ok) {
        throw new Error(await parseError(response));
      }

      setLoggedIn(false);
      navigate("/signup", { replace: true });
    } catch (error) {
      setStatus(error.message || "Failed to delete account.");
      setDeleteLoading(false);
    }
  }

  function openDeleteModal() {
    setShowDeleteModal(true);
    setDeleteStep("confirm");
    setDeleteConfirmText("");
    setStatus("");
  }

  function closeDeleteModal() {
    if (deleteLoading) {
      return;
    }
    setShowDeleteModal(false);
    setDeleteStep("confirm");
    setDeleteConfirmText("");
  }

  return (
    <AppSidebarLayout title="Profile">
      <div className="profile-grid">
        <section className="panel-card">
          <h3 className="panel-title">Account</h3>

          {loading ? (
            <p className="info-text">Loading profile...</p>
          ) : (
            <dl className="profile-list">
              <div>
                <dt>Username</dt>
                <dd>{profile?.username || "-"}</dd>
              </div>
              <div>
                <dt>Email</dt>
                <dd>{profile?.email || "-"}</dd>
              </div>
              <div>
                <dt>Account creation</dt>
                <dd>{formatDate(profile?.creationDate)}</dd>
              </div>
            </dl>
          )}
        </section>

        <section className="panel-card danger-zone">
          <h3 className="panel-title">Account Deletion</h3>
          <button
            type="button"
            className="danger-action"
            disabled={deleteLoading}
            onClick={openDeleteModal}
          >
            Delete account
          </button>
        </section>
      </div>

      {status && <p className="status">{status}</p>}

      {showDeleteModal && (
        <div className="modal-overlay" role="presentation" onClick={closeDeleteModal}>
          <section
            className="modal-card"
            role="dialog"
            aria-modal="true"
            aria-label="Delete account confirmation"
            onClick={(event) => event.stopPropagation()}
          >
            {deleteStep === "confirm" ? (
              <>
                <h3 className="modal-title">Delete account</h3>
                <p className="info-text">
                  Are you sure you want to delete your account? It cannot be undone.
                </p>
                <div className="modal-actions">
                  <button type="button" className="danger-action" onClick={() => setDeleteStep("type")}>Delete</button>
                  <button type="button" className="secondary" onClick={closeDeleteModal}>Cancel</button>
                </div>
              </>
            ) : (
              <>
                <h3 className="modal-title">Delete account</h3>
                <label>
                  Type DELETE to continue
                  <input
                    type="text"
                    value={deleteConfirmText}
                    onChange={(event) => setDeleteConfirmText(event.target.value)}
                    disabled={deleteLoading}
                    autoFocus
                  />
                </label>
                <div className="modal-actions">
                  <button
                    type="button"
                    className="danger-action"
                    disabled={deleteLoading}
                    onClick={handleDeleteAccount}
                  >
                    {deleteLoading ? "Deleting..." : "Delete"}
                  </button>
                  <button type="button" className="secondary" disabled={deleteLoading} onClick={closeDeleteModal}>Cancel</button>
                </div>
              </>
            )}
          </section>
        </div>
      )}
    </AppSidebarLayout>
  );
}

function formatDate(value) {
  if (!value) {
    return "-";
  }

  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) {
    return value;
  }

  return parsed.toLocaleString();
}