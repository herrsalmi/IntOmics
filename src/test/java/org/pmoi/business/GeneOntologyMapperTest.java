package org.pmoi.business;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GeneOntologyMapperTest {

    @Test
    void checkSecretomeGO() {
        GeneOntologyMapper mapper = new GeneOntologyMapper();
        assertTrue(mapper.checkSecretomeGO("3481"));
    }
}