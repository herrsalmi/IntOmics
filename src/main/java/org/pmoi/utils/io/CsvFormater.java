package org.pmoi.utils.io;

import java.util.StringJoiner;

public class CsvFormater implements OutputFormater {
    private StringBuffer buffer;

    public CsvFormater() {
        this.buffer = new StringBuffer();
        this.buffer.append(String.format("%s;%s;%s;%s;%s;%s;%s;%s;%s\n",
                "#protein", "name", "score D", "score R", "gene", "name", "I score", "gene_fdr", "gene_fc"));
    }

    @Override
    public void append(String ...item) {
        StringJoiner joiner = new StringJoiner(";");
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
