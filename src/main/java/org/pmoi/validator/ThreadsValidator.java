package org.pmoi.validator;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;

public class ThreadsValidator implements IParameterValidator {

    @Override
    public void validate(String name, String value) {
        if (Integer.parseInt(value) < 1) {
            throw new ParameterException("Parameter " + name + " should be greater than 1 (found " + value +")");
        }
    }
}
