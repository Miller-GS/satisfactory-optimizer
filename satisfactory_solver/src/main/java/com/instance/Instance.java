package com.instance;

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
}
