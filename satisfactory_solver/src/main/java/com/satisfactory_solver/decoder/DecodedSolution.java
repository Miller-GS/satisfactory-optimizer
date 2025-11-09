package com.satisfactory_solver.decoder;

import java.util.Map;

public class DecodedSolution {
    private Map<String, Double> recipeUsages;
    private Map<String, Double> itemLiquidDemand;
    private int unsatisfiedDemandSum;
    private int numberOfUsedMachines;

    public DecodedSolution(Map<String, Double> recipeUsages, Map<String, Double> itemLiquidDemand) {
        this.recipeUsages = recipeUsages;
        this.itemLiquidDemand = itemLiquidDemand;
        this.unsatisfiedDemandSum = calculateUnsatisfiedDemandSum();
        this.numberOfUsedMachines = calculateNumberOfUsedMachines();
    }

    private int calculateUnsatisfiedDemandSum() {
        int sum = 0;
        for (Double demand : itemLiquidDemand.values()) {
            if (demand > 0) {
                sum += demand;
            }
        }
        return sum;
    }

    private int calculateNumberOfUsedMachines() {
        int count = 0;
        for (Double usage : recipeUsages.values()) {
            count += Math.ceil(usage);
        }
        return count;
    }

    public Map<String, Double> getRecipeUsages() {
        return recipeUsages;
    }

    public Map<String, Double> getItemLiquidDemand() {
        return itemLiquidDemand;
    }
    
    public int getUnsatisfiedDemandSum() {
        return unsatisfiedDemandSum;
    }

    public int getNumberOfUsedMachines() {
        return numberOfUsedMachines;
    }
}
