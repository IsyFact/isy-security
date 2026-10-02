package de.bund.bva.isyfact.security.oauth2.client.authentication.ropc;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.jspecify.annotations.Nullable;

/**
 * Response of an OAuth2 token request.
 *
 * <p>This class is part of the custom ROPC-flow, which, since the original ROPC-flow was removed in Spring Security 7,
 * is now implemented with a custom logic. It is compatible with Spring Security 7.
 *
 * @param accessToken the access token value
 * @param tokenType   the token type (e.g. {@code Bearer})
 * @param scopes      the granted scopes, never {@code null}
 * @param expiresAt   the point in time when the access token expires, may be {@code null} if not provided
 */
public record OAuth2TokenResponse(
        String accessToken,
        String tokenType,
        Set<String> scopes,
        @Nullable Instant expiresAt) {

    public OAuth2TokenResponse {
        scopes = (scopes == null) ? Collections.emptySet() : Collections.unmodifiableSet(new LinkedHashSet<>(scopes));
    }
}
