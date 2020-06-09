package org.pmoi.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.pmoi.ApplicationParameters;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;

/**
 * project IntOmics
 * Created by ayyoub on 11/10/19.
 */
public class HttpConnector {

    private static final Logger LOGGER = LogManager.getRootLogger();
    
    public String getContent(URL url) throws IOException {
        int count = 0;
        String output = null;
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

                try (Reader reader = new BufferedReader(new InputStreamReader(response, StandardCharsets.UTF_8))) {
                    StringBuilder builder = new StringBuilder();
                    char[] buffer = new char[8192];
                    int read;
                    while ((read = reader.read(buffer, 0, buffer.length)) > 0) {
                        builder.append(buffer, 0, read);
                    }
                    output = builder.toString();
                } catch (IOException e) {
                        LOGGER.error(e);
                }
                return output;
            } catch (IOException e) {
                if (count == ApplicationParameters.getInstance().getMaxTries()) throw e;
            }
        }
    }
}
