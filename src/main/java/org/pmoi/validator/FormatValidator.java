package org.pmoi.validator;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;

public class FormatValidator implements IParameterValidator {
    @Override
    public void validate(String name, String value) {
        if (!value.equals("tsv") && !value.equals("fwf") && !value.equals("html")){
            throw new ParameterException("Parameter " + name + " should be TSV or FWF (found " + value + ")");
        }
    }
}
