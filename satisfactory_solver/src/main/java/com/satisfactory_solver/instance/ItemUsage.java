package com.satisfactory_solver.instance;

public class ItemUsage {
    protected String itemName;
    protected double quantityPerMinute;
    protected boolean isPrimary = false;

    public ItemUsage(String itemName, double quantityPerMinute, boolean isPrimary) {
        this.itemName = itemName;
        this.quantityPerMinute = quantityPerMinute;
        this.isPrimary = isPrimary;
    }

    public String getItemName() {
        return itemName;
    }
    
    public double getQuantityPerMinute() {
        return quantityPerMinute;
    }

    public boolean isPrimary() {
        return isPrimary;
    }

    @Override
    public String toString() {
        return "ItemUsage{" +
                "itemName='" + itemName + '\'' +
                ", quantityPerMinute=" + quantityPerMinute +
                ", isPrimary=" + isPrimary +
                '}';
    }
}
