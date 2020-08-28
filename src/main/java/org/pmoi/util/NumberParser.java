package org.pmoi.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class NumberParser {

    private static final Logger LOGGER = LogManager.getRootLogger();

    private NumberParser(){
    }

    /**
     * Check if a text can be parsed into a double
     * @param value number in text format
     * @return true if the text is a valid double
     */
    public static boolean tryParseDouble(String value) {
        try {
            Double.parseDouble(value);
            return true;
        } catch (NumberFormatException e) {
            LOGGER.debug("Unable to parse {}", value);
            return false;
        }
    }

    /**
     * Check if a text can be parsed into an int
     * @param value number in text format
     * @return true if the text is a valid double
     */
    public static boolean tryParseInt(String value) {
        try {
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
