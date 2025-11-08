package com.satisfactory_solver.instance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class InstanceJsonReader {
    public Instance readInstanceFromJson(String json) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode root = mapper.readTree(json);

            List<Recipe> recipes = new ArrayList<>();
            JsonNode recipesNode = root.path("recipes");
            if (recipesNode.isArray()) {
                for (JsonNode rNode : recipesNode) {
                    String name = rNode.path("name").asText(null);
                    List<ItemUsage> inputs = parseItemUsageList(rNode.path("inputs"));
                    List<ItemUsage> outputs = parseItemUsageList(rNode.path("outputs"));
                    recipes.add(new Recipe(name, inputs, outputs));
                }
            }

            List<ItemUsage> availableInputs = parseItemUsageList(root.path("available_inputs"));
            List<ItemUsage> desiredOutputs = parseItemUsageList(root.path("desired_outputs"));

            return new Instance(recipes, availableInputs, desiredOutputs);
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse Instance JSON", e);
        }
    }

    private List<ItemUsage> parseItemUsageList(JsonNode node) {
        List<ItemUsage> list = new ArrayList<>();
        if (node == null || node.isMissingNode()) {
            return list;
        }

        if (node.isArray()) {
            for (JsonNode iNode : node) {
                String name = iNode.path("name").asText(null);
                double qty = 0.0;
                if (iNode.has("quantity_per_min")) {
                    qty = iNode.path("quantity_per_min").asDouble(0.0);
                }

                if (name != null) {
                    list.add(new ItemUsage(name, qty));
                }
            }
        }

        return list;
    }

}
