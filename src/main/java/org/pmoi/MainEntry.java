package org.pmoi;


import org.pmoi.models.ProteomeType;
import org.pmoi.models.SecretomeMappingMode;

public class MainEntry {

    public MainEntry() {
        OperationDispatcher operationDispatcher = new OperationDispatcher();
        operationDispatcher.run("output/S2M", ProteomeType.LABEL_FREE, SecretomeMappingMode.GOTERM);
    }


    public static void main(String[] args) {
        new MainEntry();
    }
}
