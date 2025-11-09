package com.satisfactory_solver.decoder;

import org.jgrapht.graph.DefaultEdge;

import com.satisfactory_solver.instance.Recipe;

public class RecipeEdge extends DefaultEdge {
    private final Recipe recipe;
    private final double inputQuantityPerMinute;
    private final double outputQuantityPerMinute;

    public RecipeEdge(Recipe recipe, double inputQuantityPerMinute, double outputQuantityPerMinute) {
        this.recipe = recipe;
        this.inputQuantityPerMinute = inputQuantityPerMinute;
        this.outputQuantityPerMinute = outputQuantityPerMinute;
    }

    public Recipe getRecipe() {
        return recipe;
    }

    public double getInputQuantityPerMinute() {
        return inputQuantityPerMinute;
    }

    public double getOutputQuantityPerMinute() {
        return outputQuantityPerMinute;
    }

    @Override
    public String toString() {
        return "(" + getSource() + "[" + inputQuantityPerMinute + "] --[" + recipe.getRecipeName() + "]--> " + getTarget() + "[" + outputQuantityPerMinute + "])";
    }
}
