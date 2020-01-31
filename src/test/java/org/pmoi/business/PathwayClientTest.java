package org.pmoi.business;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PathwayClientTest {

    @Test
    void getPathway() {
        PathwayClient pathwayClient = new PathwayClient();
        pathwayClient.getPathways("IGF2").forEach(System.out::println);
    }

    @Test
    void KEGGSearch() {
        PathwayClient pathwayClient = new PathwayClient();
        assertEquals(2, pathwayClient.KEGGSearch("351").size());
        assertEquals("hsa04726  Serotonergic synapse", pathwayClient.KEGGSearch("351").get(0));
        assertEquals("hsa05010  Alzheimer disease", pathwayClient.KEGGSearch("351").get(1));

        assertEquals(0, pathwayClient.KEGGSearch("2022").size());

        pathwayClient.KEGGSearch("351").forEach(System.out::println);
    }

    @Test
    void getPathwayGenes() {
        PathwayClient pathwayClient = new PathwayClient();
        assertEquals(295, pathwayClient.getKEGGPathwayGenes("hsa04010").size());
    }

    @Test
    void getIntercatorsFromPathway() {
        PathwayClient pathwayClient = new PathwayClient();
        var res = pathwayClient.getIntercatorsFromPathway("TGFBR3");
        res.forEach(System.out::println);
    }

    @Test
    void getPathwaysForGene() {
        PathwayClient pathwayClient = new PathwayClient();
        pathwayClient.getPathwaysForGene("IGF2").forEach(System.out::println);
    }
}