package org.pmoi.business;

import org.pmoi.models.Gene;

import static org.junit.jupiter.api.Assertions.*;

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
        assertEquals("COL1A2", gene.getGeneName());
        //2
        gene.setGeneEntrezID("2335");
        ncbiQueryClient.entrezIDToGeneName(gene);
        assertEquals("FN1", gene.getGeneName());
        //3
        gene.setGeneEntrezID("7057");
        ncbiQueryClient.entrezIDToGeneName(gene);
        assertEquals("THBS1", gene.getGeneName());
        //4
        gene.setGeneEntrezID("3857");
        ncbiQueryClient.entrezIDToGeneName(gene);
        assertEquals("KRT9", gene.getGeneName());
        //5
        gene.setGeneEntrezID("71");
        ncbiQueryClient.entrezIDToGeneName(gene);
        assertEquals("ACTG1", gene.getGeneName());
    }
}