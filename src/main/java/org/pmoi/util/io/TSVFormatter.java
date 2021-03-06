package org.pmoi.util.io;

import java.util.StringJoiner;

public class TSVFormatter implements OutputFormatter {
    private final StringBuilder buffer;

    public TSVFormatter() {
        this.buffer = new StringBuilder();
        this.buffer.append(String.format("%s\t%s\t%s\t%s\t%s",
                "#protein", "name", "gene", "name", "I score"));
    }

    @Override
    public void append(String ...item) {
        StringJoiner joiner = new StringJoiner("\t");
        for (String l : item) {
            joiner.add(l);
        }
        buffer.append("\n").append(joiner.toString());
    }

    @Override
    public String getText() {
        return buffer.toString();
    }
}
