package org.pmoi;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class MainEntryTest {

    @Test
    void main() {
        var args = new String[]{"-p", "test/secretome_withNames_test.csv",
                "-g", "test/Gene_DE_9h.csv",
                "-a", "test/all_genes.csv",
                "-db", "KEGG",
                "-f", "FWF",
                "-fc", "1.5",
                "-s", "900",
                "-t", "8"};
        assertDoesNotThrow(() -> MainEntry.main(args));
    }
}