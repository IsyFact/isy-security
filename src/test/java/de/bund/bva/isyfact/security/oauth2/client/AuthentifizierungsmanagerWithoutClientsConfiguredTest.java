package de.bund.bva.isyfact.security.oauth2.client;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.ProviderNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import de.bund.bva.isyfact.security.oauth2.client.authentication.ManualClientCredentialsAuthenticationProvider;
import de.bund.bva.isyfact.security.oauth2.client.authentication.ManualClientCredentialsAuthenticationToken;
import de.bund.bva.isyfact.security.oauth2.client.authentication.PasswordAuthenticationToken;

@SpringBootTest
public class AuthentifizierungsmanagerWithoutClientsConfiguredTest {

    @MockBean
    private ManualClientCredentialsAuthenticationProvider manualClientCredentialsAuthenticationProvider;

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

        when(manualClientCredentialsAuthenticationProvider.supports(any())).thenCallRealMethod();
        when(manualClientCredentialsAuthenticationProvider.authenticate(any(Authentication.class))).thenReturn(mockJwt);
    }

    @Test
    public void testHasOnlyManualClientCredentialsProvider() {
        assertThat(isyOAuth2AuthenticationProviderManager.getProviders()).containsOnly(manualClientCredentialsAuthenticationProvider);
    }

    @Test
    public void testAuthWithRegistrationIdThrowsException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> authentifizierungsmanager.authentifiziere("testclient"));

        assertThat(exception).hasMessageContaining("testclient");
    }

    @Test
    public void testAuthClientWithCredentials() {
        authentifizierungsmanager.authentifiziereClient("testid", "testsecret", "http://test/");

        ArgumentCaptor<ManualClientCredentialsAuthenticationToken> tokenCaptor =
                ArgumentCaptor.forClass(ManualClientCredentialsAuthenticationToken.class);
        verify(manualClientCredentialsAuthenticationProvider).authenticate(tokenCaptor.capture());
        assertEquals("testid", tokenCaptor.getValue().getClientId());
        assertEquals("testsecret", tokenCaptor.getValue().getClientSecret());
        assertEquals("http://test/", tokenCaptor.getValue().getIssuerLocation());

        assertEquals(mockJwt, SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    public void testAuthSystemWithRegistrationIdAndCredentialsThrowsException() {
        ProviderNotFoundException exception = assertThrows(ProviderNotFoundException.class,
                () -> authentifizierungsmanager.authentifiziereSystem("testclient", "test", "pw", null));

        assertThat(exception.getMessage()).contains(PasswordAuthenticationToken.class.getName());
    }

}
