package org.pmoi.models;

public class ResultRecord {
    private Protein protein;
    private Gene gene;
    private String interactionScore;

    public ResultRecord(Protein protein, Gene gene, String interactionScore) {
        this.protein = protein;
        this.gene = gene;
        this.interactionScore = interactionScore;
    }

    public Protein getProtein() {
        return protein;
    }

    public void setProtein(Protein protein) {
        this.protein = protein;
    }

    public Gene getGene() {
        return gene;
    }

    public void setGene(Gene gene) {
        this.gene = gene;
    }

    public String getInteractionScore() {
        return interactionScore;
    }

    public void setInteractionScore(String interactionScore) {
        this.interactionScore = interactionScore;
    }
}
