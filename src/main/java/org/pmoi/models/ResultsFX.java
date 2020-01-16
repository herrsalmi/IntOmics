package org.pmoi.models;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class ResultsFX {

    private final StringProperty protein;
    private final StringProperty proteinID;
    private final StringProperty proteinName;
    private final StringProperty gene;
    private final StringProperty geneId;
    private final StringProperty geneName;
    private final StringProperty IScore;
    private final StringProperty DScore;
    private final StringProperty RScore;
    private final StringProperty pvalue;
    private final StringProperty fc;
    private final StringProperty pathway;

    public ResultsFX(String protein, String proteinID, String proteinName, String gene,
                     String geneId, String geneName, String IScore, String DScore,
                     String RScore, String pvalue, String fc, String pathway) {
        this.protein = new SimpleStringProperty(protein);
        this.proteinID = new SimpleStringProperty(proteinID);
        this.proteinName = new SimpleStringProperty(proteinName);
        this.gene = new SimpleStringProperty(gene);
        this.geneId = new SimpleStringProperty(geneId);
        this.geneName = new SimpleStringProperty(geneName);
        this.IScore = new SimpleStringProperty(IScore);
        this.DScore = new SimpleStringProperty(DScore);
        this.RScore = new SimpleStringProperty(RScore);
        this.pvalue = new SimpleStringProperty(pvalue);
        this.fc = new SimpleStringProperty(fc);
        this.pathway = new SimpleStringProperty(pathway);
    }

    public String getProtein() {
        return protein.get();
    }

    public StringProperty proteinProperty() {
        return protein;
    }

    public void setProtein(String protein) {
        this.protein.set(protein);
    }

    public String getProteinID() {
        return proteinID.get();
    }

    public StringProperty proteinIDProperty() {
        return proteinID;
    }

    public void setProteinID(String proteinID) {
        this.proteinID.set(proteinID);
    }

    public String getProteinName() {
        return proteinName.get();
    }

    public StringProperty proteinNameProperty() {
        return proteinName;
    }

    public void setProteinName(String proteinName) {
        this.proteinName.set(proteinName);
    }

    public String getGene() {
        return gene.get();
    }

    public StringProperty geneProperty() {
        return gene;
    }

    public void setGene(String gene) {
        this.gene.set(gene);
    }

    public String getGeneId() {
        return geneId.get();
    }

    public StringProperty geneIdProperty() {
        return geneId;
    }

    public void setGeneId(String geneId) {
        this.geneId.set(geneId);
    }

    public String getGeneName() {
        return geneName.get();
    }

    public StringProperty geneNameProperty() {
        return geneName;
    }

    public void setGeneName(String geneName) {
        this.geneName.set(geneName);
    }

    public String getIScore() {
        return IScore.get();
    }

    public StringProperty IScoreProperty() {
        return IScore;
    }

    public void setIScore(String IScore) {
        this.IScore.set(IScore);
    }

    public String getDScore() {
        return DScore.get();
    }

    public StringProperty DScoreProperty() {
        return DScore;
    }

    public void setDScore(String DScore) {
        this.DScore.set(DScore);
    }

    public String getRScore() {
        return RScore.get();
    }

    public StringProperty RScoreProperty() {
        return RScore;
    }

    public void setRScore(String RScore) {
        this.RScore.set(RScore);
    }

    public String getPvalue() {
        return pvalue.get();
    }

    public StringProperty pvalueProperty() {
        return pvalue;
    }

    public void setPvalue(String pvalue) {
        this.pvalue.set(pvalue);
    }

    public String getFc() {
        return fc.get();
    }

    public StringProperty fcProperty() {
        return fc;
    }

    public void setFc(String fc) {
        this.fc.set(fc);
    }

    public String getPathway() {
        return pathway.get();
    }

    public StringProperty pathwayProperty() {
        return pathway;
    }

    public void setPathway(String pathway) {
        this.pathway.set(pathway);
    }
}
