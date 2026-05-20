package de.bund.bva.isyfact.security.config;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Builder class for additional authentication data.
 * Wraps a BHKNZ that can be used for authentication with an OAuth 2.0 client.
 */
public final class AdditionalCredentials {

    /** The BHKNZ to send as part of the authentication request. */
    @Nullable
    private final String bhknz;

    private AdditionalCredentials(@Nullable String bhknz) {
        this.bhknz = bhknz;
    }

    /**
     * Creates a builder-object for AdditionalCredentials only with BHKNZ.
     *
     * @param bhknz the BHKNZ to be sent as part of the authentication request
     * @return a new AdditionalCredentials instance
     */
    public static AdditionalCredentials createWithBhknz(String bhknz) {
        Assert.notNull(bhknz, "bhknz cannot be null");
        return new AdditionalCredentials(bhknz);
    }

    @Nullable
    public String getBhknz() {
        return bhknz;
    }

    /**
     * Checks whether the BHKNZ is set.
     *
     * @return true if BHKNZ is set, otherwise false
     */
    public boolean hasBhknz() {
        return bhknz != null;
    }
}
