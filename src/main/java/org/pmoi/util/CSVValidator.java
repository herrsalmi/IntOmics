package org.pmoi.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.pmoi.Args;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class CSVValidator {

    private static final Logger LOGGER = LogManager.getRootLogger();

    public boolean isConform(String path) {
        try (var stream = Files.lines(Path.of(path))) {
            var prob = stream.filter(l -> countOccurrences(l) != 2)
                    .findAny();
            if (prob.isPresent()) {
                LOGGER.error("incorrect number of column in CSV file. line [{}]", prob.get());
                return false;
            }
        } catch (IOException e) {
            LOGGER.error(e);
        }
        return true;
    }

    private long countOccurrences(String line) {
        int count = 0;
        int index = 0;
        while (( index = line.indexOf(Args.getInstance().getSeparator(), index)) != -1) {
            count++;
            index++;
        }
        return count;
    }
}
