package de.bund.bva.isyfact.security.test.oidcprovider;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.springframework.security.oauth2.core.AuthorizationGrantType.CLIENT_CREDENTIALS;
import static org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames.GRANT_TYPE;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.matching.ContainsPattern;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import com.github.tomakehurst.wiremock.matching.NegativeRegexPattern;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;

/**
 * This class mock an OIDC provider for tests environments in which no dedicated OIDC provider can be used.
 * This requires WireMock as a standalone service.
 */
public abstract class OidcProviderMockBase extends EmbeddedOidcProviderStub {

    /**
     * Name of the HTTP header that contains the BHKNZ and second OU.
     */
    static final String BHKNZ_HEADER_NAME = "x-client-cert-bhknz";

    /**
     * Endpoint for the OpenID Connect configuration.
     */
    static final String OIDC_CONFIG_ENDPOINT = "/.well-known/openid-configuration";

    /**
     * Endpoint for the authorization request.
     */
    static final String AUTHORIZATION_ENDPOINT = "/protocol/openid-connect/auth";

    /**
     * Endpoint for the JSON Web Key Set (JWKS).
     */
    static final String JWKS_ENDPOINT = "/protocol/openid-connect/certs";

    /**
     * Endpoint for the token request.
     */
    static final String TOKEN_ENDPOINT = "/protocol/openid-connect/token";

    /**
     * Default value for the second OU used when none is explicitly set.
     */
    private static final String DEFAULT_SECOND_OU = "TESTOU";

    /**
     * OAuth2 grant type value for the ROPC flow.
     */
    private static final String PASSWORD_GRANT_TYPE_VALUE = "password";

    /**
     * Stores all client-related stub mappings keyed by client ID.
     */
    private final Map<String, Set<StubMapping>> clientMappings = new HashMap<>();

    /**
     * Stores all user-related ROPC stub mappings keyed by {@code clientId + ":" + username}.
     */
    private final Map<String, Set<StubMapping>> userMappings = new HashMap<>();


    public OidcProviderMockBase(String host, int port, String issuerPath) {
        super(host, port, issuerPath);
    }

    public OidcProviderMockBase(String host, int port, String issuerPath, int tokenLifespan) {
        super(host, port, issuerPath, tokenLifespan);
    }

    public OidcProviderMockBase(String host, int port, String issuerPath, String publicKey, String privateKey) {
        super(host, port, issuerPath, publicKey, privateKey);
    }

    public OidcProviderMockBase(String host, int port, String issuerPath, String publicKey, String privateKey, int tokenLifespan) {
        super(host, port, issuerPath, publicKey, privateKey, tokenLifespan);
    }

    protected void init(String host, int port) {
        WireMock.configureFor(host, port);
        WireMock.reset();
        setupOIDCConfigEndpoint();
        setupDefaultTokenEndpoints();
        setupJwksEndpoint();
    }

    public void addClient(String clientId, String secret, Set<String> roles) {
        final String accessTokenResponse = getAccessTokenResponse(clientId, "service-account-" + clientId, Optional.empty(), roles);
        clientMappings.put(clientId, generateClientMapping(clientId, secret, accessTokenResponse));
    }

    public void removeClient(String clientId) {
        final Set<StubMapping> clientMapping = clientMappings.remove(clientId);
        if (clientMapping != null) {
            for (StubMapping mapping : clientMapping) {
                WireMock.removeStub(mapping);
            }
        }
    }

    public void removeAllClients() {
        for (String client : clientMappings.keySet()) {
            removeClient(client);
        }
    }

    /**
     * Registers a resource owner (user) that can be authenticated via the OAuth2 ROPC flow
     * against the client identified by {@code clientId}/{@code secret}.
     *
     * @param clientId the client ID of the ROPC client (sent via HTTP Basic auth)
     * @param secret   the client secret of the ROPC client
     * @param username the resource owner's username
     * @param password the resource owner's password
     * @param bhknz    an optional BHKNZ to include in the issued access token
     * @param roles    the roles to include in the issued access token
     */
    public void addUser(String clientId, String secret, String username, String password, Optional<String> bhknz, Set<String> roles) {
        String accessTokenResponse = getAccessTokenResponse(clientId, username, bhknz, roles);
        userMappings.put(userKey(clientId, username), generateUserMapping(clientId, secret, username, password, accessTokenResponse));
    }

    public void removeUser(String clientId, String username) {
        final Set<StubMapping> userMapping = userMappings.remove(userKey(clientId, username));
        if (userMapping != null) {
            for (StubMapping mapping : userMapping) {
                WireMock.removeStub(mapping);
            }
        }
    }

    public void removeAllUsers() {
        for (String key : new HashSet<>(userMappings.keySet())) {
            final Set<StubMapping> userMapping = userMappings.remove(key);
            for (StubMapping mapping : userMapping) {
                WireMock.removeStub(mapping);
            }
        }
    }

    private String userKey(String clientId, String username) {
        return clientId + ":" + username;
    }

    /**
     * The OpenID configuration endpoint is called to get the location of the other endpoints (like the JWKS, token and issuer endpoint).
     *
     * @return stub mapping for the OpenID configuration endpoint
     */
    private StubMapping setupOIDCConfigEndpoint() {
        return stubFor(get(urlEqualTo(appendToIssuerPath(OIDC_CONFIG_ENDPOINT)))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(getOIDCConfigResponse(JWKS_ENDPOINT, AUTHORIZATION_ENDPOINT, TOKEN_ENDPOINT))
                ));
    }

    private StubMapping setupJwksEndpoint() {
        return stubFor(get(urlEqualTo(appendToIssuerPath(JWKS_ENDPOINT)))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(getJwksResponse())
                ));
    }

    /**
     * Set up error responses when no stub for the token endpoint (user or client) were found or the request contains invalid data.
     */
    @SuppressWarnings("java:S2068")
    private Set<StubMapping> setupDefaultTokenEndpoints() {
        Set<StubMapping> stubMappings = new HashSet<>();

        String tokenEndpoint = appendToIssuerPath(TOKEN_ENDPOINT);

        String clientCredentialsGrantType = "%s=%s".formatted(GRANT_TYPE, CLIENT_CREDENTIALS.getValue());

        stubMappings.add(stubFor(post(urlEqualTo(tokenEndpoint)).atPriority(2)
                .withRequestBody(new NegativeRegexPattern(".*%s=.*".formatted(GRANT_TYPE)))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.BAD_REQUEST.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(createErrorResponse("invalid_request", "Missing grant type"))
                )));

        stubMappings.add(stubFor(post(urlEqualTo(tokenEndpoint)).atPriority(3)
                .withRequestBody(new EqualToPattern(clientCredentialsGrantType))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.BAD_REQUEST.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(createErrorResponse("unsupported_grant_type", "Invalid client-id or secret"))
                )));

        stubMappings.add(stubFor(post(urlEqualTo(tokenEndpoint)).atPriority(4)
                .withRequestBody(new NegativeRegexPattern(".*username=.+"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.UNAUTHORIZED.value())
                        .withHeader(HttpHeaders.WWW_AUTHENTICATE, "dummy")
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(createErrorResponse("invalid_grant", "Missing username"))
                )));

        stubMappings.add(stubFor(post(urlEqualTo(tokenEndpoint)).atPriority(4)
                .withRequestBody(new NegativeRegexPattern(".*password=.+"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.UNAUTHORIZED.value())
                        .withHeader(HttpHeaders.WWW_AUTHENTICATE, "dummy")
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(createErrorResponse("invalid_grant", "Missing password"))
                )));

        String passwordGrantType = "%s=%s".formatted(GRANT_TYPE, PASSWORD_GRANT_TYPE_VALUE);
        stubMappings.add(stubFor(post(urlEqualTo(tokenEndpoint)).atPriority(5)
                .withRequestBody(new ContainsPattern(passwordGrantType))
                .withRequestBody(new ContainsPattern("username="))
                .withRequestBody(new ContainsPattern("password="))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.UNAUTHORIZED.value())
                        .withHeader(HttpHeaders.WWW_AUTHENTICATE, "dummy")
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(createErrorResponse("invalid_grant", "Invalid username or password"))
                )));

        return stubMappings;
    }

    /**
     * Builds the stub mapping for a successful ROPC token request for the given resource owner.
     *
     * @param clientId          the client ID of the ROPC client (sent via HTTP Basic auth)
     * @param secret            the client secret of the ROPC client
     * @param username          the resource owner's username
     * @param password          the resource owner's password
     * @param accessTokenResponse the JSON access token response to return on a successful match
     * @return the stub mappings created for this user
     */
    private Set<StubMapping> generateUserMapping(String clientId, String secret, String username, String password,
                                                 String accessTokenResponse) {
        Set<StubMapping> stubMappings = new HashSet<>();

        String tokenEndpoint = appendToIssuerPath(TOKEN_ENDPOINT);
        String passwordGrantType = "%s=%s".formatted(GRANT_TYPE, PASSWORD_GRANT_TYPE_VALUE);

        stubMappings.add(stubFor(post(urlEqualTo(tokenEndpoint)).atPriority(1)
                .withRequestBody(new ContainsPattern(passwordGrantType))
                .withRequestBody(new ContainsPattern("username=" + username))
                .withRequestBody(new ContainsPattern("password=" + password))
                .withBasicAuth(clientId, secret)
                .willReturn(aResponse()
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withStatus(HttpStatus.OK.value())
                        .withBody(accessTokenResponse)
                )));

        return stubMappings;
    }

    private Set<StubMapping> generateClientMapping(String clientId, String secret, String accessTokenResponse) {
        Set<StubMapping> stubMappings = new HashSet<>();

        String clientCredentialsGrantType = "%s=%s".formatted(GRANT_TYPE, CLIENT_CREDENTIALS.getValue());

        stubMappings.add(stubFor(
                post(urlEqualTo(appendToIssuerPath(TOKEN_ENDPOINT)))
                        .atPriority(1)
                        .withRequestBody(new EqualToPattern(clientCredentialsGrantType)).withBasicAuth(clientId, secret)
                        .willReturn(aResponse()
                                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                .withStatus(HttpStatus.OK.value())
                                .withBody(accessTokenResponse)
                        )));

        return stubMappings;
    }

    private String createErrorResponse(String errorCode, String errorDescription) {
        return "{\n" +
                "  \"error\": \"" + errorCode + "\"," +
                "  \"error_description\": \"" + errorDescription + "\"" +
                "}";
    }

    private String appendToIssuerPath(String endpoint) {
        return appendPath(getIssuer(), endpoint).getPath();
    }

}
