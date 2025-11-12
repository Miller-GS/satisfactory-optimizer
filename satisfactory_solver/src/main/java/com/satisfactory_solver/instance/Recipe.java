package com.satisfactory_solver.instance;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Recipe {
    protected String recipeName;
    protected List<ItemUsage> inputs;
    protected List<ItemUsage> outputs;
    protected Map<String, Double> resultingItemQuantities;

    public Recipe(String recipeName, List<ItemUsage> inputs, List<ItemUsage> outputs) {
        this.recipeName = recipeName;
        this.inputs = inputs;
        this.outputs = outputs;
        this.resultingItemQuantities = new HashMap<>();
        for (ItemUsage output : outputs) {
            resultingItemQuantities.put(output.getItemName(), output.getQuantityPerMinute());
        }
        for (ItemUsage input : inputs) {
            resultingItemQuantities.put(input.getItemName(),
                    resultingItemQuantities.getOrDefault(input.getItemName(), 0.0) - input.getQuantityPerMinute());
        }
    }

    public String getRecipeName() {
        return recipeName;
    }

    public List<ItemUsage> getInputs() {
        return inputs;
    }

    public List<ItemUsage> getOutputs() {
        return outputs;
    }

    public Double getResultingQuantityForItem(String itemName) {
        return resultingItemQuantities.getOrDefault(itemName, 0.0);
    }

    @Override
    public String toString() {
        return "Recipe{" +
                "recipeName='" + recipeName + '\'' +
                ", inputs=" + inputs +
                ", outputs=" + outputs +
                '}';
    }
}
