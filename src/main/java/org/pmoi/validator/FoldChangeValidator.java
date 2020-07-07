package org.pmoi.validator;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;
import org.pmoi.util.NumberParser;

public class FoldChangeValidator implements IParameterValidator {

    @Override
    public void validate(String name, String value) {
        if (!NumberParser.tryParseDouble(value)) {
            throw new ParameterException("Parameter " + name + " has an invalid value (" + value +")");
        }
    }
}
