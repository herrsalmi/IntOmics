package org.pmoi;


import org.apache.commons.math3.distribution.HypergeometricDistribution;
import org.pmoi.business.PathwayClient;
import org.pmoi.models.ProteomeType;
import org.pmoi.models.SecretomeMappingMode;


public class MainEntry {

    public MainEntry() {
        PathwayClient pathwayClient = new PathwayClient();
        System.out.println(pathwayClient.getNumberOfGenes());
        HypergeometricDistribution distribution = new HypergeometricDistribution((int)pathwayClient.getNumberOfGenes(), 80, 1700);
        System.out.println(distribution.probability(10));
        System.exit(0);
        OperationDispatcher operationDispatcher = new OperationDispatcher();
        operationDispatcher.run("output/S2M", ProteomeType.LABEL_FREE, SecretomeMappingMode.GOTERM);
    }


    public static void main(String[] args) {
        new MainEntry();
    }
}
