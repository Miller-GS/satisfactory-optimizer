package com.satisfactory_solver;

import com.satisfactory_solver.instance.Instance;
import com.satisfactory_solver.instance.InstanceJsonReader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Main {
    public static void main(String[] args) {
        Instance instance = readJsonFile("instances/example.json");
        System.out.println(instance);
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