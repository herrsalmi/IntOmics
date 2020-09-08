package org.pmoi.business.pathway;

import org.junit.jupiter.api.Test;
import org.pmoi.model.PathwayMode;

class ReactomePathwayMapperTest {

    @Test
    void search() {
        PathwayMapper pm = PathwayMapperFactory.getPathwayMapper(PathwayMode.REACTOME);
        System.out.println(pm.getPathways("LEP"));
    }
}