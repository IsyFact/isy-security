package de.bund.bva.isyfact.security.oauth2.client.authentication.ropc;

import org.jspecify.annotations.Nullable;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.util.Assert;

/**
 * Request DTO for an OAuth2 ROPC-grant.
 *
 * <p>This class is part of the custom ROPC-flow, which, since the original ROPC-flow was removed in Spring Security 7,
 * is now implemented with a custom logic. It is compatible with Spring Security 7.
 *
 * @param clientRegistration the client registration holding the token endpoint and client authentication details
 * @param username           the resource owner's username
 * @param password           the resource owner's password
 * @param bhknz              an optional value for the BHKNZ header, may be {@code null}
 */
public record OAuth2PasswordGrantRequest(
        ClientRegistration clientRegistration,
        String username,
        String password,
        @Nullable String bhknz) {

    public OAuth2PasswordGrantRequest {
        Assert.notNull(clientRegistration, "clientRegistration cannot be null");
        Assert.hasText(username, "username cannot be empty");
        Assert.hasText(password, "password cannot be empty");
    }

    /**
     * Constructs a request without a BHKNZ header value.
     *
     * @param clientRegistration the client registration
     * @param username           the resource owner's username
     * @param password           the resource owner's password
     */
    public OAuth2PasswordGrantRequest(ClientRegistration clientRegistration, String username, String password) {
        this(clientRegistration, username, password, null);
    }

    /**
     * Overridden to avoid leaking the plaintext password, which the generated record {@code toString()} would
     * otherwise include.
     */
    @Override
    public String toString() {
        return "OAuth2PasswordGrantRequest[clientRegistration=" + clientRegistration.getRegistrationId()
                + ", username=" + username + ", password=[PROTECTED], bhknz=" + bhknz + "]";
    }
}
