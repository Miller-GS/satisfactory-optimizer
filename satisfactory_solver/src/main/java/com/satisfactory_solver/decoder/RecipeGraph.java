package com.satisfactory_solver.decoder;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.stream.Collectors;

import org.jgrapht.Graph;
import org.jgrapht.graph.DirectedPseudograph;
import org.jgrapht.traverse.NotDirectedAcyclicGraphException;
import org.jgrapht.traverse.TopologicalOrderIterator;

import com.satisfactory_solver.instance.Instance;

public class RecipeGraph {
    protected Instance instance;
    protected Graph<String, RecipeEdge> graph;

    public RecipeGraph(Instance instance) {
        this.instance = instance;
        this.graph = buildDependencyGraph();
        this.graph = simplifyGraph();
    }

    public List<String> getTopologicalOrder() {
        try{
            var topoIterator = new TopologicalOrderIterator<>(this.graph);
            List<String> order = new ArrayList<>();
            while (topoIterator.hasNext()) {
                order.add(topoIterator.next());
            }
            return order;
        } catch (NotDirectedAcyclicGraphException e) {
            throw new IllegalStateException(
                "There is a cycle in the recipe dependency graph. " +
                "Either remove the recipes causing cycles, or mark their outputs as \"primary\": false so they can be ignored.", e
            );
        }
    }

    protected Graph<String, RecipeEdge> buildDependencyGraph() {
        Graph<String, RecipeEdge> graph = new DirectedPseudograph<>(RecipeEdge.class);

        // One item -> one vertex
        for (var recipe : this.instance.getRecipes()) {
            for (var input : recipe.getInputs()) {
                graph.addVertex(input.getItemName());
            }
            for (var output : recipe.getOutputs()) {
                graph.addVertex(output.getItemName());
            }
        }

        // One recipe -> multiple edges
        for (var recipe : this.instance.getRecipes()) {
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

    protected Graph<String, RecipeEdge> simplifyGraph() {
        // Keep only nodes that are connected, directly or indirectly, to target items
        // Collect final product item names that exist in the graph
        Set<String> targets = this.instance.getFinalProducts().stream()
                .map(item -> item.getItemName())
                .filter(this.graph::containsVertex)
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
            for (RecipeEdge e : this.graph.incomingEdgesOf(v)) {
                String u = this.graph.getEdgeSource(e);
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

        for (RecipeEdge e : this.graph.edgeSet()) {
            String src = this.graph.getEdgeSource(e);
            String tgt = this.graph.getEdgeTarget(e);
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
