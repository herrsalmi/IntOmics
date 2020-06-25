package org.pmoi.validator;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;

public class InteractionScoreValidator implements IParameterValidator {
    @Override
    public void validate(String name, String value) {
        if (Integer.parseInt(value) < 0 || Integer.parseInt(value) > 999) {
            throw new ParameterException("Parameter " + name + " should be between 0 and 999 (found " + value +")");
        }
    }
}
