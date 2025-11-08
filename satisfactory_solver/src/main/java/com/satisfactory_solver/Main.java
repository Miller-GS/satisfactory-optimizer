package com.satisfactory_solver;

import com.satisfactory_solver.decoder.RecipeEdge;
import com.satisfactory_solver.instance.Instance;
import com.satisfactory_solver.instance.InstanceJsonReader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Set;
import java.util.stream.Collectors;

import org.jgrapht.*;
import org.jgrapht.graph.*;
import org.jgrapht.traverse.TopologicalOrderIterator;

public class Main {
    public static void main(String[] args) {
        Instance instance = readJsonFile("instances/phase1.json");
        Graph<String, RecipeEdge> graph = buildDependencyGraph(instance);
        graph = simplifyGraph(graph, instance);
        TopologicalOrderIterator<String, RecipeEdge> topoIterator = new TopologicalOrderIterator<>(graph);
        while (topoIterator.hasNext()) {
            String vertex = topoIterator.next();
            System.out.println(vertex);
        }
    }

    private static Instance readJsonFile(String filePath) {
        InstanceJsonReader reader = new InstanceJsonReader();
        try {
            String jsonContent = Files.readString(Paths.get(filePath), StandardCharsets.UTF_8);
            return reader.readInstanceFromJson(jsonContent);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read JSON file: " + filePath, e);
        }
    }

    private static Graph<String, RecipeEdge> buildDependencyGraph(Instance instance) {
        Graph<String, RecipeEdge> graph = new DirectedPseudograph<>(RecipeEdge.class);

        // One item -> one vertex
        for (var recipe : instance.getRecipes()) {
            for (var input : recipe.getInputs()) {
                graph.addVertex(input.getItemName());
            }
            for (var output : recipe.getOutputs()) {
                graph.addVertex(output.getItemName());
            }
        }

        // One recipe -> multiple edges
        for (var recipe : instance.getRecipes()) {
            for (var input : recipe.getInputs()) {
                for (var output : recipe.getOutputs()) {
                    if (output.isPrimary()) {
                        graph.addEdge(
                            input.getItemName(),
                            output.getItemName(),
                            new RecipeEdge(recipe, input.getQuantityPerMinute(), output.getQuantityPerMinute())
                        );
                    }
                }
            }
        }

        return graph;
    }

    private static Graph<String, RecipeEdge> simplifyGraph(Graph<String, RecipeEdge> graph, Instance instance) {
        // Keep only nodes that are connected, directly or indirectly, to target items
        // Collect final product item names that exist in the graph
        Set<String> targets = instance.getFinalProducts().stream()
                .map(item -> item.getItemName())
                .filter(graph::containsVertex)
                .collect(Collectors.toSet());

        // If no targets in the graph, return an empty graph of same type
        if (targets.isEmpty()) {
            Graph<String, RecipeEdge> empty = new DirectedPseudograph<>(RecipeEdge.class);
            return empty;
        }

        // Do a reverse traversal: from targets, follow incoming edges to collect all vertices
        java.util.Deque<String> stack = new java.util.ArrayDeque<>();
        Set<String> reachable = new java.util.HashSet<>();

        for (String t : targets) {
            reachable.add(t);
            stack.push(t);
        }

        while (!stack.isEmpty()) {
            String v = stack.pop();
            // incomingEdgesOf gives edges whose target is v (i.e., predecessors)
            for (RecipeEdge e : graph.incomingEdgesOf(v)) {
                String u = graph.getEdgeSource(e);
                if (!reachable.contains(u)) {
                    reachable.add(u);
                    stack.push(u);
                }
            }
        }

        // Build subgraph containing only reachable vertices and edges between them
        Graph<String, RecipeEdge> sub = new DirectedPseudograph<>(RecipeEdge.class);
        for (String v : reachable) {
            sub.addVertex(v);
        }

        for (RecipeEdge e : graph.edgeSet()) {
            String src = graph.getEdgeSource(e);
            String tgt = graph.getEdgeTarget(e);
            if (reachable.contains(src) && reachable.contains(tgt)) {
                try {
                    sub.addEdge(src, tgt, new RecipeEdge(e.getRecipe(), e.getInputQuantityPerMinute(), e.getOutputQuantityPerMinute()));
                } catch (IllegalArgumentException ex) {
                    // skip duplicate edges if any
                }
            }
        }

        return sub;
    }
}