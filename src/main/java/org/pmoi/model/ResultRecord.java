package org.pmoi.model;

public class ResultRecord implements Comparable<ResultRecord>{
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

    @Override
    public int compareTo(ResultRecord o) {
        if (o != null)
            return Double.compare(this.getGene().getFoldChange(), o.getGene().getFoldChange());
        return 1;
    }



}
