package com.satisfactory_solver.decoder;

import java.util.Map;

public class DecodedSolution {
    private Map<String, Double> recipeUsages;
    private Map<String, Double> itemLiquidDemand;
    private int unsatisfiedDemandSum;

    public DecodedSolution(Map<String, Double> recipeUsages, Map<String, Double> itemLiquidDemand) {
        this.recipeUsages = recipeUsages;
        this.itemLiquidDemand = itemLiquidDemand;
        this.unsatisfiedDemandSum = calculateUnsatisfiedDemandSum();
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

    public Map<String, Double> getRecipeUsages() {
        return recipeUsages;
    }

    public Map<String, Double> getItemLiquidDemand() {
        return itemLiquidDemand;
    }
    
    public int getUnsatisfiedDemandSum() {
        return unsatisfiedDemandSum;
    }
}
