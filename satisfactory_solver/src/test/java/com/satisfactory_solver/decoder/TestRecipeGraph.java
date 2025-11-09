package com.satisfactory_solver.decoder;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import com.satisfactory_solver.instance.Instance;
import com.satisfactory_solver.instance.Recipe;
import com.satisfactory_solver.instance.ItemUsage;

public class TestRecipeGraph {
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
            "Screws",
            List.of(new ItemUsage("Iron Rod", 10)),
            List.of(new ItemUsage("Screws", 45))
        ));
        allRecipes.add(new Recipe(
            "Reinforced Iron Plate",
            List.of(new ItemUsage("Iron Plate", 30), new ItemUsage("Screws", 60)),
            List.of(new ItemUsage("Reinforced Iron Plate", 5))
        ));
        List<ItemUsage> rawMaterials = List.of(new ItemUsage("Iron Ore", 100));
        List<ItemUsage> finalProducts = List.of(new ItemUsage("Reinforced Iron Plate", 8));

        instance = new Instance(allRecipes, rawMaterials, finalProducts);
    }

    @Test
    public void testGetTopologicalOrder() {
        RecipeGraph graph = new RecipeGraph(instance);
        List<String> topologicalOrder = graph.getTopologicalOrder();
        Map<String, Integer> itemPositions = new HashMap<>();
        for (int i = 0; i < topologicalOrder.size(); i++) { 
            itemPositions.put(topologicalOrder.get(i), i);
        }

        // Check that the topological order is valid
        assertTrue(itemPositions.get("Iron Ore") < itemPositions.get("Iron Ingot"));
        assertTrue(itemPositions.get("Iron Ingot") < itemPositions.get("Iron Plate"));
        assertTrue(itemPositions.get("Iron Ingot") < itemPositions.get("Iron Rod"));
        assertTrue(itemPositions.get("Iron Rod") < itemPositions.get("Screws"));
        assertTrue(itemPositions.get("Iron Plate") < itemPositions.get("Reinforced Iron Plate"));
        assertTrue(itemPositions.get("Screws") < itemPositions.get("Reinforced Iron Plate"));
    }

    @Test
    public void testShouldErrorOnCyclicGraph() {
        instance.getRecipes().add(new Recipe(
            "Cyclic Recipe",
            List.of(new ItemUsage("Reinforced Iron Plate", 1)),
            List.of(new ItemUsage("Iron Ore", 1))
        ));

        RecipeGraph graph = new RecipeGraph(instance);
        try {
            graph.getTopologicalOrder();
            assertTrue(false, "Expected an exception due to cyclic graph");
        } catch (IllegalStateException e) {
            assertTrue(true);
        }
    }

    @Test
    public void shouldIgnoreOutputsMarkedAsNonPrimary() {
        instance.getRecipes().add(new Recipe(
            "Cyclic Recipe",
            List.of(new ItemUsage("Reinforced Iron Plate", 1)),
            List.of(new ItemUsage("Iron Ore", 1, false))
        ));
        RecipeGraph graph = new RecipeGraph(instance);
        try {
            graph.getTopologicalOrder();
            assertTrue(true);
        } catch (IllegalStateException e) {
            assertTrue(false, "Did not expect a cycle since the output is marked as non-primary");
        }
    }
}
