package org.pmoi.validator;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;

import java.nio.file.Files;
import java.nio.file.Path;

public class GeneValidator implements IParameterValidator {
    @Override
    public void validate(String name, String value) {
        if (Files.notExists(Path.of(value))){
            throw new ParameterException("File " + value + " not found");
        }

    }
}
