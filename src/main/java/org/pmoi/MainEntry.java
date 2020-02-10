package org.pmoi;

import org.pmoi.models.ProteomeType;
import org.pmoi.models.SecretomeMappingMode;
import org.pmoi.utils.io.CsvFormater;


public class MainEntry {

    public MainEntry() {
//        HypergeometricDistribution distribution = new HypergeometricDistribution(8829,
//                481, 2282);
//        System.out.println(distribution.probability(0));
//        System.exit(0);
        OperationDispatcher operationDispatcher = new OperationDispatcher();
        operationDispatcher.run("output/S2M", ProteomeType.LABEL_FREE, SecretomeMappingMode.GOTERM, new CsvFormater());
    }


    public static void main(String[] args) {
        new MainEntry();
    }
}
