package org.pmoi.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class NumberParser {

    private static final Logger LOGGER = LogManager.getRootLogger();

    private NumberParser(){
    }

    public static boolean tryParseDouble(String value) {
        try {
            Double.parseDouble(value);
            return true;
        } catch (NumberFormatException e) {
            LOGGER.warn("Unable to parse {}", value);
            return false;
        }
    }

    public static boolean tryParseInt(String value) {
        try {
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException e) {
            LOGGER.warn("Unable to parse {}", value);
            return false;
        }
    }
}
