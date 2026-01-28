package de.bund.bva.isyfact.security.oauth2.client;

import java.lang.reflect.Field;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.AbstractOAuth2TokenAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import de.bund.bva.isyfact.security.AbstractOidcProviderTest;
import de.bund.bva.isyfact.security.autoconfigure.IsyOAuth2ClientAutoConfiguration;
import de.bund.bva.isyfact.security.autoconfigure.IsySecurityAutoConfigurationTest;
import de.bund.bva.isyfact.security.config.AdditionalCredentials;
import de.bund.bva.isyfact.security.oauth2.client.authentication.ClientCredentialsAuthorizedClientAuthenticationProvider;
import de.bund.bva.isyfact.security.oauth2.client.authentication.ClientCredentialsClientRegistrationAuthenticationProvider;
import de.bund.bva.isyfact.security.oauth2.client.authentication.token.ClientCredentialsClientRegistrationAuthenticationToken;
import de.bund.bva.isyfact.security.oauth2.client.authentication.token.ClientCredentialsRegistrationIdAuthenticationToken;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests the Authentifizierungsmanager with all available authentication providers.
 * Because this test defines mock beans it skips the actual bean creation in {@link IsyOAuth2ClientAutoConfiguration} which would
 * only create them if OAuth 2.0 clients are configured. The {@link IsySecurityAutoConfigurationTest} still makes sure that the
 * correct beans are created depending on the application configuration.
 */
@ActiveProfiles("test-clients")
@SpringBootTest
@TestPropertySource(properties = {
        "isy.security.cache.ttl=300",
        "isy.security.cache.maxelements=100",
        "isy.security.cache.token-expiration-time-offset=10"
})
public class AuthentifizierungsmanagerTest extends AbstractOidcProviderTest {

    @MockBean
    private ClientCredentialsAuthorizedClientAuthenticationProvider clientCredentialsAuthorizedClientAuthenticationProvider;

    @MockBean
    private ClientCredentialsClientRegistrationAuthenticationProvider clientCredentialsClientRegistrationAuthenticationProvider;

    @Autowired
    private Authentifizierungsmanager authentifizierungsmanager;

    @Autowired
    private ProviderManager isyOAuth2AuthenticationProviderManager;

    private JwtAuthenticationToken mockJwt;

    private Jwt mockToken;

    @BeforeEach
    public void configureMocks() throws NoSuchFieldException, IllegalAccessException {
        // clear authenticated principal
        SecurityContextHolder.getContext().setAuthentication(null);

        mockJwt = mock(JwtAuthenticationToken.class);
        JwtAuthenticationToken secondMockJwt = mock(JwtAuthenticationToken.class);
        mockToken = mock(Jwt.class);
        Field field = AbstractOAuth2TokenAuthenticationToken.class.getDeclaredField("token");
        field.setAccessible(true);
        field.set(mockJwt, mockToken);
        field.set(secondMockJwt, mockToken);
        when(mockJwt.getToken()).thenCallRealMethod();
        when(secondMockJwt.getToken()).thenCallRealMethod();
        when(mockToken.getExpiresAt()).thenReturn(Instant.now().plusSeconds(300));

        when(clientCredentialsAuthorizedClientAuthenticationProvider.supports(any())).thenCallRealMethod();
        when(clientCredentialsAuthorizedClientAuthenticationProvider.authenticate(any(Authentication.class))).thenReturn(mockJwt);

        when(clientCredentialsClientRegistrationAuthenticationProvider.supports(any())).thenCallRealMethod();
        when(clientCredentialsClientRegistrationAuthenticationProvider.authenticate(any(Authentication.class))).thenReturn(mockJwt);
    }

    @Test
    public void testHasAllProviders() {
        assertThat(isyOAuth2AuthenticationProviderManager.getProviders()).containsExactlyInAnyOrder(
                clientCredentialsAuthorizedClientAuthenticationProvider,
                clientCredentialsClientRegistrationAuthenticationProvider
        );
    }

    @Test
    public void testAuthWithClientRegistrationCC() {
        authentifizierungsmanager.authentifiziere("cc-client");

        ArgumentCaptor<ClientCredentialsRegistrationIdAuthenticationToken> tokenCaptor =
                ArgumentCaptor.forClass(ClientCredentialsRegistrationIdAuthenticationToken.class);
        verify(clientCredentialsAuthorizedClientAuthenticationProvider).authenticate(tokenCaptor.capture());
        assertEquals("cc-client", tokenCaptor.getValue().getRegistrationId());

        assertEquals(mockJwt, SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    public void testAuthWithRegistrationIdAndBhknz() {
        AdditionalCredentials credentials = AdditionalCredentials.createWithBhknz("900600");

        authentifizierungsmanager.authentifiziere("cc-client", credentials);

        ArgumentCaptor<ClientCredentialsRegistrationIdAuthenticationToken> tokenCaptor =
                ArgumentCaptor.forClass(ClientCredentialsRegistrationIdAuthenticationToken.class);
        verify(clientCredentialsAuthorizedClientAuthenticationProvider).authenticate(tokenCaptor.capture());

        assertEquals("cc-client", tokenCaptor.getValue().getRegistrationId());
        assertEquals("900600", tokenCaptor.getValue().getBhknz());
        assertEquals(mockJwt, SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    public void testAuthWithRegistrationIdAndNullCredentials() {
        assertThrows(IllegalArgumentException.class,
                () -> authentifizierungsmanager.authentifiziere("cc-client", (AdditionalCredentials) null));
    }

    @Test
    public void testAuthWithUnknownRegistrationIdAndCredentials() {
        AdditionalCredentials credentials = AdditionalCredentials.createWithBhknz("900600");

        assertThrows(IllegalArgumentException.class,
                () -> authentifizierungsmanager.authentifiziere("unknown-clientId", credentials));
    }

    @Test
    public void testAuthWithDirectClientRegistrationCC() {
        ClientRegistration clientRegistration = ClientRegistration.withRegistrationId("dummy-unused")
                .tokenUri("http://localhost:9095/auth/realms/testrealm")
                .clientId("client-credentials-test-client")
                .clientSecret("supersecretpassword")
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .build();

        authentifizierungsmanager.authentifiziere(clientRegistration);

        ArgumentCaptor<ClientCredentialsClientRegistrationAuthenticationToken> tokenCaptor =
                ArgumentCaptor.forClass(ClientCredentialsClientRegistrationAuthenticationToken.class);
        verify(clientCredentialsClientRegistrationAuthenticationProvider).authenticate(tokenCaptor.capture());
        assertEquals("dummy-unused", tokenCaptor.getValue().getClientRegistration().getRegistrationId());

        assertEquals(mockJwt, SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    public void testAuthWithNullClientRegistration() {
        assertThrows(IllegalArgumentException.class,
                () -> authentifizierungsmanager.authentifiziere((ClientRegistration) null));
    }

    @Test
    public void testAuthWithDirectClientRegistrationCCAndAdditionalCredentials() {
        ClientRegistration clientRegistration = ClientRegistration.withRegistrationId("custom-cc-client")
                .tokenUri("http://localhost:9095/auth/realms/testrealm/protocol/openid-connect/token")
                .jwkSetUri("http://localhost:9095/auth/realms/testrealm/protocol/openid-connect/certs")
                .clientId("client-credentials-test-client")
                .clientSecret("supersecretpassword")
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .build();

        AdditionalCredentials additionalCredentials = AdditionalCredentials.createWithBhknz("900600");

        authentifizierungsmanager.authentifiziere(clientRegistration, additionalCredentials);

        ArgumentCaptor<ClientCredentialsClientRegistrationAuthenticationToken> tokenCaptor =
                ArgumentCaptor.forClass(ClientCredentialsClientRegistrationAuthenticationToken.class);
        verify(clientCredentialsClientRegistrationAuthenticationProvider).authenticate(tokenCaptor.capture());

        ClientCredentialsClientRegistrationAuthenticationToken value = tokenCaptor.getValue();
        assertEquals("custom-cc-client", value.getClientRegistration().getRegistrationId());
        assertEquals("client-credentials-test-client", value.getClientRegistration().getClientId());
        assertEquals("supersecretpassword", value.getClientRegistration().getClientSecret());
        assertEquals("900600", value.getBhknz());

        assertEquals(mockJwt, SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    public void testAuthWithDirectClientRegistrationFailsForWrongGrantType() {
        ClientRegistration clientRegistration = ClientRegistration.withRegistrationId("failing-ropc-client")
                .tokenUri("http://localhost:9095/auth/realms/testrealm/protocol/openid-connect/token")
                .jwkSetUri("http://localhost:9095/auth/realms/testrealm/protocol/openid-connect/certs")
                .clientId("resource-owner-password-credentials-test-client")
                .clientSecret("hypersecretpassword")
                .authorizationGrantType(AuthorizationGrantType.PASSWORD)
                .build();

        assertThrows(IllegalArgumentException.class,
                () -> authentifizierungsmanager.authentifiziere(clientRegistration));
    }

    @Test
    public void testAuthWithDirectClientRegistrationFailsForUnsupportedGrantType() {
        ClientRegistration clientRegistration = ClientRegistration.withRegistrationId("failing-client")
                .tokenUri("http://localhost:9095/auth/realms/testrealm/protocol/openid-connect/token")
                .jwkSetUri("http://localhost:9095/auth/realms/testrealm/protocol/openid-connect/certs")
                .clientId("test-client")
                .clientSecret("testsecret")
                .authorizationGrantType(AuthorizationGrantType.DEVICE_CODE)
                .build();

        AdditionalCredentials credentials = AdditionalCredentials.createWithUsernamePassword("user", "password");

        assertThrows(IllegalArgumentException.class,
                () -> authentifizierungsmanager.authentifiziere(clientRegistration, credentials));
    }

    @Test
    public void testAuthWithNullClientRegistrationAndValidCredentials() {
        AdditionalCredentials credentials = AdditionalCredentials.createWithBhknz("900600");

        assertThrows(IllegalArgumentException.class,
                () -> authentifizierungsmanager.authentifiziere((String) null, credentials));
    }

    @Test
    public void testAuthWithClientRegistrationAndNullCredentials() {
        ClientRegistration clientRegistration = ClientRegistration.withRegistrationId("custom-cc-client")
                .tokenUri("http://localhost:9095/auth/realms/testrealm/protocol/openid-connect/token")
                .jwkSetUri("http://localhost:9095/auth/realms/testrealm/protocol/openid-connect/certs")
                .clientId("client-credentials-test-client")
                .clientSecret("supersecretpassword")
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .build();

        assertThrows(IllegalArgumentException.class,
                () -> authentifizierungsmanager.authentifiziere(clientRegistration, null));
    }

    @Test
    public void testAuthWithClientRegistrationCCAndBhknz() {
        authentifizierungsmanager.authentifiziere("cc-client-with-bhknz");

        ArgumentCaptor<ClientCredentialsRegistrationIdAuthenticationToken> tokenCaptor =
                ArgumentCaptor.forClass(ClientCredentialsRegistrationIdAuthenticationToken.class);
        verify(clientCredentialsAuthorizedClientAuthenticationProvider).authenticate(tokenCaptor.capture());
        assertEquals("cc-client-with-bhknz", tokenCaptor.getValue().getRegistrationId());
        assertEquals("123456", tokenCaptor.getValue().getBhknz());

        assertEquals(mockJwt, SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    public void testAuthRefreshesIfTokenIsNotOAuth2() {
        Authentication notOAuth2Authentication = new TestingAuthenticationToken("principal", "credentials");
        SecurityContextHolder.getContext().setAuthentication(notOAuth2Authentication);

        authentifizierungsmanager.authentifiziere("cc-client", Duration.ofSeconds(60));

        // refreshed because existing authentication got replaced with the mockJwt
        assertEquals(mockJwt, SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    public void testAuthRefreshesIfTokenIsExpired() {
        Instant expiresAt = Instant.now().plus(1, ChronoUnit.MINUTES);
        Jwt token = Mockito.mock(Jwt.class);
        when(token.getExpiresAt()).thenReturn(expiresAt);

        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(token));

        authentifizierungsmanager.authentifiziere("cc-client", Duration.ofSeconds(60));

        // refreshed because existing authentication got replaced with the mockJwt
        assertEquals(mockJwt, SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    public void testAuthDoesNotRefreshIfTokenIsOAuth2AndNotExpired() {
        Duration tokenExpiryDelta = Duration.ofSeconds(60);

        Instant expiresAt = Instant.now().plus(tokenExpiryDelta).plusSeconds(1);
        Jwt token = Mockito.mock(Jwt.class);
        when(token.getExpiresAt()).thenReturn(expiresAt);
        JwtAuthenticationToken authentication = new JwtAuthenticationToken(token);

        SecurityContextHolder.getContext().setAuthentication(authentication);

        authentifizierungsmanager.authentifiziere("cc-client", tokenExpiryDelta);

        // not refreshed because authentication stays the same
        assertEquals(authentication, SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    public void testAuthViaRegistrationIdFailsIfUnknown() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> authentifizierungsmanager.authentifiziere("unknown-client"));

        assertThat(exception).hasMessageContaining("unknown-client", "not find");
    }

    @Test
    public void testAuthRegistrationIdFailsIfUnsupportedGrantType() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> authentifizierungsmanager.authentifiziere("unsupported-client"));

        assertThat(exception).hasMessageContainingAll("unsupported-grant-type", "not supported");
    }

    @Test
    public void testNoCachingForClientCredentialsRegistrationId() {
        authentifizierungsmanager.authentifiziere("cc-client");
        verify(clientCredentialsAuthorizedClientAuthenticationProvider, times(1)).authenticate(any());

        SecurityContextHolder.clearContext();
        authentifizierungsmanager.authentifiziere("cc-client");
        verify(clientCredentialsAuthorizedClientAuthenticationProvider, times(2)).authenticate(any());
    }

    @Test
    public void testCacheClearedAfterClearCache() {
        ClientRegistration reg = ClientRegistration.withRegistrationId("testid")
            .issuerUri(getIssuer())
            .clientId("testid")
            .clientSecret("testsecret")
            .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
            .tokenUri("http://localhost/token")
            .build();

        authentifizierungsmanager.authentifiziere(reg);
        verify(clientCredentialsClientRegistrationAuthenticationProvider, times(1)).authenticate(any());

        authentifizierungsmanager.clearCache();
        SecurityContextHolder.clearContext();
        authentifizierungsmanager.authentifiziere(reg);

        verify(clientCredentialsClientRegistrationAuthenticationProvider, times(2)).authenticate(any());
    }

    @AfterEach
    public void tearDown() {
        // clear context and cache so no other tests are affected
        authentifizierungsmanager.clearCache();
        SecurityContextHolder.clearContext();
    }
}