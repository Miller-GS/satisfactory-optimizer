package problems.qbf.solvers;

import java.io.IOException;

import problems.qbf.QBF_SC_Inverse;
import solutions.Solution;

public class GA_QBF_SC extends GA_QBF {
    private QBF_SC_Inverse qbfSC;

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
	public GA_QBF_SC(
        Integer generations,
        Integer popSize,
        Double mutationRate,
        String filename,
        Long timeoutInSeconds
    ) throws IOException {
		super(new QBF_SC_Inverse(filename), generations, popSize, mutationRate, timeoutInSeconds);
		qbfSC = (QBF_SC_Inverse) this.ObjFunction;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see metaheuristics.ga.AbstractGA#fitness(metaheuristics.ga.AbstractGA.
	 * Chromosome)
	 */
	@Override
	protected Double fitness(Chromosome chromosome) {
        Solution<Integer> sol = decode(chromosome);
		Double cost = sol.cost;
        Double penalty = qbfSC.countUncoveredElements() * qbfSC.getCoefficientsMagnitude();
		return -(cost + penalty);
	}

    /**
	 * A main method used for testing the GA metaheuristic.
	 * 
	 */
	public static void main(String[] args) throws IOException {
        Long timeoutInSeconds = 60L;
		long startTime = System.currentTimeMillis();
		GA_QBF_SC ga = new GA_QBF_SC(10000, 100, 1.0 / 100.0, "GA-Framework/instances/qbf-sc/instance_7.txt", timeoutInSeconds);
		Solution<Integer> bestSol = ga.solve();
		System.out.println("maxVal = " + bestSol);
		long endTime = System.currentTimeMillis();
		long totalTime = endTime - startTime;
		System.out.println("Time = " + (double) totalTime / (double) 1000 + " seg");

	}
}
