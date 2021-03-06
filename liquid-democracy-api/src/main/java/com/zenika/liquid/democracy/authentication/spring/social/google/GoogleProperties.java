package com.zenika.liquid.democracy.authentication.spring.social.google;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("spring.social.google")
public class GoogleProperties {
	/**
	 * Application id.
	 */
	private String appId;

	/**
	 * Application secret.
	 */
	private String appSecret;

	public String getAppId() {
		return this.appId;
	}

	public void setAppId(String appId) {
		this.appId = appId;
	}

	public String getAppSecret() {
		return this.appSecret;
	}

	public void setAppSecret(String appSecret) {
		this.appSecret = appSecret;
	}
}