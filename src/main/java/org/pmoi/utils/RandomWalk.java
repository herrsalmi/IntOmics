package org.pmoi.utils;

import com.google.common.util.concurrent.AtomicDouble;
import org.pmoi.models.Feature;
import org.pmoi.models.Gene;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class RandomWalk {
    private List<Double> runningSum;

    public void walk(Set<String> geneSet, List<Gene> dataset) {
        runningSum = new ArrayList<>(dataset.size());
        AtomicDouble sum = new AtomicDouble(0);
        long hitSize = geneSet.stream().filter(e -> dataset.stream().map(Feature::getName).collect(Collectors.toList()).contains(e)).count();
        System.out.println(hitSize);
        double stepUp = Math.sqrt(((double)dataset.size() - hitSize) / hitSize);
        double stepDown = - Math.sqrt((double)hitSize / (dataset.size() - hitSize));
        dataset.stream().sorted(Comparator.comparingDouble(Gene::significanceScore).reversed())
                .forEach(e -> {
                    System.out.println(e.significanceScore());
                    runningSum.add(sum.addAndGet(geneSet.contains(e.getName()) ? stepUp : stepDown));
                });

    }

    public List<Double> getRunningSum() {
        return runningSum;
    }
}
