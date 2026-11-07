export function createApiClient({ onAuthFailure, authBasePath = "/api/auth" }) {
  let refreshPromise = null;

  async function refreshAccessToken() {
    if (!refreshPromise) {
      const sendRefreshRequest = () => fetch(`${authBasePath}/refresh`, {
          method: "POST",
          credentials: "include"
        })
        .then((response) => response.ok)
        .catch(() => false);

      const refreshRequest = typeof navigator !== "undefined" && navigator.locks
        ? navigator.locks.request("routine-impulse-session-refresh", sendRefreshRequest)
        : sendRefreshRequest();

      refreshPromise = Promise.resolve(refreshRequest)
        .finally(() => {
          refreshPromise = null;
        });
    }

    return refreshPromise;
  }

  async function apiFetch(input, init = {}, options = {}) {
    const { retryOnUnauthorized = true } = options;
    const requestInit = {
      credentials: "include",
      ...init
    };

    const response = await fetch(input, requestInit);
    if (response.status !== 401 || !retryOnUnauthorized) {
      return response;
    }

    const refreshed = await refreshAccessToken();
    if (!refreshed) {
      if (onAuthFailure) {
        onAuthFailure();
      }
      return response;
    }

    const retryResponse = await fetch(input, requestInit);
    if (retryResponse.status === 401 && onAuthFailure) {
      onAuthFailure();
    }

    return retryResponse;
  }

  return {
    apiFetch,
    refreshAccessToken
  };
}
