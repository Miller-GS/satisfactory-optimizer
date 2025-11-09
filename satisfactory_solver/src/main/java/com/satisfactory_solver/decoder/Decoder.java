package com.satisfactory_solver.decoder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

import com.satisfactory_solver.instance.Instance;
import com.satisfactory_solver.instance.ItemUsage;
import com.satisfactory_solver.instance.Recipe;

public class Decoder {
    protected Instance instance;
    protected List<String> reverseTopologicalOrder;
    protected Map<String, List<Recipe>> itemToRecipesMap;
    protected int chromosomeLength;

    public Decoder(Instance instance) {
        this.instance = instance;
        this.reverseTopologicalOrder = new RecipeGraph(instance).getTopologicalOrder().reversed();
        this.itemToRecipesMap = buildItemToRecipesMap();
    }

    protected Map<String, List<Recipe>> buildItemToRecipesMap() {
        this.chromosomeLength = 0;
        Map<String, List<Recipe>> map = new HashMap<>();
        for (Recipe recipe : instance.getRecipes()) {
            for (ItemUsage output : recipe.getOutputs()) {
                if (output.isPrimary()) {
                    map.computeIfAbsent(output.getItemName(), k -> new ArrayList<>()).add(recipe);
                    chromosomeLength++;
                }
            }
        }
        return map;
    }

    public int getChromosomeLength() {
        return this.chromosomeLength;
    }

    public DecodedSolution decode(List<Double> chromosome) {
        Map<String, Double> recipeUsages = new HashMap<>();
        // Positive represents demand, negative represents supply
        Map<String, Double> itemLiquidDemand = new HashMap<>();

        // Initialize demand
        for (ItemUsage item : instance.getFinalProducts()) {
            itemLiquidDemand.put(item.getItemName(), item.getQuantityPerMinute());
        }
        // Initialize supply
        for (ItemUsage item : instance.getRawMaterials()) {
            itemLiquidDemand.put(
                item.getItemName(),
                itemLiquidDemand.getOrDefault(item.getItemName(), 0.0) - item.getQuantityPerMinute()
            );
        }

        int index = 0;

        for (String itemName : reverseTopologicalOrder) {
            List<Recipe> recipes = itemToRecipesMap.get(itemName);
            // genes from index to index + nRecipesForItem - 1 represent the proportions for each recipe producing this item
            int nRecipesForItem = (recipes != null) ? recipes.size() : 0;
            double denominator = chromosome.subList(index, index + nRecipesForItem).stream().mapToDouble(Double::doubleValue).sum();
            double itemDemand = itemLiquidDemand.getOrDefault(itemName, 0.0);

            for (int i = 0; i < nRecipesForItem; i++) {
                Recipe recipe = recipes.get(i);
                double geneValue = chromosome.get(index + i);

                // proportion of the demand for this item to be fulfilled by this recipe
                // if the denominator is 0, distribute evenly among all recipes
                double proportion = (denominator == 0.0) ? 1.0 / nRecipesForItem : geneValue / denominator;
                double demandSatisfiedByThisRecipe = proportion * itemDemand;

                double recipeUsage = demandSatisfiedByThisRecipe / recipe.getResultingQuantityForItem(itemName);
                recipeUsages.put(
                    recipe.getRecipeName(),
                    recipeUsage + recipeUsages.getOrDefault(recipe.getRecipeName(), 0.0)
                );

                // Update demands for inputs
                for (ItemUsage input : recipe.getInputs()) {
                    String inputItemName = input.getItemName();
                    double inputQuantity = recipeUsage * input.getQuantityPerMinute();
                    itemLiquidDemand.put(
                        inputItemName,
                        itemLiquidDemand.getOrDefault(inputItemName, 0.0) + inputQuantity
                    );
                }
            }
            index += nRecipesForItem;
            itemLiquidDemand.put(itemName, 0.0); // demand for this item has been satisfied
        }
        return new DecodedSolution(recipeUsages, itemLiquidDemand);
    }
}
