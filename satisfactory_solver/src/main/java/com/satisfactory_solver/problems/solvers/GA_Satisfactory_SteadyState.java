package com.satisfactory_solver.problems.solvers;

import java.io.IOException;

import com.satisfactory_solver.decoder.Solution;

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
        logger.info(logPrefix + "(Gen. " + 0 + ") BestSol = " + bestSol);

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
            if (bestSolCurrentGen.cost < bestSol.cost && ObjFunction.isFeasible(bestSolCurrentGen)) {
                bestSol = bestSolCurrentGen;
                if (verbose)
					logger.info(logPrefix + "(Gen. " + currentGeneration + ") BestSol = " + bestSol);
            }

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

    @Override
    protected Population crossover(Population parents)
    {
        Population offsprings = new Population();
    
        Chromosome parent1 = parents.get(0);
        Chromosome parent2 = parents.get(1);

        // Get both crosspoints from random generation
        int crosspoint1 = rng.nextInt(chromosomeSize);
        int crosspoint2 = rng.nextInt(chromosomeSize);

        // If the first one turns out to be bigger, we switch both values
        if (crosspoint1 > crosspoint2)
        {
            int temp = crosspoint1;
            crosspoint1 = crosspoint2;
            crosspoint2 = temp;
        }
    
        Chromosome offspring1 = new Chromosome();
        Chromosome offspring2 = new Chromosome();
    
        for (int j = 0; j < chromosomeSize; j++) {
            if (j >= crosspoint1 && j < crosspoint2) {
                offspring1.add(parent2.get(j));
                offspring2.add(parent1.get(j));
            } else {
                offspring1.add(parent1.get(j));
                offspring2.add(parent2.get(j));
            }
        }
    
        offsprings.add(offspring1);
        offsprings.add(offspring2);
    
    	return offsprings;
    }
}


