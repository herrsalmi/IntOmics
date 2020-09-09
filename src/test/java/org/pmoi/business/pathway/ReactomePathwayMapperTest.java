package org.pmoi.business.pathway;

import org.junit.jupiter.api.Test;
import org.pmoi.model.PathwayMode;

import static org.junit.jupiter.api.Assertions.assertEquals;


class ReactomePathwayMapperTest {

    @Test
    void search() {
        PathwayMapper pm = PathwayMapperFactory.getPathwayMapper(PathwayMode.REACTOME);
        assertEquals("Signaling by Leptin", pm.getPathways("LEP").get(0).getName());
    }
}