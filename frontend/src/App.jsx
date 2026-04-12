import { useEffect, useMemo, useState } from "react";
import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";
import { createApiClient } from "./api/client";
import { AUTH_API_BASE } from "./config/api";
import StartPage from "./pages/StartPage";
import LoginPage from "./pages/LoginPage";
import SignupPage from "./pages/SignupPage";
import DashboardPage from "./pages/DashboardPage";
import ProtectedRoute from "./components/ProtectedRoute";
import PageShell from "./components/PageShell";

export default function App() {
  const [loggedIn, setLoggedIn] = useState(false);
  const [checkingSession, setCheckingSession] = useState(true);
  const { apiFetch, refreshAccessToken } = useMemo(
    () =>
      createApiClient({
        onAuthFailure: () => setLoggedIn(false),
        authBasePath: AUTH_API_BASE
      }),
    []
  );

  useEffect(() => {
    checkSession();
  }, []);

  async function checkSession() {
    try {
      const refreshed = await refreshAccessToken();
      setLoggedIn(refreshed);
    } catch {
      setLoggedIn(false);
    } finally {
      setCheckingSession(false);
    }
  }

  if (checkingSession) {
    return (
      <PageShell title="Routine Impulse" subtitle="Checking session..." />
    );
  }

  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<StartPage loggedIn={loggedIn} />} />
        <Route
          path="/login"
          element={<LoginPage loggedIn={loggedIn} setLoggedIn={setLoggedIn} apiFetch={apiFetch} />}
        />
        <Route
          path="/signup"
          element={<SignupPage loggedIn={loggedIn} setLoggedIn={setLoggedIn} apiFetch={apiFetch} />}
        />
        <Route
          path="/dashboard"
          element={
            <ProtectedRoute loggedIn={loggedIn}>
              <DashboardPage setLoggedIn={setLoggedIn} apiFetch={apiFetch} />
            </ProtectedRoute>
          }
        />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  );
}