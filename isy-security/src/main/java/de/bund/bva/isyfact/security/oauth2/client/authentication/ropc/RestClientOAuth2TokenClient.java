package de.bund.bva.isyfact.security.oauth2.client.authentication.ropc;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.core.http.converter.OAuth2ErrorHttpMessageConverter;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriUtils;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * {@link OAuth2TokenClient} implementation that uses the Spring {@link RestClient} to communicate with the token
 * endpoint.
 *
 * <p>This class is part of the custom ROPC-flow, which, since the original ROPC-flow was removed in Spring Security 7,
 * is now implemented with a custom logic. It is compatible with Spring Security 7.
 */
public class RestClientOAuth2TokenClient implements OAuth2TokenClient {

    /** OAuth2 request parameter value for the password grant type. */
    private static final String PASSWORD_GRANT_TYPE = "password";

    /** OAuth2 request parameter name for the username. */
    private static final String USERNAME_PARAMETER = "username";

    /** OAuth2 request parameter name for the password. */
    private static final String PASSWORD_PARAMETER = "password";

    /** OAuth2 error code for an invalid token response. */
    private static final String INVALID_TOKEN_RESPONSE_ERROR_CODE = "invalid_token_response";

    /** Value of the {@code token_type} field supported by this client, per RFC 6750. */
    private static final String BEARER_TOKEN_TYPE = "bearer";

    /** {@link ClientAuthenticationMethod}s supported by this client for the ROPC flow. */
    private static final Set<ClientAuthenticationMethod> SUPPORTED_CLIENT_AUTHENTICATION_METHODS =
            Set.of(ClientAuthenticationMethod.CLIENT_SECRET_BASIC, ClientAuthenticationMethod.CLIENT_SECRET_POST);

    /** Converts error responses (e.g. {@code invalid_grant}) from the token endpoint into an {@link OAuth2Error}. */
    private static final OAuth2ErrorHttpMessageConverter OAUTH2_ERROR_CONVERTER = new OAuth2ErrorHttpMessageConverter();

    /** The {@link RestClient} used for the token requests. */
    private final RestClient restClient;

    /** The name of the HTTP header used to pass the BHKNZ, may be {@code null} if BHKNZ is not supported. */
    @Nullable
    private final String bhknzHeaderName;

    /**
     * Creates a token client using a default {@link RestClient} without BHKNZ header support.
     */
    public RestClientOAuth2TokenClient() {
        this(RestClient.create(), null);
    }

    /**
     * Creates a token client using a default {@link RestClient}.
     *
     * @param bhknzHeaderName the name of the HTTP header used to pass the BHKNZ, may be {@code null}
     */
    public RestClientOAuth2TokenClient(@Nullable String bhknzHeaderName) {
        this(RestClient.create(), bhknzHeaderName);
    }

    /**
     * Creates a token client.
     *
     * @param restClient      the {@link RestClient} to use for the token requests
     * @param bhknzHeaderName the name of the HTTP header used to pass the BHKNZ, may be {@code null}
     */
    public RestClientOAuth2TokenClient(RestClient restClient, @Nullable String bhknzHeaderName) {
        Assert.notNull(restClient, "restClient cannot be null");
        this.restClient = restClient;
        this.bhknzHeaderName = bhknzHeaderName;
    }

    @Override
    public OAuth2TokenResponse passwordGrant(OAuth2PasswordGrantRequest request) {
        Assert.notNull(request, "request cannot be null");
        ClientRegistration clientRegistration = request.clientRegistration();
        validateClientAuthenticationMethod(clientRegistration);

        RestClient client = this.restClient;
        if (request.bhknz() != null) {
            Assert.state(this.bhknzHeaderName != null,
                    "bhknzHeaderName must be configured to send a BHKNZ header");
            String headerName = this.bhknzHeaderName;
            String headerValue = request.bhknz();
            // set the optional BHKNZ header via a RestClient request interceptor
            client = client.mutate()
                    .requestInterceptor((httpRequest, body, execution) -> {
                        httpRequest.getHeaders().add(headerName, headerValue);
                        return execution.execute(httpRequest, body);
                    })
                    .build();
        }

        MultiValueMap<String, String> parameters = buildParameters(request);

        try {
            TokenEndpointResponse response = client.post()
                    .uri(URI.create(clientRegistration.getProviderDetails().getTokenUri()))
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .accept(MediaType.APPLICATION_JSON)
                    .headers(headers -> applyClientAuthentication(headers, clientRegistration))
                    .body(parameters)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> handleErrorResponse(res))
                    .body(TokenEndpointResponse.class);
            return toTokenResponse(response, clientRegistration);
        } catch (OAuth2AuthorizationException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            OAuth2Error oauth2Error = new OAuth2Error(INVALID_TOKEN_RESPONSE_ERROR_CODE,
                    "An error occurred while attempting to retrieve the OAuth 2.0 Access Token Response: "
                            + ex.getMessage(), null);
            throw new OAuth2AuthorizationException(oauth2Error, ex);
        }
    }

    /**
     * Ensures that only client authentication methods supported by this client are used, so that requests do not
     * silently omit the client credentials.
     *
     * @param clientRegistration the client registration to validate
     */
    private void validateClientAuthenticationMethod(ClientRegistration clientRegistration) {
        ClientAuthenticationMethod authenticationMethod = clientRegistration.getClientAuthenticationMethod();
        if (!SUPPORTED_CLIENT_AUTHENTICATION_METHODS.contains(authenticationMethod)) {
            throw new IllegalArgumentException(
                    "Unsupported ClientAuthenticationMethod '%s' for the ROPC flow of client registration '%s'. Only '%s' and '%s' are supported."
                            .formatted(authenticationMethod.getValue(), clientRegistration.getRegistrationId(),
                                    ClientAuthenticationMethod.CLIENT_SECRET_BASIC.getValue(),
                                    ClientAuthenticationMethod.CLIENT_SECRET_POST.getValue()));
        }
    }

    /**
     * Reads the {@link OAuth2Error} from an error response of the token endpoint (e.g. {@code invalid_grant}) and
     * throws it as an {@link OAuth2AuthorizationException}, so that callers can distinguish OAuth2 protocol errors
     * from transport-level errors.
     *
     * @param response the erroneous HTTP response
     */
    private void handleErrorResponse(ClientHttpResponse response) {
        OAuth2Error oauth2Error;
        try {
            oauth2Error = OAUTH2_ERROR_CONVERTER.read(OAuth2Error.class, response);
        } catch (Exception ex) {
            oauth2Error = new OAuth2Error(INVALID_TOKEN_RESPONSE_ERROR_CODE,
                    "An error occurred while attempting to retrieve the OAuth 2.0 Access Token Response: "
                            + ex.getMessage(), null);
        }
        throw new OAuth2AuthorizationException(oauth2Error);
    }

    private MultiValueMap<String, String> buildParameters(OAuth2PasswordGrantRequest request) {
        ClientRegistration clientRegistration = request.clientRegistration();
        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        parameters.add(OAuth2ParameterNames.GRANT_TYPE, PASSWORD_GRANT_TYPE);
        parameters.add(USERNAME_PARAMETER, request.username());
        parameters.add(PASSWORD_PARAMETER, request.password());
        if (!CollectionUtils.isEmpty(clientRegistration.getScopes())) {
            parameters.add(OAuth2ParameterNames.SCOPE,
                    StringUtils.collectionToDelimitedString(clientRegistration.getScopes(), " "));
        }
        if (ClientAuthenticationMethod.CLIENT_SECRET_POST.equals(clientRegistration.getClientAuthenticationMethod())) {
            parameters.add(OAuth2ParameterNames.CLIENT_ID, clientRegistration.getClientId());
            parameters.add(OAuth2ParameterNames.CLIENT_SECRET, clientRegistration.getClientSecret());
        }
        return parameters;
    }

    private void applyClientAuthentication(HttpHeaders headers, ClientRegistration clientRegistration) {
        if (ClientAuthenticationMethod.CLIENT_SECRET_BASIC.equals(clientRegistration.getClientAuthenticationMethod())) {
            // per RFC 6749 Appendix B, spaces must be percent-encoded as %20, not as '+' as URLEncoder would do
            String clientId = UriUtils.encode(clientRegistration.getClientId(), StandardCharsets.UTF_8);
            String clientSecret = UriUtils.encode(clientRegistration.getClientSecret(), StandardCharsets.UTF_8);
            headers.setBasicAuth(clientId, clientSecret);
        }
    }

    private OAuth2TokenResponse toTokenResponse(@Nullable TokenEndpointResponse response,
                                                ClientRegistration clientRegistration) {
        if (response == null || !StringUtils.hasText(response.accessToken())) {
            OAuth2Error oauth2Error = new OAuth2Error(INVALID_TOKEN_RESPONSE_ERROR_CODE,
                    "The token endpoint returned an empty or invalid access token response", null);
            throw new OAuth2AuthorizationException(oauth2Error);
        }

        if (!BEARER_TOKEN_TYPE.equalsIgnoreCase(response.tokenType())) {
            OAuth2Error oauth2Error = new OAuth2Error(INVALID_TOKEN_RESPONSE_ERROR_CODE,
                    "The token endpoint returned an unsupported token_type: " + response.tokenType(), null);
            throw new OAuth2AuthorizationException(oauth2Error);
        }

        Instant expiresAt = (response.expiresIn() != null)
                ? Instant.now().plusSeconds(response.expiresIn())
                : null;

        Set<String> scopes;
        if (StringUtils.hasText(response.scope())) {
            scopes = new LinkedHashSet<>(Arrays.asList(StringUtils.delimitedListToStringArray(response.scope(), " ")));
        } else if (clientRegistration.getScopes() != null) {
            scopes = new LinkedHashSet<>(clientRegistration.getScopes());
        } else {
            scopes = new LinkedHashSet<>();
        }

        return new OAuth2TokenResponse(response.accessToken(), response.tokenType(), scopes, expiresAt);
    }

    /**
     * Internal representation of the raw JSON token endpoint response.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record TokenEndpointResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("token_type") String tokenType,
            @JsonProperty("expires_in") Long expiresIn,
            @JsonProperty("scope") String scope,
            @JsonProperty("refresh_token") String refreshToken) {
    }
}
