package de.bund.bva.isyfact.security.oauth2.client.authentication;

import java.time.Instant;

import org.jspecify.annotations.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.client.ClientAuthorizationException;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import de.bund.bva.isyfact.security.oauth2.client.authentication.ropc.OAuth2PasswordGrantRequest;
import de.bund.bva.isyfact.security.oauth2.client.authentication.ropc.OAuth2TokenClient;
import de.bund.bva.isyfact.security.oauth2.client.authentication.ropc.OAuth2TokenResponse;
import de.bund.bva.isyfact.security.oauth2.client.authentication.token.PasswordClientRegistrationAuthenticationToken;

/**
 * Authentication Provider to obtain an {@link Authentication} with the OAuth2 ROPC flow
 * using an externally created Client Registration object.
 *
 * <p>This class is part of the custom ROPC-flow, which, since the original ROPC-flow was removed in Spring Security 7,
 * is now implemented with a custom logic. It is compatible with Spring Security 7.
 */
public class PasswordClientRegistrationAuthenticationProvider extends IsyOAuth2AuthenticationProvider {

    /**
     * Custom variable for the authorization grant type for the OAuth2 ROPC-flow.
     */
    private static final AuthorizationGrantType PASSWORD = new AuthorizationGrantType("password");

    /** Token client used to execute the password grant request against the token endpoint. */
    private final OAuth2TokenClient oauth2TokenClient;

    /** Organizational unit combined with the bhknz to form the value of the bhknz header, may be {@code null}. */
    @Nullable
    private final String defaultCertificateOu;

    public PasswordClientRegistrationAuthenticationProvider(JwtAuthenticationConverter jwtAuthenticationConverter,
                                                            OAuth2TokenClient oauth2TokenClient,
                                                            @Nullable String defaultCertificateOu) {
        super(jwtAuthenticationConverter);
        this.oauth2TokenClient = oauth2TokenClient;
        this.defaultCertificateOu = defaultCertificateOu;
    }

    @Override
    @Nullable
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        if (!(authentication instanceof PasswordClientRegistrationAuthenticationToken)) {
            return null;
        }

        PasswordClientRegistrationAuthenticationToken token = (PasswordClientRegistrationAuthenticationToken) authentication;
        ClientRegistration clientRegistration = token.getClientRegistration();

        OAuth2AuthorizedClient authorizedClient =
                obtainAuthorizedClient(clientRegistration, token.getUsername(), token.getPassword(), token.getBhknz());
        if (authorizedClient == null) {
            throw new ClientAuthorizationException(new OAuth2Error(OAuth2ErrorCodes.INVALID_GRANT), clientRegistration.getRegistrationId(),
                    "clientRegistration.authorizationGrantType must be AuthorizationGrantType with value 'password'");
        }

        return createJwtAuthentication(authorizedClient);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return PasswordClientRegistrationAuthenticationToken.class.isAssignableFrom(authentication);
    }

    /**
     * Obtains an {@link OAuth2AuthorizedClient} by executing a password grant request against the token endpoint
     * configured in the given {@link ClientRegistration}.
     *
     * @param clientRegistration the client registration, must use the password authorization grant type
     * @param username the resource owner's username
     * @param password the resource owner's password
     * @param bhknz the optional bhknz to send as part of the request, may be {@code null}
     * @return the authorized client, or {@code null} if the client registration does not use the password grant type
     */
    @Nullable
    protected OAuth2AuthorizedClient obtainAuthorizedClient(ClientRegistration clientRegistration,
                                                            String username, String password, @Nullable String bhknz) {
        Assert.hasText(username, "username cannot be empty for client: " + clientRegistration.getRegistrationId());
        Assert.hasText(password, "password cannot be empty for client: " + clientRegistration.getRegistrationId());

        if (!PASSWORD.equals(clientRegistration.getAuthorizationGrantType())) {
            return null;
        }

        OAuth2PasswordGrantRequest request =
                new OAuth2PasswordGrantRequest(clientRegistration, username, password, buildBhknzHeaderValue(bhknz));

        OAuth2TokenResponse tokenResponse;
        try {
            tokenResponse = oauth2TokenClient.passwordGrant(request);
        } catch (OAuth2AuthorizationException ex) {
            throw new ClientAuthorizationException(ex.getError(), clientRegistration.getRegistrationId(), ex.getMessage(), ex);
        }

        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                tokenResponse.accessToken(),
                Instant.now(),
                tokenResponse.expiresAt(),
                tokenResponse.scopes());

        return new OAuth2AuthorizedClient(clientRegistration, username, accessToken);
    }

    /**
     * Combines the given bhknz with the configured Certificate-OUto form the value that is sent
     * as part of the bhknz header.
     *
     * @param bhknz the bhknz, may be {@code null} if no bhknz should be sent
     * @return the combined header value, or {@code null} if bhknz is {@code null}
     */
    @Nullable
    private String buildBhknzHeaderValue(@Nullable String bhknz) {
        if (bhknz == null) {
            return null;
        }
        Assert.state(StringUtils.hasText(defaultCertificateOu), "defaultCertificateOu must be configured when bhknz is set");
        return bhknz + ":" + defaultCertificateOu;
    }
}
