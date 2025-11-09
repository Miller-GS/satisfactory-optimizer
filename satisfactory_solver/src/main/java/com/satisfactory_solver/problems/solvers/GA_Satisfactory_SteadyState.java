package problems.satisfactory.solvers;

import java.io.IOException;
import solutions.Solution;

public class GA_Satisfactory_SteadyState extends GA_Satisfactory
{
    public GA_Satisfactory_SteadyState(Integer generations, Integer popSize, Double mutationRate, String filename, Long timeoutInSeconds) throws IOException
    {
        super(generations, popSize, mutationRate, filename, timeoutInSeconds);
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
            // Calculates number of individuals to be exchanged in the steady-state selection
            int replacementsCount = Math.max(2, (int) (0.04 * popSize));
            int generatedChildren = 0;

            // The while condition checks for the count of generated children
            while (generatedChildren < replacementsCount)
            {
                // Selecting parents
                Population parents = selectParents(population);
                Chromosome parent1 = parents.get(rng.nextInt(parents.size()));
                Chromosome parent2 = parents.get(rng.nextInt(parents.size()));

                // Generates new individuals and mutate them. Crossover strategy is currently the 2-point crossover
                Population children = crossover(new Population() {{
                    add(parent1);
                    add(parent2);
                }});
                children = mutate(children);

                // Evaluating new individuals and exchanging worst individuals in the current population
                for (Chromosome child : children)
                {
                    Chromosome worst = getWorseChromosome(population);
                    double childFitness = fitness(child);
                    double worstFitness = fitness(worst);
                    if (childFitness > worstFitness)
                    {
                        population.remove(worst);
                        population.add(child);
                    }

                    // Incrementing generated children count
                    generatedChildren++;
                    if (generatedChildren >= replacementsCount)
                        break;
                }
            }

            // Update best solution
            bestChromosome = getBestChromosome(population);
            Solution<Double> bestSolCurrentGen = decode(bestChromosome);
            if (bestSolCurrentGen.cost < bestSol.cost && ObjFunction.isFeasible(bestSolCurrentGen))
                bestSol = bestSolCurrentGen;

            long currentTime = System.currentTimeMillis();
            if (timeoutInSeconds != null && (currentTime - startTime) >= timeoutInSeconds * 1000)
            {
                logger.warning(logPrefix + "Timeout reached after " + timeoutInSeconds + " seconds.");
                break;
            }
        }

        if (!ObjFunction.isFeasible(bestSol))
            logger.warning("No feasible solution found.");

        return bestSol;
    }
}
