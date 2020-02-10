package org.pmoi.utils;

import org.pmoi.models.Gene;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

public class GSEA {

    private List<Double> Phit;
    private List<Double> Pmiss;
    private double normalizedScore;
    private static final int nbrPermutations = 1000;

    public GSEA() {
    }

    public double run(List<Gene> geneSet, List<Gene> geneList) {
        if (geneList.stream().filter(geneSet::contains).findAny().isEmpty())
            return 1;
        var sortedList = geneList.stream()
                .sorted(Comparator.comparingDouble(Gene::significanceScore).reversed())
                .collect(Collectors.toList());
        double enrichmentScore = enrichmentScore(geneSet, sortedList);
        // keep the original hit/miss
        List<Double> PhitTmp = Phit;
        List<Double> PmissTmp = Pmiss;
        double[] nullDistribution = new double[nbrPermutations];
        for (int i = 0; i < nbrPermutations; i++) {
            Collections.shuffle(sortedList, new Random(i + 32));
            nullDistribution[i] = enrichmentScore(geneSet, sortedList);
        }

        //System.out.println(enrichmentScore);
        //System.out.println(Arrays.toString(nullDistribution));
        try (BufferedWriter bw = new BufferedWriter(new FileWriter("d.txt"))) {
            for (double d : nullDistribution)
                bw.write(Double.toString(d) + "\n");
        } catch (IOException ignored) {
        }
        double Icount = 0;
        for (int i = 0; i < nbrPermutations; i++) {
            if (Math.abs(nullDistribution[i]) >= Math.abs(enrichmentScore))
                Icount++;
        }
        double pvalue = Icount / nbrPermutations;
        Phit = PhitTmp;
        Pmiss = PmissTmp;
        this.normalizedScore = enrichmentScore >= 0 ?
                enrichmentScore / Arrays.stream(nullDistribution).filter(e -> e >= 0).average().getAsDouble() :
                enrichmentScore / Arrays.stream(nullDistribution).filter(e -> e < 0).average().getAsDouble();;
        return pvalue;
    }

    /**
     *  Calculates the enrichment score defined in the GSEA algorithm
     * @param geneSet set containing gene names
     * @param sortedList list of genes having a significance score
     * @param stepWeight options: 0, 1, 1.5, 2
     */
    public double enrichmentScore(List<Gene> geneSet, List<Gene> sortedList, double stepWeight) {
        Phit = new ArrayList<>();
        Pmiss = new ArrayList<>();
        BigDecimal hitSum = new BigDecimal("0");
        BigDecimal missSum = new BigDecimal("0");
        var Nr = sortedList.stream().filter(geneSet::contains)
                .mapToDouble(e -> Math.pow(Math.abs(e.significanceScore()), stepWeight)).sum();
        var Nh = sortedList.stream().filter(geneSet::contains).count();
        for (Gene g : sortedList) {
            if (geneSet.contains(g)) {
                hitSum = hitSum.add(new BigDecimal(Double.toString(Math.pow(Math.abs(g.significanceScore()), stepWeight))).divide(BigDecimal.valueOf(Nr),30 , RoundingMode.HALF_UP));
                Phit.add(hitSum.doubleValue());
                Pmiss.add(missSum.doubleValue());
            } else {
                missSum = missSum.add(new BigDecimal("1").divide(new BigDecimal(Double.toString(sortedList.size() - Nh)), 30, RoundingMode.HALF_EVEN));
                Phit.add(hitSum.doubleValue());
                Pmiss.add(missSum.doubleValue());
            }
        }

        double es = 0;
        for (int i = 0; i < Phit.size(); i++) {
            es = Math.abs(es) > Math.abs(Phit.get(i) - Pmiss.get(i)) ? es : Phit.get(i) - Pmiss.get(i);
        }
        BigDecimal res = new BigDecimal(Double.toString(es));
        res = res.setScale(5, RoundingMode.HALF_UP);
        return res.doubleValue();
    }

    /**
     *  Calculates the enrichment score defined in the GSEA algorithm using step weight = 1
     * @param geneSet set containing gene names
     * @param geneList list of genes having a significance score
     */
    public double enrichmentScore(List<Gene> geneSet, List<Gene> geneList) {
        return this.enrichmentScore(geneSet, geneList, 1);
    }

    public List<Double> getPhit() {
        return Phit;
    }

    public List<Double> getPmiss() {
        return Pmiss;
    }

    public double getNormalizedScore() {
        BigDecimal res = new BigDecimal(Double.toString(normalizedScore));
        res = res.setScale(4, RoundingMode.HALF_UP);
        return res.doubleValue();
    }
}
