package org.pmoi.utils.io;

public class TextFormater implements OutputFormater{
    private StringBuffer buffer;

    public TextFormater() {
        this.buffer = new StringBuffer();
        buffer.append(String.format("%-10s %-50s %-10s %-10s %-10s %-50s %-10s %-10s %-10s\n",
                "#protein", "name", "score D", "score R", "gene", "name", "I score", "gene_fdr", "gene_fc"));
    }

    @Override
    public void append(String ...item) {
        if (item.length == 10)
            buffer.append(String.format("%-10s %-50s %-10s %-10s %-10s %-50s %-10s %-10s %-10s %s\n",
                item[0], item[1], item[2], item[3], item[4], item[5], item[6], item[7], item[8], item[9]));
        else
            buffer.append(String.format("%-10s %-50s %-10s %-10s %-10s %-50s %-10s %-10s %-10s\n",
                    item[0], item[1], item[2], item[3], item[4], item[5], item[6], item[7], item[8]));
    }

    @Override
    public String getText() {
        return buffer.toString();
    }
}
