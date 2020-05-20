package org.pmoi;

import org.pmoi.models.ProteomeType;
import org.pmoi.models.SecretomeMappingMode;
import org.pmoi.utils.io.TextFormatter;


public class MainEntry {

    public MainEntry() {
//        HypergeometricDistribution distribution = new HypergeometricDistribution(8829,
//                481, 2282);
//        System.out.println(distribution.probability(0));
//        System.exit(0);
        OperationDispatcher operationDispatcher = new OperationDispatcher();
        operationDispatcher.run("output/S2M", ProteomeType.LCMS, SecretomeMappingMode.GOTERM, new TextFormatter());
    }


    public static void main(String[] args) {
        new MainEntry();
    }
}
