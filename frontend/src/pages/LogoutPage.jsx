import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import AppSidebarLayout from "../components/AppSidebarLayout";
import { AUTH_API_BASE } from "../config/api";
import { parseError } from "../utils/parseError";

export default function LogoutPage({ setLoggedIn, apiFetch }) {
  const [status, setStatus] = useState("Logging out...");
  const navigate = useNavigate();

  useEffect(() => {
    handleLogout();
  }, []);

  async function handleLogout() {
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
      setStatus(`Logout failed: ${error.message || "Unknown error"}`);
    }
  }

  return (
    <AppSidebarLayout title="Logout">
      <p className="info-text">{status}</p>
    </AppSidebarLayout>
  );
}