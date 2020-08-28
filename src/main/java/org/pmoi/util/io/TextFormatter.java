package org.pmoi.util.io;

public class TextFormatter implements OutputFormatter {
    private final StringBuilder buffer;

    public TextFormatter() {
        this.buffer = new StringBuilder();
        buffer.append(String.format("%-10s %-50s %-10s %-50s %-10s%n",
                "#protein", "name", "gene", "name", "I score"));
    }

    @Override
    public void append(String ...item) {
        String n1 = item[1].length() > 49 ? item[1].substring(0, 46) + "..." : item[1];
        String n2 = item[3].length() > 49 ? item[3].substring(0, 46) + "..." : item[3];
        if (item.length == 6) {
            buffer.append(String.format("%-10s %-50s %-10s %-50s %-10s %s%n",
                item[0], n1, item[2], n2, item[4], item[5]));
        }
        else {
            buffer.append(String.format("%-10s %-50s %-10s %-50s %-10s%n",
                    item[0], n1, item[2], n2, item[4]));
        }
    }

    @Override
    public String getText() {
        return buffer.toString();
    }
}
