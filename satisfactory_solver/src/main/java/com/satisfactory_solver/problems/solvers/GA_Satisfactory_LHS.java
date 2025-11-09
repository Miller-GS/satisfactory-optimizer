package com.satisfactory_solver.problems.solvers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * GA_Satisfactory_LHS variant with population initialization by Latin Hypercube Sampling.
 */
public class GA_Satisfactory_LHS extends GA_Satisfactory {

    public GA_Satisfactory_LHS(
        Integer generations,
        Integer popSize,
        Double mutationRate,
        String filename,
        Long timeoutInSeconds
    ) throws IOException {
        super(generations, popSize, mutationRate, filename, timeoutInSeconds);
    }

    @Override
    protected Population initializePopulation() {

        Population population = new Population();

        for (int i = 0; i < popSize; i++) {
            population.add(new Chromosome());
        }

        for (int locus = 0; locus < chromosomeSize; locus++)
        {
            List<Integer> strata = new ArrayList<>();
            for (int s = 0; s < popSize; s++) strata.add(s);
            Collections.shuffle(strata, rng);

            for (int ind = 0; ind < popSize; ind++)
            {
                double value = (strata.get(ind) + rng.nextDouble()) / ((double) popSize);
                population.get(ind).add(value);
            }
        }

        return population;
    }

}
