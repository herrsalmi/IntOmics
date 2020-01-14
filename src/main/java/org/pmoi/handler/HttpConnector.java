package org.pmoi.handler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.pmoi.MainEntry;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

/**
 * project IntOmics
 * Created by ayyoub on 11/10/19.
 */
public class HttpConnector {

    private static final Logger LOGGER = LogManager.getLogger(HttpConnector.class.getName());


    public HttpConnector() {
    }

    public String getContent(URL url) throws IOException {
        int count = 0;
        String output;
        while (true) {
            try {
                URLConnection connection = url.openConnection();
                HttpURLConnection httpConnection = (HttpURLConnection) connection;

                httpConnection.setRequestProperty("Content-Type", "application/json");

                InputStream response = connection.getInputStream();
                int responseCode = httpConnection.getResponseCode();

                if (responseCode != 200) {
                    throw new RuntimeException("Response code was not 200. Detected response was " + responseCode);
                }

                Reader reader = null;
                try {
                    reader = new BufferedReader(new InputStreamReader(response, "UTF-8"));
                    StringBuilder builder = new StringBuilder();
                    char[] buffer = new char[8192];
                    int read;
                    while ((read = reader.read(buffer, 0, buffer.length)) > 0) {
                        builder.append(buffer, 0, read);
                    }
                    output = builder.toString();
                } finally {
                    if (reader != null) try {
                        reader.close();
                    } catch (IOException logOrIgnore) {
                        logOrIgnore.printStackTrace();
                    }
                }
                return output;
            } catch (IOException e) {
                //LOGGER.warn("HTTP Connection: Network I/O error while connecting to server. Retrying ... (" + ++count + "/" + MainEntry.MAX_TRIES + ")");
                if (count == MainEntry.MAX_TRIES) throw e;
            }
        }
    }
}
