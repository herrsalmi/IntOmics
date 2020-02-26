package org.pmoi.models;

import org.junit.jupiter.api.Test;
import org.pmoi.business.StringdbQueryClient;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ProteinTest {

    @Test
    void objectConstruction() {
        Protein protein = new Protein("226;591,57;872,93;747,51;782,73;2086,79;1902,48");
        assertEquals(737.33, protein.depletedMeanScore());
        assertEquals(1590.66, protein.rinsedMeanScore());

        protein = new Protein("4048;191,6;;;144,69;59,41;187,2");
        assertEquals(191.6, protein.depletedMeanScore());
        assertEquals(130.43, protein.rinsedMeanScore());

        protein = new Protein("4048;;;;144,69;59,41;187,2");
        assertEquals(0, protein.depletedMeanScore());
        assertEquals(130.43, protein.rinsedMeanScore());

        protein = new Protein("4521;32,07;;;;;");
        assertEquals(32.07, protein.depletedMeanScore());
        assertEquals(0, protein.rinsedMeanScore());
    }

    @Test
    void comparaisonFilter() {
        Protein protein = new Protein("4048;191,6;;;144,69;59,41;187,2");
        assertFalse(protein.isMoreExpressedInDepletedSamples(1.3));

        protein = new Protein("4048;191,6;60;;144,69;59,41;187,2");
        assertFalse(protein.isMoreExpressedInDepletedSamples(1.3));

        protein = new Protein("4048;119;;102;132;57;32");
        assertFalse(protein.isMoreExpressedInDepletedSamples(1.3));

        protein = new Protein("4048;62;;;;;");
        assertTrue(protein.isMoreExpressedInDepletedSamples(1.3));
    }

    @Test
    void alternativeObjectConstruction() {
        Protein protein = new Protein("Mothers against decapentaplegic homolog 1 OS=Homo sapiens GN=SMAD1 PE=1 SV=1", 124.2, 62.4);
        StringdbQueryClient stringdbQueryClient = new StringdbQueryClient();
        Map<String, String> interactors = stringdbQueryClient.getProteinNetwork(protein.getName());
        assertEquals("SMAD1", protein.getName());
        interactors.forEach((k, v) -> System.out.println(k + " : " + v));
        assertEquals(63, interactors.size());
    }

}