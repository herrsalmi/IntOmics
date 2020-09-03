package org.pmoi.validator;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;
import org.pmoi.util.NumberParser;

public class PvalueValidator implements IParameterValidator {

    @Override
    public void validate(String name, String value) {
        if (!NumberParser.tryParseDouble(value)) {
            throw new ParameterException("Parameter " + name + " has an invalid value (" + value +")");
        }
        if (Double.parseDouble(value) > 1 || Double.parseDouble(value) < 0) {
            throw new ParameterException("Parameter " + name + " should be between 1 and 0 (found : " + value +")");
        }
    }
}
