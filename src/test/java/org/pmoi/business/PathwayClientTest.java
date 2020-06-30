package org.pmoi.business;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PathwayClientTest {

    @Test
    void getPathway() {
        PathwayClient pathwayClient = PathwayClient.getInstance();
        assertEquals(21, pathwayClient.getPathways("HLA").size());
        //pathwayClient.close();
    }

    @Test
    void KEGGSearch() {
        PathwayClient pathwayClient = PathwayClient.getInstance();
        assertEquals(2, pathwayClient.KEGGSearch("351").size());
        assertEquals("hsa04726  Serotonergic synapse", pathwayClient.KEGGSearch("351").get(0));
        assertEquals("hsa05010  Alzheimer disease", pathwayClient.KEGGSearch("351").get(1));

        assertEquals(0, pathwayClient.KEGGSearch("2022").size());

        pathwayClient.KEGGSearch("351").forEach(System.out::println);
    }

    @Test
    void getPathwayGenes() {
        PathwayClient pathwayClient = PathwayClient.getInstance();
        assertEquals(294, pathwayClient.getKEGGPathwayGenes("hsa04010").size());
    }


    @Test
    void getPathwaysForGene() {
        PathwayClient pathwayClient = PathwayClient.getInstance();
        assertTrue(pathwayClient.getPathwaysForGene("IGF2").size() != 0);
    }

    @Test
    void getNumberOfGenesByPathway() {
        PathwayClient pathwayClient = PathwayClient.getInstance();
        assertTrue(pathwayClient.getNumberOfGenesByPathway("TGF-beta signaling pathway") != 0);
    }
}