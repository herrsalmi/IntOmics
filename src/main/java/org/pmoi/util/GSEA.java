package org.pmoi.util;

import org.pmoi.model.Gene;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;

public class GSEA {

    private double normalizedScore;
    private static final int PERMUTATIONS = 1000;

    /**
     * Run the GSEA method
     * @param geneSet genes from a defined set. eg: Pathway
     * @param geneList list of significant genes
     * @return p value
     */
    public double run(List<Gene> geneSet, List<Gene> geneList) {
        if (geneList.stream().filter(geneSet::contains).findAny().isEmpty())
            return 1;
        var sortedList = geneList.stream()
                .sorted(Comparator.comparingDouble(Gene::significanceScore).reversed())
                .collect(Collectors.toList());
        double enrichmentScore = enrichmentScore(geneSet, sortedList);
        double[] nullDistribution = new double[PERMUTATIONS];
        for (int i = 0; i < PERMUTATIONS; i++) {
            Collections.shuffle(sortedList, new SecureRandom());
            nullDistribution[i] = enrichmentScore(geneSet, sortedList);
        }
        double icount = 0;
        for (int i = 0; i < PERMUTATIONS; i++) {
            if (Math.abs(nullDistribution[i]) >= Math.abs(enrichmentScore))
                icount++;
        }
        double pvalue = icount / PERMUTATIONS;
        this.normalizedScore = enrichmentScore >= 0 ?
                enrichmentScore / Arrays.stream(nullDistribution).filter(e -> e >= 0).average().orElseThrow() :
                enrichmentScore / Math.abs(Arrays.stream(nullDistribution).filter(e -> e < 0).average().orElseThrow());
        return pvalue;
    }

    /**
     *  Calculates the enrichment score defined in the GSEA algorithm
     * @param geneSet set containing gene names
     * @param sortedList list of genes having a significance score
     * @param stepWeight recommended values: 0, 1, 1.5, 2
     */
    private double enrichmentScore(List<Gene> geneSet, List<Gene> sortedList, double stepWeight) {
        List<BigDecimal> lHits = new ArrayList<>(100);
        List<BigDecimal> lMisses = new ArrayList<>(100);
        BigDecimal hitSum = new BigDecimal("0");
        BigDecimal missSum = new BigDecimal("0");
        var nr = sortedList.stream().filter(geneSet::contains)
                .mapToDouble(e -> Math.pow(Math.abs(e.significanceScore()), stepWeight)).sum();
        var nh = sortedList.stream().filter(geneSet::contains).count();
        for (Gene g : sortedList) {
            if (geneSet.contains(g)) {
                hitSum = hitSum.add(new BigDecimal(Double.toString(Math.pow(Math.abs(g.significanceScore()), stepWeight)))
                        .divide(BigDecimal.valueOf(nr),30 , RoundingMode.HALF_UP));
                lHits.add(hitSum);
                lMisses.add(missSum);
            } else {
                missSum = missSum.add(new BigDecimal("1")
                        .divide(new BigDecimal(Double.toString((double)sortedList.size() - nh)), 30, RoundingMode.HALF_EVEN));
                lHits.add(hitSum);
                lMisses.add(missSum);
            }
        }
        BigDecimal esBD = new BigDecimal("0");
        for (int i = 0; i < lHits.size(); i++) {
            esBD = esBD.abs().compareTo(lHits.get(i).subtract(lMisses.get(i)).abs()) > 0 ? esBD : lHits.get(i).subtract(lMisses.get(i));
        }
        return esBD.setScale(5, RoundingMode.HALF_UP).doubleValue();
    }

    /**
     *  Calculates the enrichment score defined in the GSEA algorithm using step weight = 1.5
     * @param geneSet set containing gene names
     * @param geneList list of genes having a significance score
     */
    private double enrichmentScore(List<Gene> geneSet, List<Gene> geneList) {
        return this.enrichmentScore(geneSet, geneList, 1.5);
    }

    /**
     * Does exactly as the name suggests
     * @return normalized enrichment score
     */
    public double getNormalizedScore() {
        BigDecimal res = new BigDecimal(Double.toString(normalizedScore));
        res = res.setScale(4, RoundingMode.HALF_UP);
        return res.doubleValue();
    }
}
