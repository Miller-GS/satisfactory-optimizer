package com.satisfactory_solver.problems.solvers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import com.satisfactory_solver.metaheuristics.ga.AbstractGA;
import com.satisfactory_solver.problems.Satisfactory;
import com.satisfactory_solver.decoder.DecodedSolution;
import com.satisfactory_solver.decoder.Solution;

/**
 * Metaheuristic GA (Genetic Algorithm) for
 * obtaining an optimal solution to a Satisfactory problem for minimizing the number of machine
 * {@link #QuadracticBinaryFunction}). 
 * 
 * @author ccavellucci, fusberti
 */
public class GA_Satisfactory extends AbstractGA<Double, Double> {
    protected Double biasToMutateToZero = 0.9;

	/**
	 * Constructor for the GA_Satisfactory class. The Satisfactory objective function is passed as
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
     * Constructor for the GA_Satisfactory class. The Satisfactory objective function is passed as
     * argument for the superclass constructor
     * 
     * @param objFunction The Satisfactory objective function.
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
	 * This createEmptySol instantiates an empty solution
	 */
	@Override
	public Solution<Double> createEmptySol() {
		Solution<Double> sol = new Solution<Double>();
		return sol;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see metaheuristics.ga.AbstractGA#decode(metaheuristics.ga.AbstractGA.
	 * Chromosome)
	 */
	@Override
	protected Solution<Double> decode(Chromosome chromosome) {

		Solution<Double> solution = createEmptySol();
		for (int locus = 0; locus < chromosome.size(); locus++) {
			solution.add(chromosome.get(locus));
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
            // Introduce a chance of having a zero value
            if (rng.nextDouble() < biasToMutateToZero)
                chromosome.add(0.0);
            else
			    chromosome.add(rng.nextDouble());
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
        double penaltyMultiplier = 1.0;
        Solution<Double> decoded = decode(chromosome);
		return -decode(chromosome).cost - penaltyMultiplier * decoded.infeasibility;

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
        double rand = rng.nextDouble();
        double newValue = 0.0;

        // chance of resetting to 0
        if (rand > biasToMutateToZero) {
            newValue = chromosome.get(locus) + rng.nextGaussian() * 0.5;
            if (newValue < 0.05)
                newValue = 0.0;
            else if (newValue > 1.0)
                newValue = 1.0;
        }
		chromosome.set(locus, newValue);
	}

	/**
	 * A main method used for testing the GA metaheuristic.
	 * 
	 */
	public static void main(String[] args) throws IOException {

		// Default instance files (if none provided as args)
		List<String> instanceFiles = new ArrayList<>();
		if (args.length > 0) {
			for (String a : args) instanceFiles.add(a);
		} else {
			instanceFiles.add("instances/phase1.json");
			instanceFiles.add("instances/phase2.json");
			instanceFiles.add("instances/phase3.json");
			instanceFiles.add("instances/phase4.json");
			instanceFiles.add("instances/phase5.json");
		}

		Path logDir = Paths.get("satisfactory_logs");
		Files.createDirectories(logDir);

		// Solver suppliers (class names)
		List<Class<? extends GA_Satisfactory>> solverClasses = List.of(
				GA_Satisfactory.class,
				GA_Satisfactory_SteadyState.class,
				GA_Satisfactory_LHS.class,
				GA_Satisfactory_HybridAdaptiveMutation.class,
				GA_Satisfactory_AllStrategies.class
		);

		// For each instance file, run the solvers in parallel
		for (String filename : instanceFiles) {
			Logger mainLogger = Logger.getLogger("GA_Satisfactory.main");
			mainLogger.setUseParentHandlers(false);
			mainLogger.setLevel(Level.INFO);
			ConsoleHandler mainCh = new ConsoleHandler();
			mainCh.setFormatter(new SimpleFormatter());
			mainCh.setLevel(Level.INFO);
			mainLogger.addHandler(mainCh);
			mainLogger.info("Starting solvers for instance: " + filename);

			int nSolvers = solverClasses.size();
			ExecutorService exec = Executors.newFixedThreadPool(nSolvers);
			List<Future<Void>> futures = new ArrayList<>();

			for (Class<? extends GA_Satisfactory> solverClass : solverClasses) {
				Callable<Void> task = () -> {
					String solverName = solverClass.getSimpleName();
					String safeFilename = Paths.get(filename).getFileName().toString().replaceAll("\\\\W+", "_");
					String logFile = logDir.resolve(solverName + "_" + safeFilename + "_" + System.currentTimeMillis() + ".log").toString();

					// configure logger for this solver
					Logger solverLogger = Logger.getLogger(solverName + "." + safeFilename + "." + Thread.currentThread().getId());
					solverLogger.setUseParentHandlers(false);
					solverLogger.setLevel(Level.INFO);
					FileHandler solverFh = new FileHandler(logFile, true);
					solverFh.setFormatter(new SimpleFormatter());
					solverFh.setLevel(Level.INFO);
					solverLogger.addHandler(solverFh);
					ConsoleHandler solverCh = new ConsoleHandler();
					solverCh.setFormatter(new SimpleFormatter());
					solverCh.setLevel(Level.INFO);
					solverLogger.addHandler(solverCh);

					GA_Satisfactory gaInstance;
					try {
						gaInstance = solverClass.getConstructor(Integer.class, Integer.class, Double.class, String.class, Long.class)
								.newInstance(Integer.MAX_VALUE, 100, 1.0 / 100.0, filename, 600L);
					} catch (Exception e) {
						solverLogger.log(Level.SEVERE, "Failed to instantiate solver " + solverName + " for file " + filename, e);
						solverFh.close();
						return null;
					}

					// set logger and prefix so GA internals log to the configured logger
					gaInstance.setLogger(solverLogger);
					gaInstance.setLogPrefix("[" + solverName + "] ");

					long start = System.currentTimeMillis();
					try {
						solverLogger.info("Starting solver " + solverName + " on " + filename);
						Solution<Double> best = gaInstance.solve();
						long end = System.currentTimeMillis();
						solverLogger.info("Solver " + solverName + " finished. Best = " + best);
						solverLogger.info("Time = " + ((double) (end - start) / 1000.0) + " seg");

						DecodedSolution decoded = ((Satisfactory) gaInstance.ObjFunction).decode(best);
						solverLogger.info("Decoded Recipe Usages:");
						for (var entry : decoded.getRecipeUsages().entrySet()) {
							solverLogger.info("  " + entry.getKey() + ": " + entry.getValue());
						}
						var itemLiquidDemand = decoded.getItemLiquidDemand();
						solverLogger.info("Item Liquid Demand:");
						for (var entry : itemLiquidDemand.entrySet()) {
							solverLogger.info("  " + entry.getKey() + ": " + entry.getValue());
						}
						solverLogger.info("Unsatisfied Demand Sum: " + decoded.getUnsatisfiedDemandSum());
						solverLogger.info("Number of Used Machines: " + decoded.getNumberOfUsedMachines());

					} catch (Exception e) {
						solverLogger.log(Level.SEVERE, "Solver " + solverName + " failed on file " + filename, e);
					} finally {
						// close file handler to release the file
						solverFh.close();
					}

					return null;
				};

				futures.add(exec.submit(task));
			}

			// wait for solvers to finish for this file
			exec.shutdown();
			try {
				boolean finished = exec.awaitTermination(1, TimeUnit.HOURS);
				if (!finished) {
					mainLogger.warning("Solvers did not finish within 1 hour for file: " + filename);
				}
			} catch (InterruptedException e) {
				mainLogger.log(Level.SEVERE, "Interrupted while waiting for solvers for file: " + filename, e);
				Thread.currentThread().interrupt();
			}

			mainLogger.info("Finished all solvers for instance: " + filename);
		}
	}

}
