package org.pmoi.database;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GeneMapperTest {

    private GeneMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = GeneMapper.getInstance();
    }

    @Test
    void getSymbolFromAlias() {
        assertEquals("GP6", mapper.getSymbolFromAlias("GPIV").get());
    }
}