package com.satisfactory_solver.problems.solvers;

import java.io.IOException;
import com.satisfactory_solver.metaheuristics.ga.AbstractGA;
import com.satisfactory_solver.problems.Satisfactory;
import com.satisfactory_solver.decoder.DecodedSolution;
import com.satisfactory_solver.decoder.Decoder;
import com.satisfactory_solver.decoder.Solution;

/**
 * Metaheuristic GA (Genetic Algorithm) for
 * obtaining an optimal solution to a QBF (Quadractive Binary Function --
 * {@link #QuadracticBinaryFunction}). 
 * 
 * @author ccavellucci, fusberti
 */
public class GA_Satisfactory extends AbstractGA<Integer, Integer> {

	/**
	 * Constructor for the GA_QBF class. The QBF objective function is passed as
	 * argument for the superclass constructor.
	 * 
	 * @param generations
	 *            Maximum number of generations.
	 * @param popSize
	 *            Size of the population.
	 * @param mutationRate
	 *            The mutation rate.
	 * @param filename
	 *            Name of the file for which the objective function parameters
	 *            should be read.
	 * @throws IOException
	 *             Necessary for I/O operations.
	 */
	public GA_Satisfactory(Integer generations, Integer popSize, Double mutationRate, String filename, Long timeoutInSeconds) throws IOException {
		super(new Satisfactory(filename), generations, popSize, mutationRate, timeoutInSeconds);
	}

    /**
     * Constructor for the GA_QBF class. The QBF objective function is passed as
     * argument for the superclass constructor
     * 
     * @param objFunction The QBF objective function.
     * @param generations Maximum number of generations.
     * @param popSize Size of the population.
     * @param mutationRate The mutation rate.
     */
    public GA_Satisfactory(Satisfactory objFunction, Integer generations, Integer popSize, Double mutationRate, Long timeoutInSeconds) {
        super(objFunction, generations, popSize, mutationRate, timeoutInSeconds);
    }

	/**
	 * {@inheritDoc}
	 * 
	 * This createEmptySol instantiates an empty solution and it attributes a
	 * zero cost, since it is known that a QBF solution with all variables set
	 * to zero has also zero cost.
	 */
	@Override
	public Solution<Integer> createEmptySol() {
		Solution<Integer> sol = new Solution<Integer>();
		sol.cost = 0.0;
		return sol;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see metaheuristics.ga.AbstractGA#decode(metaheuristics.ga.AbstractGA.
	 * Chromosome)
	 */
	@Override
	protected Solution<Integer> decode(Chromosome chromosome) {

		Solution<Integer> solution = createEmptySol();
		for (int locus = 0; locus < chromosome.size(); locus++) {
			if (chromosome.get(locus) == 1) {
				solution.add(locus);
			}
		}

		ObjFunction.evaluate(solution);
		return solution;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see metaheuristics.ga.AbstractGA#generateRandomChromosome()
	 */
	@Override
	protected Chromosome generateRandomChromosome() {

		Chromosome chromosome = new Chromosome();
		for (int i = 0; i < chromosomeSize; i++) {
			chromosome.add(rng.nextInt(2));
		}

		return chromosome;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see metaheuristics.ga.AbstractGA#fitness(metaheuristics.ga.AbstractGA.
	 * Chromosome)
	 */
	@Override
	protected Double fitness(Chromosome chromosome) {

		return -decode(chromosome).cost;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * metaheuristics.ga.AbstractGA#mutateGene(metaheuristics.ga.AbstractGA.
	 * Chromosome, java.lang.Integer)
	 */
	@Override
	protected void mutateGene(Chromosome chromosome, Integer locus) {

		chromosome.set(locus, 1 - chromosome.get(locus));

	}

	/**
	 * A main method used for testing the GA metaheuristic.
	 * 
	 */
	public static void main(String[] args) throws IOException {

		long startTime = System.currentTimeMillis();
		GA_Satisfactory ga = new GA_Satisfactory(1000, 100, 1.0 / 100.0, "instances/phase3.json", null);
		Solution<Integer> bestSol = ga.solve();
		System.out.println("maxVal = " + bestSol);
		long endTime = System.currentTimeMillis();
		long totalTime = endTime - startTime;
		System.out.println("Time = " + (double) totalTime / (double) 1000 + " seg");

        DecodedSolution decodedSolution = ((Satisfactory) ga.ObjFunction).decode(bestSol);

        System.out.println("Decoded Recipe Usages:");
        for (var entry : decodedSolution.getRecipeUsages().entrySet()) {
            System.out.println("  " + entry.getKey() + ": " + entry.getValue());
        }
        var itemLiquidDemand = decodedSolution.getItemLiquidDemand();
        System.out.println("Item Liquid Demand:");
        for (var entry : itemLiquidDemand.entrySet()) {
            System.out.println("  " + entry.getKey() + ": " + entry.getValue());
        }
        System.out.println("Unsatisfied Demand Sum: " + decodedSolution.getUnsatisfiedDemandSum());
        System.out.println("Number of Used Machines: " + decodedSolution.getNumberOfUsedMachines());
	}

}
