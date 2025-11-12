package com.satisfactory_solver.instance;

import java.util.List;

public class Instance {
    protected List<Recipe> recipes;
    protected List<ItemUsage> rawMaterials;
    protected List<ItemUsage> finalProducts;

    public Instance(List<Recipe> recipes, List<ItemUsage> rawMaterials, List<ItemUsage> finalProducts) {
        this.recipes = recipes;
        this.rawMaterials = rawMaterials;
        this.finalProducts = finalProducts;
    }

    public List<Recipe> getRecipes() {
        return recipes;
    }

    public List<ItemUsage> getRawMaterials() {
        return rawMaterials;
    }

    public List<ItemUsage> getFinalProducts() {
        return finalProducts;
    }

    @Override
    public String toString() {
        return "Instance{" +
                "recipes=" + recipes +
                ", rawMaterials=" + rawMaterials +
                ", finalProducts=" + finalProducts +
                '}';
    }
}
