package com.lumencloud.lumen.auth.support.passkey;

import com.lumencloud.lumen.auth.support.base.OAuth2ResourceOwnerBaseAuthenticationConverter;
import com.lumencloud.lumen.common.core.constant.SecurityConstants;
import com.lumencloud.lumen.common.security.util.OAuth2EndpointUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.Set;

/**
 * OAuth2 passkey authentication converter.
 */
public class OAuth2ResourceOwnerPasskeyAuthenticationConverter
		extends OAuth2ResourceOwnerBaseAuthenticationConverter<OAuth2ResourceOwnerPasskeyAuthenticationToken> {

	private static final String PARAM_CREDENTIAL_ID = "credentialId";

	private static final String PARAM_CLIENT_DATA_JSON = "clientDataJSON";

	private static final String PARAM_AUTHENTICATOR_DATA = "authenticatorData";

	private static final String PARAM_SIGNATURE = "signature";

	@Override
	public boolean support(String grantType) {
		return SecurityConstants.PASSKEY.equals(grantType);
	}

	@Override
	public OAuth2ResourceOwnerPasskeyAuthenticationToken buildToken(String grantType, Authentication clientPrincipal,
			Set<String> requestedScopes, Map<String, Object> additionalParameters) {
		return new OAuth2ResourceOwnerPasskeyAuthenticationToken(new AuthorizationGrantType(grantType),
				clientPrincipal, requestedScopes, additionalParameters);
	}

	@Override
	public void checkParams(HttpServletRequest request) {
		MultiValueMap<String, String> parameters = OAuth2EndpointUtils.getParameters(request);
		requireSingle(parameters, SecurityConstants.USERNAME);
		requireSingle(parameters, PARAM_CREDENTIAL_ID);
		requireSingle(parameters, PARAM_CLIENT_DATA_JSON);
		requireSingle(parameters, PARAM_AUTHENTICATOR_DATA);
		requireSingle(parameters, PARAM_SIGNATURE);
	}

	private void requireSingle(MultiValueMap<String, String> parameters, String parameterName) {
		String value = parameters.getFirst(parameterName);
		if (!StringUtils.hasText(value) || parameters.get(parameterName).size() != 1) {
			OAuth2EndpointUtils.throwError(OAuth2ErrorCodes.INVALID_REQUEST, parameterName,
					OAuth2EndpointUtils.ACCESS_TOKEN_REQUEST_ERROR_URI);
		}
	}

}
