package org.pmoi.validator;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class APIKeyValidator implements IParameterValidator {
    @Override
    public void validate(String name, String value) {
        Pattern pattern = Pattern.compile("\\w+");
        Matcher matcher = pattern.matcher(value);
        if (!matcher.find()) {
            throw new ParameterException("NCBI API key malformed");
        }
    }
}
