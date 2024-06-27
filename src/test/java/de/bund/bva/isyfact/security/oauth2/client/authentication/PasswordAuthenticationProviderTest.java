package de.bund.bva.isyfact.security.oauth2.client.authentication;

import static de.bund.bva.isyfact.security.test.oidcprovider.EmbeddedOidcProviderStub.BHKNZ_CLAIM_NAME;
import static de.bund.bva.isyfact.security.test.oidcprovider.EmbeddedOidcProviderStub.DEFAULT_ROLES_CLAIM_NAME;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.ClientAuthorizationException;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.oidc.StandardClaimNames;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;

import de.bund.bva.isyfact.security.AbstractOidcProviderTest;

@ActiveProfiles("test-clients")
@SpringBootTest
public class PasswordAuthenticationProviderTest extends AbstractOidcProviderTest {

    @Autowired
    private PasswordAuthenticationProvider passwordAuthenticationProvider;

    @BeforeAll
    public static void setup() {
        registerTestClients();
        SecurityContextHolder.getContext().setAuthentication(null);
    }

    @Test
    public void shouldGetAuthTokenForUserWithoutBhknz() {
        Authentication authentication = passwordAuthenticationProvider.authenticate(
                new PasswordAuthenticationToken("ropc-client", "testuser", "pw1234", null));

        // security context is still empty
        SecurityContext securityContext = SecurityContextHolder.getContext();
        assertNull(securityContext.getAuthentication());

        assertInstanceOf(JwtAuthenticationToken.class, authentication);
        JwtAuthenticationToken jwtAuth = (JwtAuthenticationToken) authentication;
        assertEquals("testuser", jwtAuth.getTokenAttributes().get(StandardClaimNames.PREFERRED_USERNAME));
        List<String> grantedAuthorityNames = jwtAuth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
        assertThat(grantedAuthorityNames).containsOnly("PRIV_Recht_A");
        assertThat((List<String>) jwtAuth.getTokenAttributes().get(DEFAULT_ROLES_CLAIM_NAME)).containsOnly("Rolle_A");
        assertFalse(jwtAuth.getTokenAttributes().containsKey(BHKNZ_CLAIM_NAME));
    }

    @Test
    public void shouldGetAuthTokenForUserWithBhknz() {
        Authentication authentication = passwordAuthenticationProvider.authenticate(
                new PasswordAuthenticationToken("ropc-client", "testuser-with-bhknz", "pw1234", "123456"));

        // security context is still empty
        SecurityContext securityContext = SecurityContextHolder.getContext();
        assertNull(securityContext.getAuthentication());

        assertInstanceOf(JwtAuthenticationToken.class, authentication);
        JwtAuthenticationToken jwtAuth = (JwtAuthenticationToken) authentication;
        assertEquals("testuser-with-bhknz", jwtAuth.getTokenAttributes().get(StandardClaimNames.PREFERRED_USERNAME));
        List<String> grantedAuthorityNames = jwtAuth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
        assertThat(grantedAuthorityNames).containsOnly("PRIV_Recht_B");
        assertThat((List<String>) jwtAuth.getTokenAttributes().get(DEFAULT_ROLES_CLAIM_NAME)).containsOnly("Rolle_B");
        assertEquals("123456", jwtAuth.getTokenAttributes().get(BHKNZ_CLAIM_NAME));
    }

    @Test
    public void shouldAutomaticallyFetchCredentialsForClientWithoutBhknz() {
        Authentication authentication = passwordAuthenticationProvider.authenticate(new PasswordAuthenticationToken("ropc-client"));

        // security context is still empty
        SecurityContext securityContext = SecurityContextHolder.getContext();
        assertNull(securityContext.getAuthentication());

        assertInstanceOf(JwtAuthenticationToken.class, authentication);
        JwtAuthenticationToken jwtAuth = (JwtAuthenticationToken) authentication;
        assertEquals("testuser", jwtAuth.getTokenAttributes().get(StandardClaimNames.PREFERRED_USERNAME));
        List<String> grantedAuthorityNames = jwtAuth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
        assertThat(grantedAuthorityNames).containsOnly("PRIV_Recht_A");
        assertThat((List<String>) jwtAuth.getTokenAttributes().get(DEFAULT_ROLES_CLAIM_NAME)).containsOnly("Rolle_A");
        assertFalse(jwtAuth.getTokenAttributes().containsKey(BHKNZ_CLAIM_NAME));
    }

    @Test
    public void shouldAutomaticallyFetchCredentialsForClientWithBhknz() {
        // for automatic fetching we a registrationId associated with a bhknz
        // if explicitly provided "ropc-client" would work as well for users with bhknz (see other tests)
        Authentication authentication = passwordAuthenticationProvider.authenticate(
                new PasswordAuthenticationToken("ropc-client-with-bhknz"));

        // security context is still empty
        SecurityContext securityContext = SecurityContextHolder.getContext();
        assertNull(securityContext.getAuthentication());

        assertInstanceOf(JwtAuthenticationToken.class, authentication);
        JwtAuthenticationToken jwtAuth = (JwtAuthenticationToken) authentication;
        assertEquals("testuser-with-bhknz", jwtAuth.getTokenAttributes().get(StandardClaimNames.PREFERRED_USERNAME));
        List<String> grantedAuthorityNames = jwtAuth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
        assertThat(grantedAuthorityNames).containsOnly("PRIV_Recht_B");
        assertThat((List<String>) jwtAuth.getTokenAttributes().get(DEFAULT_ROLES_CLAIM_NAME)).containsOnly("Rolle_B");
        assertEquals("123456", jwtAuth.getTokenAttributes().get(BHKNZ_CLAIM_NAME));
    }

    @Test
    public void shouldThrowAuthExceptionWithInvalidCredentials() {
        assertThrows(ClientAuthorizationException.class,
                () -> passwordAuthenticationProvider.authenticate(
                        new PasswordAuthenticationToken("ropc-client", "testuser", "wrong", null)));
    }

    @Test
    public void shouldThrowErrorWithWrongClient() {
        ClientAuthorizationException exception = assertThrows(ClientAuthorizationException.class,
                () -> passwordAuthenticationProvider.authenticate(
                        new PasswordAuthenticationToken("cc-client", "testuser", "pw1234", null)));

        assertEquals(OAuth2ErrorCodes.INVALID_GRANT, exception.getError().getErrorCode());
        assertEquals("cc-client", exception.getClientRegistrationId());
    }

    @Test
    public void shouldThrowErrorWithMissingBhknz() {
        // user that requires a bhknz
        ClientAuthorizationException exception = assertThrows(ClientAuthorizationException.class,
                () -> passwordAuthenticationProvider.authenticate(
                        new PasswordAuthenticationToken("ropc-client", "testuser-with-bhknz", "pw1234", null)));

        assertEquals("invalid_token_response", exception.getError().getErrorCode());
        assertEquals("ropc-client", exception.getClientRegistrationId());
    }

    @Test
    public void shouldReturnNullWhenPassingUnsupportedAuthentication() {
        Authentication authRequest = new UsernamePasswordAuthenticationToken("testuser", "pw1234");
        Authentication authentication = passwordAuthenticationProvider.authenticate(authRequest);

        assertNull(authentication);
    }

}
