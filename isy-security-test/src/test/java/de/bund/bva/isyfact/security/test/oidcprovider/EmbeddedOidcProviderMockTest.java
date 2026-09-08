package de.bund.bva.isyfact.security.test.oidcprovider;

import java.text.ParseException;
import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.core.oidc.StandardClaimNames;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.JWTParser;

import static de.bund.bva.isyfact.security.test.oidcprovider.EmbeddedOidcProviderStub.BHKNZ_CLAIM_NAME;
import static de.bund.bva.isyfact.security.test.oidcprovider.OidcProviderMockBase.JWKS_ENDPOINT;
import static de.bund.bva.isyfact.security.test.oidcprovider.OidcProviderMockBase.OIDC_CONFIG_ENDPOINT;
import static de.bund.bva.isyfact.security.test.oidcprovider.OidcProviderMockBase.TOKEN_ENDPOINT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import reactor.core.publisher.Mono;

class EmbeddedOidcProviderMockTest {

    public static final ObjectMapper mapper = new ObjectMapper();

    private static final String PASSWORD_GRANT_TYPE = "password";

    private static final String host = "localhost";

    private static final int port = 9096;

    private static final String issuerPath = "/auth/realms/testrealm";

    @RegisterExtension
    private static final EmbeddedOidcProviderMock mock = new EmbeddedOidcProviderMock(host, port, issuerPath);

    public static final String CC_ID = "client-client";
    public static final String CC_SECRET = "cc-secret";
    public static final String USER_WITHOUT_BHKNZ = "user-without-bhknz";
    public static final String USER_PASSWORD = "test";

    public static final String ROPC_CLIENT_ID = "ropc-client";
    public static final String ROPC_CLIENT_SECRET = "ropc-secret";
    public static final String ROPC_USERNAME = "ropc-user";
    public static final String ROPC_PASSWORD = "ropc-pw";
    public static final String ROPC_USERNAME_WITH_BHKNZ = "ropc-user-with-bhknz";
    public static final String ROPC_BHKNZ = "123456:TESTOU";

    private static WebClient webClient;

    @BeforeAll
    public static void setupWebClient() {
        webClient = WebClient.builder().baseUrl("http://" + host + ":" + port + issuerPath).build();
        mock.addClient(CC_ID, CC_SECRET, Collections.emptySet());
        mock.addUser(ROPC_CLIENT_ID, ROPC_CLIENT_SECRET, ROPC_USERNAME, ROPC_PASSWORD, Optional.empty(), Collections.singleton("Rolle_B"));
        mock.addUser(ROPC_CLIENT_ID, ROPC_CLIENT_SECRET, ROPC_USERNAME_WITH_BHKNZ, ROPC_PASSWORD, Optional.of(ROPC_BHKNZ),
                Collections.singleton("Rolle_B"));
    }

    @Test
    void testOidcConfigEndpoint() {
        HttpStatusCode status = webClient.get().uri(OIDC_CONFIG_ENDPOINT)
                .exchangeToMono(response -> Mono.just(response.statusCode())).block();

        assertEquals(HttpStatus.OK, status);
    }

    @Test
    void testJwksEndpoint() {
        HttpStatusCode status = webClient.get().uri(JWKS_ENDPOINT)
                .exchangeToMono(response -> Mono.just(response.statusCode())).block();

        assertEquals(HttpStatus.OK, status);
    }

    @Test
    void testTokenEndpointWithoutBodyFails() {
        String body = webClient.post().uri(TOKEN_ENDPOINT)
                .exchangeToMono(response -> {
                    assertEquals(HttpStatus.BAD_REQUEST, response.statusCode());
                    return response.bodyToMono(String.class);
                }).block();

        assertThat(body).contains("invalid_request", "Missing grant type");
    }

    @Test
    void testTokenEndpointWithOnlyGrantTypeFails() {
        String body = webClient.post().uri(TOKEN_ENDPOINT)
                .body(BodyInserters.fromFormData(OAuth2ParameterNames.GRANT_TYPE, AuthorizationGrantType.CLIENT_CREDENTIALS.getValue()))
                .exchangeToMono(response -> {
                    assertEquals(HttpStatus.BAD_REQUEST, response.statusCode());
                    return response.bodyToMono(String.class);
                }).block();

        assertThat(body).contains("unsupported_grant_type", "Invalid client-id or secret");
    }

    @Test
    void testTokenEndpointWithoutUsernameFails() {
        String body = webClient.post().uri(TOKEN_ENDPOINT)
                .body(BodyInserters.fromFormData(OAuth2ParameterNames.GRANT_TYPE, AuthorizationGrantType.CLIENT_CREDENTIALS.getValue())
                        .with("password", USER_PASSWORD)
                ).exchangeToMono(response -> {
                    assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode());
                    return response.bodyToMono(String.class);
                }).block();

        assertThat(body).contains("invalid_grant", "Missing username");
    }

    @Test
    void testTokenEndpointWithoutPasswordFails() {
        String body = webClient.post().uri(TOKEN_ENDPOINT)
                .body(BodyInserters.fromFormData(OAuth2ParameterNames.GRANT_TYPE, AuthorizationGrantType.CLIENT_CREDENTIALS.getValue())
                        .with("username", USER_WITHOUT_BHKNZ)
                ).exchangeToMono(response -> {
                    assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode());
                    return response.bodyToMono(String.class);
                }).block();

        assertThat(body).contains("invalid_grant", "Missing password");
    }

    @Test
    void testClientWorks() throws JsonProcessingException, ParseException {
        String body = webClient.post().uri(TOKEN_ENDPOINT)
                .headers(headers -> headers.setBasicAuth(CC_ID, CC_SECRET))
                .body(BodyInserters.fromFormData(OAuth2ParameterNames.GRANT_TYPE, AuthorizationGrantType.CLIENT_CREDENTIALS.getValue()))
                .exchangeToMono(response -> {
                    assertEquals(HttpStatus.OK, response.statusCode());
                    return response.bodyToMono(String.class);
                }).block();

        String token = mapper.readTree(body).get("access_token").asText();
        JWTClaimsSet claims = JWTParser.parse(token).getJWTClaimsSet();

        assertEquals("service-account-client-client", claims.getStringClaim(StandardClaimNames.PREFERRED_USERNAME));
        assertFalse(claims.getClaims().containsKey(BHKNZ_CLAIM_NAME));
    }

    @Test
    void testRopcUserWorks() throws JsonProcessingException, ParseException {
        String body = webClient.post().uri(TOKEN_ENDPOINT)
                .headers(headers -> headers.setBasicAuth(ROPC_CLIENT_ID, ROPC_CLIENT_SECRET))
                .body(BodyInserters.fromFormData(OAuth2ParameterNames.GRANT_TYPE, PASSWORD_GRANT_TYPE)
                        .with("username", ROPC_USERNAME)
                        .with("password", ROPC_PASSWORD)
                ).exchangeToMono(response -> {
                    assertEquals(HttpStatus.OK, response.statusCode());
                    return response.bodyToMono(String.class);
                }).block();

        String token = mapper.readTree(body).get("access_token").asText();
        JWTClaimsSet claims = JWTParser.parse(token).getJWTClaimsSet();

        assertEquals(ROPC_USERNAME, claims.getStringClaim(StandardClaimNames.PREFERRED_USERNAME));
        assertTrue(claims.getStringListClaim("roles").contains("Rolle_B"));
        assertFalse(claims.getClaims().containsKey(BHKNZ_CLAIM_NAME));
    }

    @Test
    void testRopcUserWithBhknzWorks() throws JsonProcessingException, ParseException {
        String body = webClient.post().uri(TOKEN_ENDPOINT)
                .headers(headers -> headers.setBasicAuth(ROPC_CLIENT_ID, ROPC_CLIENT_SECRET))
                .body(BodyInserters.fromFormData(OAuth2ParameterNames.GRANT_TYPE, PASSWORD_GRANT_TYPE)
                        .with("username", ROPC_USERNAME_WITH_BHKNZ)
                        .with("password", ROPC_PASSWORD)
                ).exchangeToMono(response -> {
                    assertEquals(HttpStatus.OK, response.statusCode());
                    return response.bodyToMono(String.class);
                }).block();

        String token = mapper.readTree(body).get("access_token").asText();
        JWTClaimsSet claims = JWTParser.parse(token).getJWTClaimsSet();

        assertEquals(ROPC_USERNAME_WITH_BHKNZ, claims.getStringClaim(StandardClaimNames.PREFERRED_USERNAME));
        assertEquals(ROPC_BHKNZ, claims.getStringClaim(BHKNZ_CLAIM_NAME));
    }

    @Test
    void testRopcUserWithWrongPasswordFails() {
        String body = webClient.post().uri(TOKEN_ENDPOINT)
                .headers(headers -> headers.setBasicAuth(ROPC_CLIENT_ID, ROPC_CLIENT_SECRET))
                .body(BodyInserters.fromFormData(OAuth2ParameterNames.GRANT_TYPE, PASSWORD_GRANT_TYPE)
                        .with("username", ROPC_USERNAME)
                        .with("password", "wrong-password")
                ).exchangeToMono(response -> {
                    assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode());
                    return response.bodyToMono(String.class);
                }).block();

        assertThat(body).contains("invalid_grant");
    }

    @Test
    void testRopcUserWithUnknownUsernameFails() {
        String body = webClient.post().uri(TOKEN_ENDPOINT)
                .headers(headers -> headers.setBasicAuth(ROPC_CLIENT_ID, ROPC_CLIENT_SECRET))
                .body(BodyInserters.fromFormData(OAuth2ParameterNames.GRANT_TYPE, PASSWORD_GRANT_TYPE)
                        .with("username", "unknown-user")
                        .with("password", ROPC_PASSWORD)
                ).exchangeToMono(response -> {
                    assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode());
                    return response.bodyToMono(String.class);
                }).block();

        assertThat(body).contains("invalid_grant");
    }

}