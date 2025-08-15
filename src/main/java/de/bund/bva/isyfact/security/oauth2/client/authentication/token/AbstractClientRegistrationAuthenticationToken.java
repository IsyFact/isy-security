package de.bund.bva.isyfact.security.oauth2.client.authentication.token;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Objects;

import org.springframework.lang.Nullable;
import org.springframework.security.oauth2.client.registration.ClientRegistration;

/**
 * Token that holds a {@link ClientRegistration}.
 */
public abstract class AbstractClientRegistrationAuthenticationToken extends AbstractIsyAuthenticationToken {

    /** Client Registration of the OAuth 2.0 client. */
    private final ClientRegistration clientRegistration;

    /** Salt to ensure safe hash code */
    private final byte[] salt;

    protected AbstractClientRegistrationAuthenticationToken(String principal, ClientRegistration clientRegistration, @Nullable String bhknz) {
        super(principal, bhknz);
        this.clientRegistration = clientRegistration;
        setAuthenticated(false);
        salt = new byte[16];
        new SecureRandom().nextBytes(salt);
    }

    public ClientRegistration getClientRegistration() {
        return clientRegistration;
    }

    /**
     * Generates a cache key that includes the following fields.
     * <ul>
     *     <li>principal</li>
     *     <li>bhknz</li>
     *     <li>issuerLocation</li>
     *     <li>clientId</li>
     *     <li>clientSecret</li>
     *     <li>authorizationGrantType</li>
     * </ul>
     *
     * @return the generated cache key as hash code or null
     */
    @Override
    public byte[] generateCacheKey() {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-512");
            digest.update(salt);

            ClientRegistration clientReg = getClientRegistration();
            String principal = getPrincipal().toString();
            String bhknz = getBhknz() != null ? getBhknz() : "";
            String issuerUri = clientReg.getProviderDetails().getIssuerUri();
            String clientId = clientReg.getClientId();
            String clientSecret = clientReg.getClientSecret();
            String authorizationGrantType = clientReg.getAuthorizationGrantType().toString();

            digest.update(principal.getBytes());
            digest.update(bhknz.getBytes());
            digest.update(issuerUri.getBytes());
            digest.update(clientId.getBytes());
            digest.update(clientSecret.getBytes());
            digest.update(authorizationGrantType.getBytes());

            return digest.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-512 nicht verfügbar.", e);
        }
    }
}
