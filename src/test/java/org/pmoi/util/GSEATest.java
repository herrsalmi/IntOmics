package org.pmoi.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pmoi.business.PathwayClient;
import org.pmoi.business.TranscriptomeManager;
import org.pmoi.model.Gene;

import java.util.List;

class GSEATest {

    private GSEA gsea;
    private List<Gene> geneSet;
    private List<Gene> geneList;
    @BeforeEach
    void setUp() {
        gsea = new GSEA();
        TranscriptomeManager transcriptomeManager = TranscriptomeManager.getInstance();
        geneList = transcriptomeManager.getDEGenes("test/Gene_DE_9h.csv");
        PathwayClient pathwayClient = PathwayClient.getInstance();
        //geneList = transcriptome.parallelStream().filter(e -> pathwayClient.isInAnyPathway(e.getName())).collect(Collectors.toList());
        geneSet = pathwayClient.getKEGGPathwayGenes("map04350");
    }

    @Test
    void run() {
        System.out.println(gsea.run(geneSet, geneList));
        System.out.println(gsea.getNormalizedScore());
    }
}