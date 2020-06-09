package org.pmoi.validator;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ProteinValidator implements IParameterValidator {
    @Override
    public void validate(String name, String value) throws ParameterException {
        if (Files.notExists(Path.of(value))){
            throw new ParameterException("File " + value + " not found");
        }
        // check if file contains one protein per line
        try {
            var prob = Files.lines(Path.of(value))
                    .filter(l -> l.contains(",") || l.contains(";") || l.contains(" ") || l.contains("\t"))
                    .findAny();
            if (prob.isPresent())
                throw new ParameterException("File " + value + " should contain one value per line");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
