package org.routineimpulse.dto;

public class SessionTokens {
	private final String accessToken;
	private final String sessionToken;

	public SessionTokens(String accessToken, String sessionToken) {
		this.accessToken = accessToken;
		this.sessionToken = sessionToken;
	}

	public String getAccessToken() {
		return accessToken;
	}

	public String getSessionToken() {
		return sessionToken;
	}
}