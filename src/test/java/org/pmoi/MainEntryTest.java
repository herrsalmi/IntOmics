package org.pmoi;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class MainEntryTest {

    @Test
    void mainDemo() {
        var args = new String[]{
                "-p", "sample/secreted.csv",
                "-g", "sample/de_testing.csv",
                "-db", "WIKIPATHWAYS",
                "-f", "fwf",
                "-fc", "1.5",
                "-pv", "0.05",
                "-gpv", "0.1",
                "-s", "900",
                "-t", "4",
                "--species", "human"
        };
        assertDoesNotThrow(() -> MainEntry.main(args));
    }

    @Test
    void mainKEGG() {
        var args = new String[]{
                "-p", "sample/secreted.csv",
                "-g", "sample/de_testing.csv",
                "-db", "KEGG",
                "-f", "FWF",
                "-fc", "1.5",
                "-pv", "0.05",
                "-s", "900",
                "-t", "8"
        };
        assertDoesNotThrow(() -> MainEntry.main(args));
    }

    @Test
    void mainWP() {
        var args = new String[]{
                "-p", "sample/secreted.csv",
                "-g", "sample/de_testing.csv",
                "-db", "WIKIPATHWAYS",
                "-f", "FWF",
                "-fc", "1.5",
                "-pv", "0.05",
                "-s", "900",
                "-t", "8"
        };
        assertDoesNotThrow(() -> MainEntry.main(args));
    }

    @Test
    void mainREACTOME() {
        var args = new String[]{
                "-p", "sample/secreted.csv",
                "-g", "sample/de_testing.csv",
                "-db", "REACTOME",
                "-f", "FWF",
                "-fc", "1.5",
                "-pv", "0.05",
                "-s", "900",
                "-t", "8"
        };
        assertDoesNotThrow(() -> MainEntry.main(args));
    }

    @Test
    void mainStringDB() {
        var args = new String[]{
                "-p", "sample/secreted.csv",
                "-g", "sample/de_testing.csv",
                "-db", "KEGG",
                "-f", "FWF",
                "-fc", "1.5",
                "-pv", "0.05",
                "-s", "900",
                "-t", "8",
                "--no-cached-ppi"
        };
        assertDoesNotThrow(() -> MainEntry.main(args));
    }

    @Test
    void mainNoCachedWP() {
        var args = new String[]{
                "-p", "sample/secreted.csv",
                "-g", "sample/de_testing.csv",
                "-db", "WIKIPATHWAYS",
                "-f", "FWF",
                "-fc", "1.5",
                "-pv", "0.05",
                "-s", "900",
                "-t", "8",
                "--no-cached-sets",
                "--ignore-check"
        };
        assertDoesNotThrow(() -> MainEntry.main(args));
    }

    @Test
    void mainNoCachedKEGG() {
        var args = new String[]{
                "-p", "sample/secreted.csv",
                "-g", "sample/de_testing.csv",
                "-db", "KEGG",
                "-f", "FWF",
                "-fc", "1.5",
                "-pv", "0.05",
                "-s", "900",
                "-t", "8",
                "--no-cached-sets",
                "--ignore-check"
        };
        assertDoesNotThrow(() -> MainEntry.main(args));
    }
}