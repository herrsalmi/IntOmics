package org.pmoi.database;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GeneMapperTest {

    private GeneMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = GeneMapper.getInstance();
    }

    @Test
    void getSymbolFromAlias() {
        System.out.println(mapper.getSymbolFromAlias("GPIV"));

    }
}