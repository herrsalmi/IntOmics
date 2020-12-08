package org.pmoi.util.io;

import java.util.StringJoiner;

public class HtmlFormatter implements OutputFormatter{

    private final StringBuilder buffer;


    public HtmlFormatter() {
        buffer = new StringBuilder();
        String header = """
                    <html>
                    <head>
                    <style>
                    
                    .styled-table {
                        border-collapse: collapse;
                        margin: 25px 0;
                        font-size: 0.9em;
                        font-family: sans-serif;
                        min-width: 400px;
                        box-shadow: 0 0 20px rgba(0, 0, 0, 0.15);
                    }
                    
                    .styled-table thead tr {
                        background-color: #009879;
                        color: #ffffff;
                        text-align: left;
                    }
                    
                    .styled-table th,
                    .styled-table td {
                        padding: 12px 15px;
                    }
                    
                    .styled-table tbody tr {
                        border-bottom: 1px solid #dddddd;
                    }
                    
                    .styled-table tbody tr:nth-of-type(even) {
                        background-color: #f3f3f3;
                    }
                    
                    .styled-table tbody tr:last-of-type {
                        border-bottom: 2px solid #009879;
                    }	
                    
                    </style>
                    </head>
                    <body>
                    <table class="styled-table">
                      <thead>
                        <tr>
                          <th>Protein</th>
                          <th>Name</th>
                          <th>Gene</th>
                          <th>Name</th>
                          <th>I score</th>
                          <th>Pathways</th>
                        </tr>
                      <thead>
                      <tbody>
                """;
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
        String footer = """
                </tbody>
                </table>
                </body>
                </html>
                """;
        buffer.append(footer);
        return buffer.toString();
    }
}
