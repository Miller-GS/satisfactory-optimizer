package com.satisfactory_solver.decoder;

public class Gene {
    private String itemName;
    private String recipeName;

    public Gene(String itemName, String recipeName) {
        this.itemName = itemName;
        this.recipeName = recipeName;
    }

    public String getItemName() {
        return itemName;
    }

    public String getRecipeName() {
        return recipeName;
    }

    public String toString() {
        return "Gene(item=" + itemName + ", recipe=" + recipeName + ")";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Gene other = (Gene) obj;
        return itemName.equals(other.itemName) && recipeName.equals(other.recipeName);
    }

    @Override
    public int hashCode() {
        return itemName.hashCode() * 31 + recipeName.hashCode();
    }
}
