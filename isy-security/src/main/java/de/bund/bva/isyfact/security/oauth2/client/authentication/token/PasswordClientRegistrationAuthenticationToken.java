package de.bund.bva.isyfact.security.oauth2.client.authentication.token;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;

import org.springframework.lang.Nullable;
import org.springframework.security.oauth2.client.registration.ClientRegistration;

/**
 * AuthenticationToken holding parameters required for creating a Client to use with Resource Owner Password Credentials Flow authentication.
 */
public class PasswordClientRegistrationAuthenticationToken extends AbstractClientRegistrationAuthenticationToken {

    /** The resource owner's username. */
    private final String username;

    /** The resource owner's password. */
    @Nullable
    private String password;

    /** Indicates whether {@link #eraseCredentials()} has already been called. */
    private boolean credentialsErased;

    public PasswordClientRegistrationAuthenticationToken(ClientRegistration clientRegistration, String username, String password, @Nullable String bhknz) {
        super(username, clientRegistration, bhknz);
        this.username = username;
        this.password = password;
        setAuthenticated(false);
    }

    public String getUsername() {
        return username;
    }

    /**
     * Returns the resource owner password.
     *
     * @return the resource owner password.
     * @throws IllegalStateException if the credentials of this token have already been erased
     */
    public String getPassword() {
        if (credentialsErased) {
            throw new IllegalStateException(
                    "The credentials of this token have already been erased. A token can only be used for a single authentication.");
        }
        return password;
    }

    /**
     * Removes the reference to the resource owner's password, so that it is no longer reachable through this token.
     */
    @Override
    public void eraseCredentials() {
        super.eraseCredentials();
        this.password = null;
        this.credentialsErased = true;
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
     *     <li>username</li>
     *     <li>password</li>
     * </ul>
     *
     * @return the generated cache key as hash code or null
     */
    @Override
    public byte[] generateCacheKey(String hashAlgorithm, byte[] salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance(hashAlgorithm);

            digest.update(super.generateCacheKey(hashAlgorithm, salt));

            List<String> additionalValues = Arrays.asList(
                getUsername(),
                getPassword()
            );
            updateDigest(digest, additionalValues);

            return digest.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(hashAlgorithm + " nicht verfügbar.", e);
        }
    }
}
