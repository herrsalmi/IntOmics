package org.pmoi;

import org.pmoi.models.SecretomeMappingMode;
import org.pmoi.utils.io.TextFormatter;


public class MainEntry {

    public MainEntry() {
        OperationDispatcher operationDispatcher = new OperationDispatcher();
        operationDispatcher.run("output/S2M", SecretomeMappingMode.GOTERM, new TextFormatter());
    }


    public static void main(String[] args) {
        new MainEntry();
    }
}
