package org.pmoi.model;

import org.pmoi.Args;

import java.util.List;

public class GeneSet {
    private String name;
    private String identifier;
    private List<Gene> genes;
    private double pvalue;
    private double score;

    public GeneSet(String identifier, String name, List<Gene> genes) {
        this.identifier = identifier;
        this.name = name;
        this.genes = genes;
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getName() {
        if (Args.getInstance().getFormat().equals(OutputMode.HTML)) {
            return switch (Args.getInstance().getPathwayDB()) {
                case KEGG -> String.format("<a href=\"%s\">%s</a>",
                        formatKEGGLink(), name);
                case WIKIPATHWAYS -> String.format("<a href=\"https://www.wikipathways.org/index.php/Pathway:%s\">%s</a>",
                        identifier, name);
                case REACTOME -> String.format("<a href=\"https://reactome.org/PathwayBrowser/#/%s\">%s</a>",
                        identifier, name);
            };
        } else {
            return name;
        }
    }

    private String formatKEGGLink() {
        StringBuilder url = new StringBuilder("https://www.kegg.jp/kegg-bin/show_pathway?map=" + identifier + "&multi_query=");
        for (var gene : genes) {
            url.append(gene.ncbiID);
            if (gene.foldChange > 0) {
                url.append("+green,");
            } else {
                url.append("+red,");
            }
            url.append("%0d%0a");
        }
        url.delete(url.length() - 6, url.length());
        url.append("&nocolor=1");
        return url.toString();
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Gene> getGenes() {
        return genes;
    }

    public void setGenes(List<Gene> genes) {
        this.genes = genes;
    }

    public double getPvalue() {
        return pvalue;
    }

    public void setPvalue(double pvalue) {
        this.pvalue = pvalue;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }
}
