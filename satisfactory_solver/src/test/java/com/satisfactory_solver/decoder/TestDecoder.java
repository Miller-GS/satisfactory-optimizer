package com.satisfactory_solver.decoder;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
        // Chromosome encodes only genes for items with multiple recipe choices. In this case, only "Screw" has two recipes.
        assertEquals(decoder.getChromosomeLength(), 2);
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
        assertEquals(decoder.getChromosomeLength(), 6); // Now "Iron Rod" and "Iron Plate" also have multiple recipe choices
    }

    @Test
    public void testChromosomeLengthIgnoringNonPrimaryOutputs() {
        Recipe byproductRecipe = new Recipe(
            "Byproduct Recipe",
            List.of(new ItemUsage("Iron Ingot", 30)),
            List.of(new ItemUsage("Iron Plate", 20), new ItemUsage("Iron Rod", 10, false)) // Iron Rod is a non-primary output
        );
        instance.getRecipes().add(byproductRecipe);
        Decoder decoder = new Decoder(instance);
        assertEquals(decoder.getChromosomeLength(), 4); // "Byproduct Recipe" should not add a gene for "Iron Rod"
    }

    @Test
    public void testDecode() {
        Decoder decoder = new Decoder(instance);
        Map<Gene, Integer> genePositions = decoder.getGenePositions();

        // We only need to encode genes for "Screw" since it's the only item with multiple recipe choices
        Map<Gene, Double> chromosomeValues = Map.of(
            new Gene("Screw", "Screw"), 0.5,
            new Gene("Screw", "Alternate: Cast Screw"), 0.1
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

        assert (decodedSolution.getUnsatisfiedDemandSum() == 0) : "All demands should be satisfied, unsatisfied demand sum should be 0.";

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
            if (position != null) {
                chromosome.set(position, value);
            }
        }
        return chromosome;
    }
}
