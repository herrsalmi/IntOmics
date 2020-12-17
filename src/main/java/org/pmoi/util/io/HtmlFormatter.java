package org.pmoi.util.io;

import java.util.StringJoiner;

public class HtmlFormatter implements OutputFormatter{

    private final StringBuilder buffer;

    public HtmlFormatter() {
        buffer = new StringBuilder();
        String header = "<html>\n" +
                "\t<head>\n" +
                "\t\t<style>\n" +
                "\t\t\t.styled-table {\n" +
                "\t\t\t\tborder-collapse: collapse;\n" +
                "\t\t\t\tmargin: 25px 0;\n" +
                "\t\t\t\tfont-size: 0.9em;\n" +
                "\t\t\t\tfont-family: sans-serif;\n" +
                "\t\t\t\tmin-width: 400px;\n" +
                "\t\t\t\tbox-shadow: 0 0 20px rgba(0, 0, 0, 0.15);\n" +
                "\t\t\t}\n" +
                "\n" +
                "\t\t\t.styled-table thead tr {\n" +
                "\t\t\t\tbackground-color: #009879;\n" +
                "\t\t\t\tcolor: #ffffff;\n" +
                "\t\t\t\ttext-align: left;\n" +
                "\t\t\t}\n" +
                "\n" +
                "\t\t\t.styled-table th,\n" +
                "\t\t\t.styled-table td {\n" +
                "\t\t\t\tpadding: 12px 15px;\n" +
                "\t\t\t}\n" +
                "\n" +
                "\t\t\t.styled-table tbody tr {\n" +
                "\t\t\t\tborder-bottom: 1px solid #dddddd;\n" +
                "\t\t\t}\n" +
                "\n" +
                "\t\t\t.styled-table tbody tr:nth-of-type(even) {\n" +
                "\t\t\t\tbackground-color: #f3f3f3;\n" +
                "\t\t\t}\n" +
                "\n" +
                "\t\t\t.styled-table tbody tr:last-of-type {\n" +
                "\t\t\t\tborder-bottom: 2px solid #009879;\n" +
                "\t\t\t}\t\n" +
                "\t\t</style>\n" +
                "\t</head>\n" +
                "\t<body>\n" +
                "\t\t<table class=\"styled-table\">\n" +
                "\t\t\t<colgroup>\n" +
                "\t\t\t\t<col span=\"1\" style=\"width: 5%;\"/>\n" +
                "\t\t\t\t<col span=\"1\" style=\"width: 15%;\"/>\n" +
                "\t\t\t\t<col span=\"1\" style=\"width: 5%;\"/>\n" +
                "\t\t\t\t<col span=\"1\" style=\"width: 15%;\"/>\n" +
                "\t\t\t\t<col span=\"1\" style=\"width: 5%;\"/>\n" +
                "\t\t\t\t<col span=\"1\" style=\"width: 60%;\"/>\n" +
                "\t\t\t</colgroup>\n" +
                "\t\t\t<thead>\n" +
                "\t\t\t\t<tr>\n" +
                "\t\t\t\t\t<th>Protein</th>\n" +
                "\t\t\t\t\t<th>Name</th>\n" +
                "\t\t\t\t\t<th>Gene</th>\n" +
                "\t\t\t\t\t<th>Name</th>\n" +
                "\t\t\t\t\t<th>Score</th>\n" +
                "\t\t\t\t\t<th>Pathways</th>\n" +
                "\t\t\t\t</tr>\n" +
                "\t\t\t</thead>\n" +
                "\t\t\t<tbody>\n";
        buffer.append(header);
    }

    @Override
    public void append(String... item) {
        StringJoiner joiner = new StringJoiner("</td>\n\t\t\t\t\t\t<td>", "\t\t\t\t\t<tr>\n\t\t\t\t\t\t<td>", "</td>\n\t\t\t\t\t</tr>\n");
        for (String l : item) {
            joiner.add(l);
        }
        buffer.append(joiner.toString());
    }

    @Override
    public String getText() {
        buffer.append("\t\t\t</tbody>\n")
              .append("\t\t</table>\n")
              .append("\t</body>\n")
              .append("</html>");
        return buffer.toString();
    }
}
