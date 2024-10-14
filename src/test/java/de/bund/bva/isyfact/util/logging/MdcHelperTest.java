package de.bund.bva.isyfact.util.logging;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testfälle des MDC-Helper.
 */
public class MdcHelperTest {

    /**
     * Testet des Setzen der Korrelations-ID im MDC.
     */
    @Test
    public void testKorrelationsId() {

        // Korrelations-ID leeren
        MDC.remove(MdcHelper.MDC_KORRELATIONS_ID);

        // Korrelations-ID ist bisher nicht gesetzt und muss daher beim Entfernen und Lesen null sein.
        String korrelationsid = MdcHelper.entferneKorrelationsId();
        assertNull(korrelationsid);

        korrelationsid = MdcHelper.liesKorrelationsId();
        assertNull(korrelationsid);

        // Korrelations-ID "1" ergänzen, lesen und entfernen
        MdcHelper.pushKorrelationsId("1");
        korrelationsid = MdcHelper.liesKorrelationsId();
        assertEquals("1", korrelationsid);

        korrelationsid = MdcHelper.entferneKorrelationsId();
        assertEquals("1", korrelationsid);

        korrelationsid = MdcHelper.liesKorrelationsId();
        assertNull(korrelationsid);

        korrelationsid = MdcHelper.entferneKorrelationsId();
        assertNull(korrelationsid);

        // Korrelations-ID "1", "2" und "3" ergänzen, lesen und entfernen
        MdcHelper.pushKorrelationsId("1");
        korrelationsid = MdcHelper.liesKorrelationsId();
        assertEquals("1", korrelationsid);

        MdcHelper.pushKorrelationsId("2");
        korrelationsid = MdcHelper.liesKorrelationsId();
        assertEquals("1;2", korrelationsid);

        MdcHelper.pushKorrelationsId("3");
        korrelationsid = MdcHelper.liesKorrelationsId();
        assertEquals("1;2;3", korrelationsid);

        korrelationsid = MdcHelper.entferneKorrelationsId();
        assertEquals("3", korrelationsid);

        korrelationsid = MdcHelper.liesKorrelationsId();
        assertEquals("1;2", korrelationsid);

        korrelationsid = MdcHelper.entferneKorrelationsId();
        assertEquals("2", korrelationsid);

        korrelationsid = MdcHelper.liesKorrelationsId();
        assertEquals("1", korrelationsid);

        korrelationsid = MdcHelper.entferneKorrelationsId();
        assertEquals("1", korrelationsid);

        korrelationsid = MdcHelper.liesKorrelationsId();
        assertNull(korrelationsid);

        korrelationsid = MdcHelper.entferneKorrelationsId();
        assertNull(korrelationsid);


    }

    /**
     * Testet das Entfernen aller Korrelations-Ids mit dem MDC-Helper.
     */
    @Test
    public void testKorrelationsIdAlleEntfernen() {

        String korrelationsid;

        // Korrelations-ID leeren
        MDC.remove(MdcHelper.MDC_KORRELATIONS_ID);

        // Korrelations-ID "1", "2" und "3" ergänzen, lesen und entfernen
        MdcHelper.pushKorrelationsId("1");
        korrelationsid = MdcHelper.liesKorrelationsId();
        assertEquals("1", korrelationsid);

        MdcHelper.pushKorrelationsId("2");
        korrelationsid = MdcHelper.liesKorrelationsId();
        assertEquals("1;2", korrelationsid);

        MdcHelper.pushKorrelationsId("3");
        korrelationsid = MdcHelper.liesKorrelationsId();
        assertEquals("1;2;3", korrelationsid);

        // Mehrfach alle Ids entfernen

        MdcHelper.entferneKorrelationsIds();
        korrelationsid = MdcHelper.liesKorrelationsId();
        assertNull(korrelationsid);

        MdcHelper.entferneKorrelationsIds();
        korrelationsid = MdcHelper.liesKorrelationsId();
        assertNull(korrelationsid);

        MdcHelper.entferneKorrelationsIds();
        korrelationsid = MdcHelper.liesKorrelationsId();
        assertNull(korrelationsid);

        // Erneut setzen: Korrelations-ID "1", "2" und "3" ergänzen, lesen und entfernen
        MdcHelper.pushKorrelationsId("1");
        korrelationsid = MdcHelper.liesKorrelationsId();
        assertEquals("1", korrelationsid);

        MdcHelper.pushKorrelationsId("2");
        korrelationsid = MdcHelper.liesKorrelationsId();
        assertEquals("1;2", korrelationsid);

        MdcHelper.pushKorrelationsId("3");
        korrelationsid = MdcHelper.liesKorrelationsId();
        assertEquals("1;2;3", korrelationsid);
    }

    /**
     * Testet das Setzen und Auslesen des MDC-Fachdaten flags.
     */
    @Test
    public void testMdcFachdaten() {
        MdcHelper.setzeMarkerFachdaten(true);
        assertTrue(MdcHelper.liesMarkerFachdaten());

        MdcHelper.setzeMarkerFachdaten(false);
        assertFalse(MdcHelper.liesMarkerFachdaten());

        MdcHelper.entferneMarkerFachdaten();
        assertNull(MDC.get(MdcHelper.MDC_FACHDATEN));
    }

}
