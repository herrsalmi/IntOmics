package org.pmoi.business;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PathwayClientTest {

    private PathwayClient pathwayClient;

    @BeforeEach
    private void init() {
        pathwayClient = PathwayClient.getInstance();
    }

    @Test
    void getPathway() {
        assertEquals(21, pathwayClient.getPathways("HLA").size());
        //pathwayClient.close();
    }

    @Test
    void KEGGSearch() {
        assertEquals(2, pathwayClient.KEGGSearch("351").size());
        assertEquals("hsa04726  Serotonergic synapse", pathwayClient.KEGGSearch("351").get(0));
        assertEquals("hsa05010  Alzheimer disease", pathwayClient.KEGGSearch("351").get(1));

        assertEquals(0, pathwayClient.KEGGSearch("2022").size());

        pathwayClient.KEGGSearch("351").forEach(System.out::println);
    }

    @Test
    void getPathwayGenes() {
        assertEquals(294, pathwayClient.getKEGGPathwayGenes("hsa04010").size());
    }


    @Test
    void getPathwaysForGene() {
        assertNotEquals(pathwayClient.getPathwaysForGene("IGF2").size(), 0);
    }

    @Test
    void getNumberOfGenesByPathway() {
        assertNotEquals(pathwayClient.getNumberOfGenesByPathway("TGF-beta signaling pathway"), 0);
    }

//    @Test
//    void listKEGG() {
//        pathwayClient.listKEGG().forEach((k, v) -> System.out.println(k + " : " + v));
//        assertEquals(pathwayClient.listKEGG().size(), 337);
//    }

    @Test
    void initKEGGPathways() {
        pathwayClient.initKEGGPathways();
        pathwayClient.getPathwaysForGene("LEP").forEach(System.out::println);
    }

    @Test
    void initWikiPathways() {
        pathwayClient.initWikiPathways();
        pathwayClient.getPathwaysForGene("LEP").forEach(System.out::println);
    }

    @Test
    void isInAnyPathway() {
        assertTrue(pathwayClient.isInAnyPathway("LEP"));
    }
}