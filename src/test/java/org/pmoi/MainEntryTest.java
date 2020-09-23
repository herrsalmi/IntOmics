package org.pmoi;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class MainEntryTest {

    @Test
    void mainKEGG() {
        var args = new String[]{"-p", "test/secretome_withNames_test.csv",
                "-g", "test/Gene_DE_9h.csv",
                "-a", "test/all_genes.csv",
                "-db", "KEGG",
                "-f", "FWF",
                "-fc", "1.5",
                "-pv", "0.05",
                "-s", "900",
                "-t", "8"};
        assertDoesNotThrow(() -> MainEntry.main(args));
    }

    @Test
    void mainWP() {
        var args = new String[]{"-p", "test/secretome_withNames_test.csv",
                "-g", "test/Gene_DE_9h.csv",
                "-a", "test/all_genes.csv",
                "-db", "WIKIPATHWAYS",
                "-f", "FWF",
                "-fc", "1.5",
                "-pv", "0.05",
                "-s", "900",
                "-t", "8"};
        assertDoesNotThrow(() -> MainEntry.main(args));
    }

    @Test
    void mainREACTOME() {
        var args = new String[]{"-p", "test/secretome_withNames_test.csv",
                "-g", "test/Gene_DE_9h.csv",
                "-a", "test/all_genes.csv",
                "-db", "REACTOME",
                "-f", "FWF",
                "-fc", "1.5",
                "-pv", "0.05",
                "-s", "900",
                "-t", "8"};
        assertDoesNotThrow(() -> MainEntry.main(args));
    }

    @Test
    void mainStringDB() {
        var args = new String[]{"-p", "test/secretome_withNames_test.csv",
                "-g", "test/Gene_DE_9h.csv",
                "-a", "test/all_genes.csv",
                "-db", "KEGG",
                "-f", "FWF",
                "-fc", "1.5",
                "-pv", "0.05",
                "-s", "900",
                "-t", "8",
                "--no-cached-ppi"};
        assertDoesNotThrow(() -> MainEntry.main(args));
    }

    @Test
    void mainNoCachedWP() {
        var args = new String[]{"-p", "test/secretome_withNames_test.csv",
                "-g", "test/Gene_DE_9h.csv",
                "-a", "test/all_genes.csv",
                "-db", "WIKIPATHWAYS",
                "-f", "FWF",
                "-fc", "1.5",
                "-pv", "0.05",
                "-s", "900",
                "-t", "8",
                "--no-cached-sets",
                "--ignore-check"};
        assertDoesNotThrow(() -> MainEntry.main(args));
    }

    @Test
    void mainNoCachedKEGG() {
        var args = new String[]{"-p", "test/secretome_withNames_test.csv",
                "-g", "test/Gene_DE_9h.csv",
                "-a", "test/all_genes.csv",
                "-db", "KEGG",
                "-f", "FWF",
                "-fc", "1.5",
                "-pv", "0.05",
                "-s", "900",
                "-t", "8",
                "--no-cached-sets",
                "--ignore-check"};
        assertDoesNotThrow(() -> MainEntry.main(args));
    }
}