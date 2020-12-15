package org.pmoi.util.io;

import java.util.StringJoiner;

public class HtmlFormatter implements OutputFormatter{

    private final StringBuilder buffer;
    String newLine = System.getProperty("line.separator");

    public HtmlFormatter() {
        buffer = new StringBuilder();
        String header = "<html>\n" +
                "<head>\n" +
                "<style>\n" +
                "\n" +
                ".styled-table {\n" +
                "\tborder-collapse: collapse;\n" +
                "\tmargin: 25px 0;\n" +
                "\tfont-size: 0.9em;\n" +
                "\tfont-family: sans-serif;\n" +
                "\tmin-width: 400px;\n" +
                "\tbox-shadow: 0 0 20px rgba(0, 0, 0, 0.15);\n" +
                "}\n" +
                "\n" +
                ".styled-table thead tr {\n" +
                "\tbackground-color: #009879;\n" +
                "\tcolor: #ffffff;\n" +
                "\ttext-align: left;\n" +
                "}\n" +
                "\n" +
                ".styled-table th,\n" +
                ".styled-table td {\n" +
                "\tpadding: 12px 15px;\n" +
                "}\n" +
                "\n" +
                ".styled-table tbody tr {\n" +
                "\tborder-bottom: 1px solid #dddddd;\n" +
                "}\n" +
                "\n" +
                ".styled-table tbody tr:nth-of-type(even) {\n" +
                "\tbackground-color: #f3f3f3;\n" +
                "}\n" +
                "\n" +
                ".styled-table tbody tr:last-of-type {\n" +
                "\tborder-bottom: 2px solid #009879;\n" +
                "}\t\n" +
                "\n" +
                "</style>\n" +
                "</head>\n" +
                "<body>\n" +
                "<table class=\"styled-table\">\n" +
                "<colgroup>\n" +
                "   <col span=\"1\" style=\"width: 5%;\">\n" +
                "   <col span=\"1\" style=\"width: 15%;\">\n" +
                "   <col span=\"1\" style=\"width: 5%;\">\n" +
                "   <col span=\"1\" style=\"width: 15%;\">\n" +
                "   <col span=\"1\" style=\"width: 5%;\">\n" +
                "   <col span=\"1\" style=\"width: 60%;\">\n" +
                "</colgroup>\n" +
                "  <thead>\n" +
                "\t<tr>\n" +
                "\t  <th>Protein</th>\n" +
                "\t  <th>Name</th>\n" +
                "\t  <th>Gene</th>\n" +
                "\t  <th>Name</th>\n" +
                "\t  <th>Score</th>\n" +
                "\t  <th>Pathways</th>\n" +
                "\t</tr>\n" +
                "  <thead>\n" +
                "\t\t\t\t  <tbody>";
        buffer.append(header);
    }

    @Override
    public void append(String... item) {
        StringJoiner joiner = new StringJoiner("</td><td>", "<tr><td>", "</td></tr>");
        for (String l : item) {
            joiner.add(l);
        }
        buffer.append(joiner.toString());
    }

    @Override
    public String getText() {
        buffer.append("</tbody>")
              .append("</table>")
              .append("</body>")
              .append("</html>");
        return buffer.toString();
    }
}
