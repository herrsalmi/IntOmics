package org.pmoi.utils.io;

import java.util.StringJoiner;

public class TSVFormatter implements OutputFormatter {
    private StringBuffer buffer;

    public TSVFormatter() {
        this.buffer = new StringBuffer();
        this.buffer.append(String.format("%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n",
                "#protein", "name", "score D", "score R", "gene", "name", "I score", "gene_fdr", "gene_fc"));
    }

    @Override
    public void append(String ...item) {
        StringJoiner joiner = new StringJoiner("\t");
        for (String l : item) {
            joiner.add(l);
        }
        buffer.append(joiner.toString()).append("\n");
    }

    @Override
    public String getText() {
        return buffer.toString();
    }
}