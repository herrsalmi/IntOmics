package org.pmoi.business;

import org.junit.jupiter.api.Test;
import org.pmoi.model.Gene;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NCBIQueryClientTest {

    NCBIQueryClient ncbiQueryClient = new NCBIQueryClient();

    @org.junit.jupiter.api.Test
    void geneNameToEntrezID() {
    }

    @org.junit.jupiter.api.Test
    void entrezIDToGeneName() {
        // 1
        Gene gene = new Gene("", "1278");
        ncbiQueryClient.entrezIDToGeneName(gene);
        assertEquals("COL1A2", gene.getName());
        //2
        gene.setEntrezID("2335");
        ncbiQueryClient.entrezIDToGeneName(gene);
        assertEquals("FN1", gene.getName());
        //3
        gene.setEntrezID("7057");
        ncbiQueryClient.entrezIDToGeneName(gene);
        assertEquals("THBS1", gene.getName());
        //4
        gene.setEntrezID("3857");
        ncbiQueryClient.entrezIDToGeneName(gene);
        assertEquals("KRT9", gene.getName());
        //5
        gene.setEntrezID("71");
        ncbiQueryClient.entrezIDToGeneName(gene);
        assertEquals("ACTG1", gene.getName());
    }

    @Test
    void fetchDescription() {
        assertEquals("leptin receptor", ncbiQueryClient.fetchDescription("3953"));
    }
}