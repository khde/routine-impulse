export async function parseError(response) {
  try {
    const text = await response.text();
    if (!text) {
      return `Request failed (${response.status})`;
    }

    try {
      const parsed = JSON.parse(text);
      return parsed.message || parsed.error || text;
    } catch {
      return text;
    }
  } catch {
    return "Unexpected network error";
  }
}