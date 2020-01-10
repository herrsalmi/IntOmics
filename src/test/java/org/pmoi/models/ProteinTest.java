package org.pmoi.models;

import org.junit.jupiter.api.Test;

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

}