package org.pmoi.models;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Protein extends Feature{
    private List<Double> depletedSamplesScore;
    private List<Double> rinsedSamplesScore;
    private List<Pathway> pathways;

    private static DecimalFormat decimalFormat = new DecimalFormat("#.##");

    public Protein(String name, String entrezID) {
        this.name = name;
        this.entrezID = entrezID;
    }

    public Protein(String description, Double scoreD, Double scoreR) {
        Pattern patter = Pattern.compile("GN=([A-Z0-9]+)");
        Matcher matcher = patter.matcher(description);
        if (matcher.find())
            this.name = matcher.group(1).trim();
        depletedSamplesScore = new ArrayList<>(1);
        rinsedSamplesScore = new ArrayList<>(1);
        depletedSamplesScore.add(scoreD);
        rinsedSamplesScore.add(scoreR);
        this.pathways = new ArrayList<>();
    }

    public Protein(String line) {
        decimalFormat.setRoundingMode(RoundingMode.FLOOR);
        depletedSamplesScore = new ArrayList<>(3);
        rinsedSamplesScore = new ArrayList<>(3);
        String[] info = line.split(";");
        // the first field is the gene id
        this.entrezID = info[0];
        // the 3 following fields are scores for depleted samples
        int DmaxLen = info.length > 3 ? 4 : info.length;
        for (int i = 1; i < DmaxLen; i++) {
            if (!info[i].isEmpty() && !info[i].isBlank())
                depletedSamplesScore.add(Double.valueOf(info[i].replace(',', '.')));
        }
        // the 3 following fields are scores for rinsed samples
        for (int i = 4; i < info.length; i++) {
            if (!info[i].isEmpty() && !info[i].isBlank())
                rinsedSamplesScore.add(Double.valueOf(info[i].replace(',', '.')));
        }
        this.pathways = new ArrayList<>();
    }

    public Double depletedMeanScore() {
        return Double.valueOf(decimalFormat.format(depletedSamplesScore.stream().collect(Collectors.summarizingDouble(Double::doubleValue)).getAverage()).replace(',', '.'));
    }

    public Double rinsedMeanScore() {
        return  Double.valueOf(decimalFormat.format(rinsedSamplesScore.stream().collect(Collectors.summarizingDouble(Double::doubleValue)).getAverage()).replace(',', '.'));
    }

    public boolean isMoreExpressedInDepletedSamples(double fc) {
        return depletedMeanScore() > rinsedMeanScore() && (depletedMeanScore() / rinsedMeanScore()) >= fc;
    }

    public List<Pathway> getPathways() {
        return pathways;
    }

    public void addPathway(Pathway pathway) {
        this.pathways.add(pathway);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Protein protein = (Protein) o;

        if (!Objects.equals(name, protein.name)) return false;
        return entrezID.equals(protein.entrezID);
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + entrezID.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "Protein{" +
                "depletedSamplesScore=" + depletedSamplesScore +
                ", rinsedSamplesScore=" + rinsedSamplesScore +
                ", name='" + name + '\'' +
                ", entrezID='" + entrezID + '\'' +
                '}';
    }
}