package com.satisfactory_solver.decoder;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.satisfactory_solver.instance.Instance;
import com.satisfactory_solver.instance.ItemUsage;
import com.satisfactory_solver.instance.Recipe;

public class TestDecoder {
    protected Instance instance;
    
    @BeforeEach
    public void setUp() {
        List<Recipe> allRecipes = new ArrayList<Recipe>();
        allRecipes.add(new Recipe(
            "Iron Ingot",
            List.of(new ItemUsage("Iron Ore", 30)),
            List.of(new ItemUsage("Iron Ingot", 30))
        ));
        allRecipes.add(new Recipe(
            "Iron Plate",
            List.of(new ItemUsage("Iron Ingot", 30)),
            List.of(new ItemUsage("Iron Plate", 20))
        ));
        allRecipes.add(new Recipe(
            "Iron Rod",
            List.of(new ItemUsage("Iron Ingot", 15)),
            List.of(new ItemUsage("Iron Rod", 15))
        ));
        allRecipes.add(new Recipe(
            "Screw",
            List.of(new ItemUsage("Iron Rod", 10)),
            List.of(new ItemUsage("Screw", 45))
        ));
        allRecipes.add(new Recipe(
            "Alternate: Cast Screw",
            List.of(new ItemUsage("Iron Ingot", 12.5)),
            List.of(new ItemUsage("Screw", 50))
        ));
        allRecipes.add(new Recipe(
            "Reinforced Iron Plate",
            List.of(new ItemUsage("Iron Plate", 30), new ItemUsage("Screw", 60)),
            List.of(new ItemUsage("Reinforced Iron Plate", 5))
        ));
        List<ItemUsage> rawMaterials = List.of(new ItemUsage("Iron Ore", 100));
        List<ItemUsage> finalProducts = List.of(new ItemUsage("Reinforced Iron Plate", 8));

        instance = new Instance(allRecipes, rawMaterials, finalProducts);
    }

    @Test
    public void testChromosomeLengthWhenOneToOneWithRecipe() {
        Decoder decoder = new Decoder(instance);
        assert (decoder.getChromosomeLength() == 6) : "When every recipe produces exactly one item, chromosome length should equal number of recipes.";
    }

    @Test
    public void testChromosomeLengthWhenOneToManyWithRecipe() {
        Recipe rodAndPlateRecipe = new Recipe(
            "Rod and Plate",
            List.of(new ItemUsage("Iron Ingot", 45)),
            List.of(new ItemUsage("Iron Rod", 15), new ItemUsage("Iron Plate", 20))
        );
        instance.getRecipes().add(rodAndPlateRecipe);
        Decoder decoder = new Decoder(instance);
        assert (decoder.getChromosomeLength() == 8) : "The number of genes should equal the sum of the number of recipes that can produce each item. " +
                                                      "If multiple items are produced by the same recipe, they should each contribute one gene.";
    }

    @Test
    public void testChromosomeLengthIgnoringNonPrimaryOutputs() {
        Recipe byproductRecipe = new Recipe(
            "Byproduct Recipe",
            List.of(new ItemUsage("Iron Ingot", 30)),
            List.of(new ItemUsage("Iron Plate", 20), new ItemUsage("Slag", 10, false)) // Slag is a non-primary output
        );
        instance.getRecipes().add(byproductRecipe);
        Decoder decoder = new Decoder(instance);
        assert (decoder.getChromosomeLength() == 7) : "Non-primary outputs should not contribute to chromosome length.";
    }
}
