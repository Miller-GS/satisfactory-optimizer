package com.satisfactory_solver;

import com.satisfactory_solver.decoder.Decoder;
import com.satisfactory_solver.decoder.Gene;
import com.satisfactory_solver.instance.Instance;
import com.satisfactory_solver.instance.InstanceJsonReader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        Instance instance = readJsonFile("instances/example.json");
        Decoder decoder = new Decoder(instance);
        Map<Gene, Integer> genePositions = decoder.getGenePositions();
        System.out.println("Chromosome length: " + decoder.getChromosomeLength());
        System.out.println("Gene Positions:");
        // Sort by value for better readability
        genePositions.entrySet().stream()
            .sorted(Map.Entry.comparingByValue())
            .forEach(entry -> System.out.println("  " + entry.getKey() + ": " + entry.getValue()));
        List<Double> chromosome = List.of(0.0, 0.1); // Example chromosome values
        var decodedSolution = decoder.decode(chromosome);
        System.out.println("Decoded Recipe Usages:");
        for (Map.Entry<String, Double> entry : decodedSolution.getRecipeUsages().entrySet()) {
            System.out.println("  " + entry.getKey() + ": " + entry.getValue());
        }
        var itemLiquidDemand = decodedSolution.getItemLiquidDemand();
        System.out.println("Item Liquid Demand:");
        for (Map.Entry<String, Double> entry : itemLiquidDemand.entrySet()) {
            System.out.println("  " + entry.getKey() + ": " + entry.getValue());
        }
        System.out.println("Unsatisfied Demand Sum: " + decodedSolution.getUnsatisfiedDemandSum());
        System.out.println("Number of Used Machines: " + decodedSolution.getNumberOfUsedMachines());
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
}