package com.satisfactory_solver.instance;

import java.util.List;

public class Recipe {
    protected String recipeName;
    protected List<ItemUsage> inputs;
    protected List<ItemUsage> outputs;

    public Recipe(String recipeName, List<ItemUsage> inputs, List<ItemUsage> outputs) {
        this.recipeName = recipeName;
        this.inputs = inputs;
        this.outputs = outputs;
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
}
