package problems.qbf.solvers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * GA_QBF_SC variant with population initialization by Latin Hypercube Sampling.
 */
public class GA_QBF_SC_LHS extends GA_QBF_SC {

    public GA_QBF_SC_LHS(
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

        for (int locus = 0; locus < chromosomeSize; locus++) {
            List<Integer> strata = new ArrayList<>();
            for (int s = 0; s < popSize; s++) strata.add(s);
            Collections.shuffle(strata, rng);

            for (int ind = 0; ind < popSize; ind++) {
                double u = rng.nextDouble();
                double v = (strata.get(ind) + u) / ((double) popSize);

                int bit = (v < 0.5) ? 1 : 0;
                population.get(ind).add(bit);
            }
        }

        return population;
    }
}
