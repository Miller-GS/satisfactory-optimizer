package com.satisfactory_solver.problems.solvers;
 
import java.io.IOException;
import solutions.Solution;
 
/**
 * GA_Satisfactory variant with hybrid adaptive mutation:
 * - increases mutation rate when many generations pass without improvement (stagnation)
 * - decreases mutation rate when improvements occur
 * - (NEW!) also increases mutation rate when population diversity falls below a threshold
 */

public class GA_Satisfactory_HybridAdaptiveMutation extends GA_Satisfactory
{
    // Generations stagnation control
    private int generationsWithoutImprovementsCounter;
    private final int generationsWithoutImprovementMax = 140;

    // Mutation rate boundaries
    private final double maxMR = 0.5;
    private final double minMR = 0.001;

    // (NEW!) Diversity control
    private final double diversityThreshold = 0.20; // if avg variance < threshold => low diversity
    private final int diversityCheckCooldown = 50; // avoid too many changes in short time

    private int lastDiversityAdjustmentGen = 0;
 
    public GA_Satisfactory_HybridAdaptiveMutation(Integer generations, Integer popSize, Double mutationRate, String filename, Long timeoutInSeconds) throws IOException
    {
        super(generations, popSize, mutationRate, filename, timeoutInSeconds);
        this.generationsWithoutImprovementsCounter = 0;
    }
 
    @Override
    public Solution<Double> solve()
    {
        Population population = initializePopulation();
        bestChromosome = getBestChromosome(population);
        bestSol = decode(bestChromosome);

        long startTime = System.currentTimeMillis();
        for (currentGeneration = 1; currentGeneration <= generations; currentGeneration++)
        {
            Population parents = selectParents(population);
            Population offsprings = crossover(parents);
            Population mutants = mutate(offsprings);
            Population newPopulation = selectPopulation(mutants);
            population = newPopulation;
            bestChromosome = getBestChromosome(population);
            Solution<Double> bestSolCurrentGen = decode(bestChromosome);

            // Check if current generation has improved (check strictly for lower cost AND feasibility)
            boolean improved = false;
            if (bestSolCurrentGen.cost < bestSol.cost)
            {
                if (ObjFunction.isFeasible(bestSolCurrentGen))
                {
                    bestSol = bestSolCurrentGen;
                    improved = true;
                    if (verbose)
                        logger.info(logPrefix + "(Gen. " + currentGeneration + ") BestSol = " + bestSol);
                }
                else
                    improved = false;
            }

            // --- CRITERIA FOR CHANGING MUTATION RATE ---
            // 1) Generations without improvements
            if (improved)
            {
                generationsWithoutImprovementsCounter = 0;
                double previousMutationRate = mutationRate;
                mutationRate = Math.max(mutationRate * 0.9, minMR);
                if (previousMutationRate != mutationRate && verbose)
                    logger.info(logPrefix + "[DEC_MR] Mutation rate decreased to " + mutationRate + " due to improvements in generation " + currentGeneration);
            }
            else
            {
                generationsWithoutImprovementsCounter++;
                if (generationsWithoutImprovementsCounter >= generationsWithoutImprovementMax)
                {
                    double previousMutationRate = mutationRate;
                    mutationRate = Math.min(mutationRate * 1.5, maxMR);
                    generationsWithoutImprovementsCounter = 0;
                    if (previousMutationRate != mutationRate && verbose)
                        logger.info(logPrefix + "[INC_MR] Mutation rate increased to " + mutationRate + " due to stagnation in generation " + currentGeneration);
                }
            }

            // 2) Diversity-based adaptation (compute every generation but apply only if cooldown passed)
            if ((currentGeneration - lastDiversityAdjustmentGen) >= diversityCheckCooldown)
            {
                double diversityRate = computePopulationDiversity(population);
                if (diversityRate < diversityThreshold)
                {
                    // Population is too homogeneous, so increase MR moderately
                    double previousMutationRate = mutationRate;
                    mutationRate = Math.min(mutationRate * 1.3, maxMR);
                    lastDiversityAdjustmentGen = currentGeneration;
                    if (previousMutationRate != mutationRate && verbose)
                        logger.info(logPrefix + "[INC_MR] Mutation rate increased to " + mutationRate + " due to low diversity in generation " + currentGeneration);
                }
                else
                {
                    // If diversity is very high we can slightly decrease MR. This adjustment is very mild to avoid removing exploration capacity
                    double highDiversityThreshold = diversityThreshold * 10;
                    if (diversityRate > highDiversityThreshold && mutationRate > minMR)
                    {
                        double previousMutationRate = mutationRate;
                        mutationRate = Math.max(mutationRate * 0.95, minMR);
                        lastDiversityAdjustmentGen = currentGeneration;
                        if (previousMutationRate != mutationRate && verbose)
                            logger.info(logPrefix + "[DEC_MR] Mutation rate decreased to " + mutationRate + " due to high diversity in generation " + currentGeneration);
                    }
                }
            }

            long currentTime = System.currentTimeMillis();
            if (timeoutInSeconds != null && (currentTime - startTime) >= timeoutInSeconds * 1000)
            {
                logger.warning(logPrefix + "[TIME] Timeout after " + timeoutInSeconds + "s");
                break;
            }

        }

        if (!ObjFunction.isFeasible(bestSol))
            throw new RuntimeException("No feasible solution found.");

        return bestSol;
    }

    /**
     * Compute a simple measure of population diversity:
     * average variance across loci (genes).
     *
     * population: list of chromosomes (each is a list of Double genes)
     * returns: avgVariance (double >= 0). Lower => more homogeneous.
     */
    protected double computePopulationDiversity(Population population)
    {
        if (population == null || population.isEmpty())
            return 0.0;

        int n = population.size();
        int m = chromosomeSize;     // number of genes per chromosome

        // Step 1: Calculate mean for each gene
        double[] means = new double[m];
        for (int j = 0; j < m; j++)
        {
            double sum = 0.0;
            for (int i = 0; i < n; i++) sum += population.get(i).get(j);
            means[j] = sum / n;
        }

        // Step 2: Calculate variance for each gene
        double totalVar = 0.0;
        for (int j = 0; j < m; j++)
        {
            double var = 0.0;
            for (int i = 0; i < n; i++)
            {
                double diff = population.get(i).get(j) - means[j];
                var += diff * diff;
            }
            var = var / n; // population variance
            totalVar += var;
        }

        // Step 3: calculate average variance across all genes
        double avgVar = totalVar / m;
        return avgVar;
    }

}
