package com.satisfactory_solver.instance;

public class ItemUsage {
    protected String itemName;
    protected double quantityPerMinute;

    public ItemUsage(String itemName, double quantityPerMinute) {
        this.itemName = itemName;
        this.quantityPerMinute = quantityPerMinute;
    }

    public String getItemName() {
        return itemName;
    }
    
    public double getQuantityPerMinute() {
        return quantityPerMinute;
    }

    @Override
    public String toString() {
        return "ItemUsage{" +
                "itemName='" + itemName + '\'' +
                ", quantityPerMinute=" + quantityPerMinute +
                '}';
    }
}
