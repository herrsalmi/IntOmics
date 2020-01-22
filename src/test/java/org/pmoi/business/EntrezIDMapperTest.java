package org.pmoi.business;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EntrezIDMapperTest {

    EntrezIDMapper mapper = EntrezIDMapper.getInstance();

    @Test
    void idToName() {
        assertEquals("TF", mapper.idToName("7018"));
        assertEquals("AHSG", mapper.idToName("197"));
        assertEquals("SDCBP", mapper.idToName("6386"));
        assertEquals("TGFBR3", mapper.idToName("7049"));
        assertEquals("TNFAIP6", mapper.idToName("7130"));
        assertEquals("CFD", mapper.idToName("1675"));
        assertEquals("INHBB", mapper.idToName("3625"));
        assertEquals("SEMA3G", mapper.idToName("56920"));
        assertEquals("CILP", mapper.idToName("8483"));
        assertEquals("AGT", mapper.idToName("183"));
    }

    @Test
    void nameToId() {
        assertEquals("7018", mapper.nameToId("TF"));
        assertEquals("56005", mapper.nameToId("C19orf10"));
        assertEquals("3084", mapper.nameToId("NRG1"));
        assertEquals("80781", mapper.nameToId("COL18A1"));
        assertEquals("4485", mapper.nameToId("MST1"));
        assertEquals("2990", mapper.nameToId("GUSB"));
    }
}