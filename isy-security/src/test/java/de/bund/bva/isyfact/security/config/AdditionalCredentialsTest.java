package de.bund.bva.isyfact.security.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class AdditionalCredentialsTest {

    private static final String TEST_BHKNZ = "900600";

    @Test
    public void testWithBhknz() {
        AdditionalCredentials credentials = AdditionalCredentials.createWithBhknz(TEST_BHKNZ);

        assertEquals(TEST_BHKNZ, credentials.getBhknz());
        assertTrue(credentials.hasBhknz());
    }

    @Test
    public void testWithBhknzNullBhknz() {
        assertThrows(IllegalArgumentException.class,
                () -> AdditionalCredentials.createWithBhknz(null));
    }
}
