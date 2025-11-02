import problems.qbf.solvers.GA_QBF_SC;
import problems.qbf.solvers.GA_QBF_SC_LHS;
import problems.qbf.solvers.GA_QBF_SC_AdaptiveMutation;
import solutions.Solution;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.ArrayList;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.Level;

public class Main {
    private static final Logger logger = Logger.getLogger(Main.class.getName());
    
    public static void main(String[] args) {
        // Setup logger to write to file
        setupLogger();
        
        String[] instances = listInstances();
        InstanceParameters[] parameters = listParameters();

        logger.info("Starting Genetic Algorithm QBF-SC solver execution");
        logger.info("Number of instances: " + instances.length);
        logger.info("Number of parameter configurations: " + parameters.length);

        // Create a thread pool with 5 threads (one for each parameter configuration)
        ExecutorService executor = Executors.newFixedThreadPool(5);

        for (InstanceParameters param : parameters) {
            logger.info("Starting with parameters: " + param);            
            
            // Create a list to hold all futures for this instance
            List<Future<Void>> futures = new ArrayList<>();
            
            // Submit all parameter configurations for this instance to run in parallel
            for (String instance : instances) {
                Future<Void> future = executor.submit(() -> {
                    try {
                        // Create a thread-safe log message prefix
                        String logPrefix = "[" + param.getAlias() + "] Instance " + instance + " - ";
                        GA_QBF_SC solver = param.createSolver(instance, logger, logPrefix);
                        long startTime = System.currentTimeMillis();
                        
                        synchronized (logger) {
                            logger.info(logPrefix + "Starting instance: " + instance);
                        }
                        
                        Solution<Integer> bestSol = solver.solve();
                        long endTime = System.currentTimeMillis();
                        long executionTime = endTime - startTime;
                        
                        synchronized (logger) {
                            logger.info(logPrefix + "maxVal = " + bestSol);
                            logger.info(logPrefix + "Solution found in " + executionTime + " ms");
                            logger.info(logPrefix + "Completed successfully");
                        }
                        
                    } catch (Exception e) {
                        synchronized (logger) {
                            logger.severe("[" + Thread.currentThread().getName() + "] Error solving instance " + 
                                        instance + " with parameters " + param + ": " + e.getMessage());
                            logger.severe("[" + Thread.currentThread().getName() + "] Stack trace: " + 
                                        java.util.Arrays.toString(e.getStackTrace()));
                        }
                    }
                    return null;
                });
                futures.add(future);
            }
            
            // Wait for all parameter configurations for this instance to complete
            try {
                for (Future<Void> future : futures) {
                    future.get(); // This will block until the task completes
                }
                logger.info("All parameter configurations completed for parameters: " + param + "\n");
            } catch (Exception e) {
                logger.severe("Error waiting for tasks to complete for parameters " + param + ": " + e.getMessage());
            }
        }
        
        // Shutdown the executor
        executor.shutdown();
        try {
            // Wait up to 60 seconds for remaining tasks to complete
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        logger.info("Genetic Algorithm QBF-SC solver execution completed");
    }
    
    private static void setupLogger() {
        try {
            // Create timestamp for unique log file
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
            String timestamp = dateFormat.format(new Date());
            String logFileName = "results/ga_qbf_sc_" + timestamp + ".log";
            
            // Create results directory if it doesn't exist
            java.io.File resultsDir = new java.io.File("results");
            if (!resultsDir.exists()) {
                resultsDir.mkdirs();
            }
            
            // Create file handler
            FileHandler fileHandler = new FileHandler(logFileName, true);
            fileHandler.setFormatter(new SimpleFormatter());
            
            // Configure logger
            logger.addHandler(fileHandler);
            logger.setLevel(Level.INFO);
            
            // Also log to console for immediate feedback
            logger.setUseParentHandlers(true);
            
            logger.info("Logger initialized. Output will be written to: " + logFileName);
            
        } catch (IOException e) {
            System.err.println("Failed to setup logger: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    protected static String[] listInstances() {
        int nInstances = 15;
        String[] instances = new String[nInstances];
        for (int i = 0; i < nInstances; i++) {
            instances[i] = "GA-Framework/instances/qbf-sc/instance_" + (i + (15 - nInstances)) + ".txt";
        }
        return instances;
    }

    protected static InstanceParameters[] listParameters() {
        Integer maxGenerations = Integer.MAX_VALUE; // Run until timeout
        Long timeoutInSeconds = 60L * 30L; // 30 minutes
        // Long timeoutInSeconds = 60L * 2L; // 2 minutes
        Integer population1 = 100;
        Integer population2 = 1000;
        Double mutationRate1 = 1.0 / 100.0;
        Double mutationRate2 = 10.0 / 100.0;

        return new InstanceParameters[] {
            // PADRÃO: população 100, mutação 1%, construção aleatória
            // new InstanceParameters("PADRAO", maxGenerations, population1, mutationRate1, timeoutInSeconds, StrategyEnum.RANDOM),
            // PADRÃO + POP: população 1000, mutação 1%, construção aleatória
            // new InstanceParameters("PADRAO_POP", maxGenerations, population2, mutationRate1, timeoutInSeconds, StrategyEnum.RANDOM),
            // PADRÃO + MUT: população 100, mutação 10%, construção aleatória
            // new InstanceParameters("PADRAO_MUT", maxGenerations, population1, mutationRate2, timeoutInSeconds, StrategyEnum.RANDOM),
            // PADRÃO + EVOL1 (Latin Hypercube): população 100, mutação 1%, construção alternativa 1
            // new InstanceParameters("PADRAO_EVOL1", maxGenerations, population1, mutationRate1, timeoutInSeconds, StrategyEnum.EVOL1),
            // PADRÃO + EVOL2 (Adaptive Mutation): população 100, mutação 1%, construção alternativa 2
            new InstanceParameters("PADRAO_EVOL2", maxGenerations, population1, mutationRate1, timeoutInSeconds, StrategyEnum.EVOL2),
        };
    }
}

enum StrategyEnum {
    RANDOM,
    EVOL1, // Latin Hypercube
    EVOL2  // Adaptive Mutation
}

class InstanceParameters {
    protected Integer maxGenerations;
    protected Integer populationSize;
    protected Double mutationRate;
    protected Long timeoutInSeconds;
    protected StrategyEnum strategy;
    protected String alias;

    public InstanceParameters(String alias, Integer maxGenerations, Integer populationSize, Double mutationRate, Long timeoutInSeconds, StrategyEnum strategy) {
        this.maxGenerations = maxGenerations;
        this.populationSize = populationSize;
        this.mutationRate = mutationRate;
        this.timeoutInSeconds = timeoutInSeconds;
        this.strategy = strategy;
        this.alias = alias;
    }

    public GA_QBF_SC createSolver(String filename, Logger logger, String logPrefix) throws Exception {
        GA_QBF_SC solver;
        if (strategy == StrategyEnum.RANDOM) {
            solver = new GA_QBF_SC(maxGenerations, populationSize, mutationRate, filename, timeoutInSeconds);
        } else if (strategy == StrategyEnum.EVOL1) { // Latin Hypercube
            solver = new GA_QBF_SC_LHS(maxGenerations, populationSize, mutationRate, filename, timeoutInSeconds);
        } else if (strategy == StrategyEnum.EVOL2) {
            solver = new GA_QBF_SC_AdaptiveMutation(maxGenerations, populationSize, mutationRate, filename, timeoutInSeconds);
        } else {
            throw new IllegalArgumentException("Unknown strategy: " + strategy);
        }
        solver.setLogger(logger);
        solver.setLogPrefix(logPrefix);
        return solver;
    }

        public String getAlias() {
            return alias;
        }

    @Override
    public String toString() {
        return "maxGenerations=" + maxGenerations + ", populationSize=" + populationSize + ", mutationRate=" + mutationRate + ", timeoutInSeconds=" + timeoutInSeconds + ", strategy=" + strategy;
    }
}
