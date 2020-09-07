package org.pmoi.business.pathway;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class ReactomePathwayMapperTest {

    @Test
    void listAll() {
        ReactomePathwayMapper rpm = new ReactomePathwayMapper();
        assertDoesNotThrow(rpm::listAll);
    }
}