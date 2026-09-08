package de.bund.bva.isyfact.security.oauth2.client.authentication.ropc;

/**
 * Client for requesting OAuth2 access tokens.
 *
 * <p>This class is part of the custom ROPC-flow, which, since the original ROPC-flow was removed in Spring Security 7,
 * is now implemented with a custom logic. It is compatible with Spring Security 7.
 */
public interface OAuth2TokenClient {

    /**
     * Performs an OAuth2 ROPC grant against the token endpoint configured in
     * the request {@link org.springframework.security.oauth2.client.registration.ClientRegistration}.
     *
     * @param request the password grant request
     * @return the unified token response
     */
    OAuth2TokenResponse passwordGrant(OAuth2PasswordGrantRequest request);
}
