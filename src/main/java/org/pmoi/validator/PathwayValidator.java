package org.pmoi.validator;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;

public class PathwayValidator implements IParameterValidator {
    @Override
    public void validate(String name, String value) {
        if (!value.equals("KEGG") && !value.equals("WIKIPATHWAYS") && !value.equals("REACTOME")){
            throw new ParameterException("Parameter " + name + " should be KEGG, WIKIPATHWAYS or REACTOME (found " + value + ")");
        }
    }
}
