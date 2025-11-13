package com.satisfactory_solver.problems.solvers;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.satisfactory_solver.decoder.Solution;

public class TimeToTargetRunner {
    static class Result {
        String solver;
        int run;
        boolean success;
        double timeSeconds;
        double bestCost;
        String error;

        Result(String solver, int run, boolean success, double timeSeconds, double bestCost, String error) {
            this.solver = solver;
            this.run = run;
            this.success = success;
            this.timeSeconds = timeSeconds;
            this.bestCost = bestCost;
            this.error = error;
        }
    }

    public static void main(String[] args) throws Exception {
        String instance = "instances/random_instance_1000_recipes_1.json";
        double targetCost = 400.0;
        List<Class<? extends GA_Satisfactory>> solverClasses = List.of(
            GA_Satisfactory_Unbiased.class,
            GA_Satisfactory.class,
            GA_Satisfactory_SteadyState.class,
            GA_Satisfactory_LHS.class,
            GA_Satisfactory_HybridAdaptiveMutation.class
            // GA_Satisfactory_AllStrategies.class
        );
        int runsPerSolver = 30;
        int parallelism = 6;

        Path out = Paths.get("time_to_target_results.csv");

        // Prepare tasks
        ExecutorService exec = Executors.newFixedThreadPool(parallelism);
        List<Future<Result>> futures = new ArrayList<>();

        for (Class<? extends GA_Satisfactory> solverClass : solverClasses) {
            for (int run = 1; run <= runsPerSolver; run++) {
                final int runIdx = run;
                Callable<Result> task = () -> {
                    String solverName = solverClass.getSimpleName();
                    // instantiate
                    GA_Satisfactory gaInstance;
                    try {
                        gaInstance = solverClass.getConstructor(Integer.class, Integer.class, Double.class, String.class, Long.class)
                                .newInstance(Integer.MAX_VALUE, 100, 1.0 / 100.0, instance, 600L);
                    } catch (Exception e) {
                        return new Result(solverName, runIdx, false, -1, Double.NaN, "instantiation failed: " + e.toString());
                    }

                    // set target to stop
                    gaInstance.setTargetCostToStop(targetCost);

                    long tStart = System.currentTimeMillis();
                    try {
                        Solution<Double> best = gaInstance.solve();
                        long tEnd = System.currentTimeMillis();
                        boolean success = best.cost <= targetCost;
                        double timeSec = (tEnd - tStart) / 1000.0;
                        return new Result(solverName, runIdx, success, timeSec, best.cost, null);
                    } catch (Exception e) {
                        long tEnd = System.currentTimeMillis();
                        double timeSec = (tEnd - tStart) / 1000.0;
                        return new Result(solverName, runIdx, false, timeSec, Double.NaN, "solve failed: " + e.toString());
                    }
                };

                futures.add(exec.submit(task));
            }
        }

        // Gather results and write CSV
        Files.createDirectories(Paths.get("."));
        try (var writer = Files.newBufferedWriter(out, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            writer.write("solver,run,success,time_seconds,best_cost,error\n");
            for (Future<Result> f : futures) {
                Result r = f.get();
                String err = (r.error == null) ? "" : r.error.replaceAll(",", ";");
                writer.write(String.format("%s,%d,%b,%.6f,%.6f,%s\n", r.solver, r.run, r.success, r.timeSeconds, Double.isNaN(r.bestCost) ? -1.0 : r.bestCost, err));
                writer.flush();
            }
        }

        exec.shutdown();
        exec.awaitTermination(1, TimeUnit.HOURS);
    }
}
