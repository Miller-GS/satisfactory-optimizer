package com.satisfactory_solver.problems.solvers;

import java.io.IOException;

public class GA_Satisfactory_Unbiased extends GA_Satisfactory {
    public GA_Satisfactory_Unbiased(Integer generations, Integer popSize, Double mutationRate, String filename, Long timeoutInSeconds) throws IOException
    {
        super(generations, popSize, mutationRate, filename, timeoutInSeconds);
        this.biasToMutateToZero = 0.0;
    }
}
