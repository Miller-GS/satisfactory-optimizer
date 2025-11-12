package com.satisfactory_solver.decoder;
import java.util.ArrayList;

@SuppressWarnings("serial")
public class Solution<E> extends ArrayList<E> {
	
	public Double cost = Double.POSITIVE_INFINITY;
    public Double infeasibility = Double.POSITIVE_INFINITY;
	
	public Solution() {
		super();
	}
	
	public Solution(Solution<E> sol) {
		super(sol);
		cost = sol.cost;
		infeasibility = sol.infeasibility;  
	}

	@Override
	public String toString() {
		return "Solution: cost=[" + cost + "], infeasibility=[" + infeasibility + "], size=[" + this.size() + "], elements=" + super.toString() + cost;
	}

}

