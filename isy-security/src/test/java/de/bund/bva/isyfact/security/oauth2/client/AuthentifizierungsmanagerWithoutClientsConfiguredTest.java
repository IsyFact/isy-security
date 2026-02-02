package de.bund.bva.isyfact.security.oauth2.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import de.bund.bva.isyfact.security.AbstractOidcProviderTest;
import de.bund.bva.isyfact.security.config.AdditionalCredentials;
import de.bund.bva.isyfact.security.oauth2.client.authentication.ClientCredentialsClientRegistrationAuthenticationProvider;
import de.bund.bva.isyfact.security.oauth2.client.authentication.token.ClientCredentialsClientRegistrationAuthenticationToken;

@SpringBootTest
public class AuthentifizierungsmanagerWithoutClientsConfiguredTest extends AbstractOidcProviderTest {

    @MockitoBean
    private ClientCredentialsClientRegistrationAuthenticationProvider clientCredentialsClientRegistrationAuthenticationProvider;

    @Autowired
    private Authentifizierungsmanager authentifizierungsmanager;

    @Autowired
    private ProviderManager isyOAuth2AuthenticationProviderManager;

    private JwtAuthenticationToken mockJwt;

    @BeforeEach
    public void configureMocks() {
        // clear authenticated principal
        SecurityContextHolder.getContext().setAuthentication(null);

        mockJwt = mock(JwtAuthenticationToken.class);

        when(clientCredentialsClientRegistrationAuthenticationProvider.supports(any())).thenCallRealMethod();
        when(clientCredentialsClientRegistrationAuthenticationProvider.authenticate(any(Authentication.class))).thenReturn(mockJwt);
    }

    @Test
    public void testAuthWithRegistrationIdThrowsException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> authentifizierungsmanager.authentifiziere("testclient"));

        assertThat(exception).hasMessageContaining("testclient");
    }

    @Test
    public void testAuthWithDirectClientRegistration() {
        ClientRegistration clientRegistration = ClientRegistration.withRegistrationId("custom-cc-client")
                .tokenUri("http://localhost:9095/auth/realms/testrealm/protocol/openid-connect/token")
                .jwkSetUri("http://localhost:9095/auth/realms/testrealm/protocol/openid-connect/certs")
                .clientId("testid")
                .clientSecret("testsecret")
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .build();

        authentifizierungsmanager.authentifiziere(clientRegistration);

        ArgumentCaptor<ClientCredentialsClientRegistrationAuthenticationToken> tokenCaptor =
                ArgumentCaptor.forClass(ClientCredentialsClientRegistrationAuthenticationToken.class);
        verify(clientCredentialsClientRegistrationAuthenticationProvider).authenticate(tokenCaptor.capture());
        ClientCredentialsClientRegistrationAuthenticationToken value = tokenCaptor.getValue();
        assertEquals("custom-cc-client", value.getClientRegistration().getRegistrationId());
        assertEquals("testid", value.getClientRegistration().getClientId());
        assertEquals("testsecret", value.getClientRegistration().getClientSecret());
        assertNull(value.getBhknz());

        assertEquals(mockJwt, SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    public void testAuthWithGranttypePasswordNotSupported() {
        ClientRegistration clientRegistration = ClientRegistration.withRegistrationId("custom-ropc-client")
                .tokenUri("http://localhost:9095/auth/realms/testrealm/protocol/openid-connect/token")
                .jwkSetUri("http://localhost:9095/auth/realms/testrealm/protocol/openid-connect/certs")
                .clientId("testid")
                .clientSecret("testsecret")
                .authorizationGrantType(new AuthorizationGrantType("password"))
                .build();

        AdditionalCredentials additionalCredentials = AdditionalCredentials.createWithUsernamePasswordBhknz(
                "newUser", "newPassword", "900600");

        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> authentifizierungsmanager.authentifiziere(clientRegistration, additionalCredentials));

        assertEquals(illegalArgumentException.getMessage(), "The AuthorizationGrantType 'password' is not supported.");
    }
}
