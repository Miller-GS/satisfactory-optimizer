package com.satisfactory_solver.decoder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    @Test
    public void testDecode() {
        Decoder decoder = new Decoder(instance);
        Map<Gene, Integer> genePositions = decoder.getGenePositions();
        Map<Gene, Double> chromosomeValues = Map.of(
            new Gene("Iron Ingot", "Iron Ingot"), 0.5,
            new Gene("Iron Plate", "Iron Plate"), 1.0,
            new Gene("Iron Rod", "Iron Rod"), 0.3,
            new Gene("Screw", "Screw"), 0.5,
            new Gene("Screw", "Alternate: Cast Screw"), 0.1,
            new Gene("Reinforced Iron Plate", "Reinforced Iron Plate"), 1.0
        );
        List<Double> chromosome = buildChromosome(chromosomeValues, genePositions);
        DecodedSolution decodedSolution = decoder.decode(chromosome);
        Map<String, Double> recipeUsages = decodedSolution.getRecipeUsages();

        assert (recipeUsages.get("Reinforced Iron Plate") == 8.0 / 5.0) : "Reinforced Iron Plate recipe usage should be 1.6 to meet final product demand of 8.";
        // 96 screws will be used to make 8 Reinforced Iron Plates (8 * 12 screws each)
        // 5/6 of that should come from the Screw recipe (0.5 / (0.5 + 0.1) of total screw production), totaling 80 screws
        assert (recipeUsages.get("Screw") == 80.0 / 45.0) : "Screw recipe usage should be 1.777... to produce 80 screws.";
        // 1/6 of screws should come from Alternate: Cast Screw, totaling 16 screws
        assert (recipeUsages.get("Alternate: Cast Screw") == 16.0 / 50.0) : "Alternate: Cast Screw recipe usage should be 0.32 to produce 16 screws.";
        assert (recipeUsages.get("Iron Plate") == 48.0 / 20.0) : "Iron Plate recipe usage should be 0.4 to produce 48 iron plates.";
        assert (recipeUsages.get("Iron Rod") == (10 * 80.0 / 45.0) / 15.0) : "Iron Rod recipe usage should be 1.1858585... to produce 17.7777 iron rods.";
        
        double expectedIronIngotsFromPlates = (48.0 / 20.0) * 30.0; // iron ingots used to make iron plates
        double expectedIronIngotsFromRods = ((10 * 80.0 / 45.0) / 15.0) * 15.0; // iron ingots used to make iron rods
        double expectedIronIngotsFromCastScrews = (16.0 / 50.0) * 12.5; // iron ingots used to make screws using cast screw recipe
        double totalIronIngotsNeeded = expectedIronIngotsFromPlates + expectedIronIngotsFromRods + expectedIronIngotsFromCastScrews;

        assert (recipeUsages.get("Iron Ingot") == (totalIronIngotsNeeded / 30.0)) : "Iron Ingot recipe usage should be 3.125926... to produce required iron ingots.";

    }

    protected List<Double> buildChromosome(Map<Gene, Double> geneValues, Map<Gene, Integer> genePositions) {
        int chromosomeLength = genePositions.size();
        List<Double> chromosome = new ArrayList<>(chromosomeLength);
        for (int i = 0; i < chromosomeLength; i++) {
            chromosome.add(0.0);
        }
        for (Map.Entry<Gene, Double> entry : geneValues.entrySet()) {
            Gene gene = entry.getKey();
            Double value = entry.getValue();
            Integer position = genePositions.get(gene);
            chromosome.set(position, value);
        }
        return chromosome;
    }
}
