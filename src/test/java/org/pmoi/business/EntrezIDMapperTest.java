package org.pmoi.business;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EntrezIDMapperTest {

    EntrezIDMapper mapper = EntrezIDMapper.getInstance();

    @Test
    void idToName() {
        assertEquals("ENG", mapper.idToName("2022"));
    }

    @Test
    void nameToId() {
        assertEquals("2022", mapper.nameToId("ENG"));
    }
}