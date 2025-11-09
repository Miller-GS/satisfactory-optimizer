import argparse
import logging
import gurobipy as gp
from pathlib import Path
from gurobipy import GRB
from instance import Instance


def main() -> None:
    args = parse_args()
    instance = Instance.from_file(args.input)
    if args.verbose:
        print("Instance loaded:")
        print(instance)

    instance_name = Path(args.input).stem
    logger, env = setup_logger(args.outdir, instance_name)

    model = build_model(instance, env)

    model.setParam(GRB.Param.TimeLimit, 3600)  # 1 hour time limit
    model.optimize()
    
    # Print the solution nicely
    print_solution(model, instance, logger)



def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Satisfactory Solver")
    parser.add_argument("--input", type=str, required=True, help="Input file of the instance")
    parser.add_argument("--outdir", type=str, required=True, help="Saves gurobi log into outdir")
    parser.add_argument("-v", "--verbose", action="store_true", help="Enable verbose output")
    return parser.parse_args()

def setup_logger(outdir: str, instance_name: str) -> logging.Logger:
    Path(outdir).mkdir(parents=True, exist_ok=True)
    log_path =f"{outdir}/{instance_name}.log"
    logging.basicConfig(
        level=logging.INFO,
        filename=Path(log_path),
        encoding="utf-8",
        filemode="a",
        format="%(asctime)s %(levelname)s %(message)s",
    )
    env = gp.Env(logfilename=log_path, params={"LogToConsole": 0})
    return logging.getLogger(), env

def build_model(instance: Instance, env:gp.Env) -> tuple[gp.Model, gp.tupledict[int, gp.Var]]:
    model = gp.Model("Satisfactory Optimizer", env=env)
    recipe_vars = model.addVars(len(instance.recipes), vtype=GRB.CONTINUOUS, name="UseRecipe")
    n_machine_vars = model.addVars(len(instance.recipes), vtype=GRB.INTEGER, name="NumMachines")
    # Objective: Minimize total number of machines used
    model.setObjective(n_machine_vars.sum(), GRB.MINIMIZE)
    # Constraints: Ensure desired outputs are met
    for desired in instance.desired_outputs:
        model.addConstr(
            gp.quicksum(
                recipe_vars[i] * (
                    sum(output_rate for output_name, output_rate in recipe.output_rates_per_minute.items() if output_name == desired.name)
                    - sum(input_rate for input_name, input_rate in recipe.input_rates_per_minute.items() if input_name == desired.name)
                )
                for i, recipe in enumerate(instance.recipes)
            ) >= desired.quantity_per_min,
            name=f"Demand_{desired.name}"
        )

    # Constraints: Balance products (production >= consumption + available inputs)
    for component in instance.all_components:
        model.addConstr(
            gp.quicksum(
                recipe_vars[i] * (
                    sum(output_rate for output_name, output_rate in recipe.output_rates_per_minute.items() if output_name == component)
                    - sum(input_rate for input_name, input_rate in recipe.input_rates_per_minute.items() if input_name == component)
                )
                for i, recipe in enumerate(instance.recipes)
            ) >= -sum(item.quantity_per_min for item in instance.available_inputs if item.name == component),
            name=f"Balance_{component}"
        )
    
    # Constraints: Link number of machines to recipe usage
    for i in range(len(instance.recipes)):
        model.addConstr(n_machine_vars[i] >= recipe_vars[i], name=f"MachineLink_{i}")

    return model

def print_solution(model: gp.Model, instance: Instance, logger: logging.Logger) -> None:
    """Print the optimization solution in a nice format."""
    if model.status == GRB.OPTIMAL:
        print("\n" + "="*60)
        print("🎉 OPTIMAL SOLUTION FOUND!")
        print("="*60)
        
        # Get variable values
        recipe_vars = [var for var in model.getVars() if "UseRecipe" in var.varName]
        machine_vars = [var for var in model.getVars() if "NumMachines" in var.varName]
        
        # Print objective value
        print(f"\n📊 OBJECTIVE VALUE: {model.objVal:.2f} total machines")
        
        # Print recipe usage
        print(f"\n🏭 RECIPE USAGE:")
        print("-" * 50)
        total_machines = 0
        for i, (recipe_var, machine_var) in enumerate(zip(recipe_vars, machine_vars)):
            if recipe_var.x > 1e-6:  # Only show non-zero recipes
                recipe = instance.recipes[i]
                machines_used = int(machine_var.x)
                total_machines += machines_used
                print(f"  {recipe.name}:")
                print(f"    • Recipe usage: {recipe_var.x:.3f} units/min")
                print(f"    • Machines needed: {machines_used}")
                
                # Show what this recipe produces and consumes
                if recipe.inputs:
                    inputs_str = ", ".join([f"{item.name}: {item.quantity_per_min * recipe_var.x:.2f}/min" 
                                          for item in recipe.inputs])
                    print(f"    • Consumes: {inputs_str}")
                
                if recipe.outputs:
                    outputs_str = ", ".join([f"{item.name}: {item.quantity_per_min * recipe_var.x:.2f}/min" 
                                           for item in recipe.outputs])
                    print(f"    • Produces: {outputs_str}")
                print()
        
        # Print resource balance
        print(f"🔄 RESOURCE BALANCE:")
        print("-" * 50)
        
        # Calculate net production/consumption for each component
        component_balance = {}
        for component in instance.all_components:
            net_rate = 0
            for i, recipe in enumerate(instance.recipes):
                recipe_usage = recipe_vars[i].x
                if recipe_usage > 1e-6:
                    # Add production
                    for output in recipe.outputs:
                        if output.name == component:
                            net_rate += output.quantity_per_min * recipe_usage  
                    # Subtract consumption
                    for input_item in recipe.inputs:
                        if input_item.name == component:
                            net_rate -= input_item.quantity_per_min * recipe_usage

            # Include available inputs
            for item in instance.available_inputs:
                if item.name == component:
                    net_rate += item.quantity_per_min

            if abs(net_rate) > 1e-6:
                component_balance[component] = net_rate
        
        for component, net_rate in component_balance.items():
            status = "SURPLUS" if net_rate > 0 else "DEFICIT"
            print(f"  {component}: {net_rate:+.2f}/min ({status})")
        
        # Check if desired outputs are met
        print(f"\n✅ DESIRED OUTPUTS CHECK:")
        print("-" * 50)
        for desired in instance.desired_outputs:
            actual_rate = component_balance.get(desired.name, 0)
            if actual_rate >= desired.quantity_per_min - 1e-6:
                status = "✅ MET"
            else:
                status = "❌ NOT MET"
            print(f"  {desired.name}: {actual_rate:.2f}/min (required: {desired.quantity_per_min}/min) {status}")
    
        print(f"\n📋 SUMMARY:")
        print("-" * 50)
        print(f"  Total machines used: {total_machines}")
        print(f"  Optimization status: OPTIMAL")
        print("="*60)
        
        # Log the solution
        logger.info(f"Optimal solution found with {model.objVal:.2f} total machines")
        logger.info(f"Total machines used: {total_machines}")
        
    elif model.status == GRB.INFEASIBLE:
        print("\n❌ PROBLEM IS INFEASIBLE!")
        print("The desired outputs cannot be achieved with the available inputs and recipes.")
        logger.error("Problem is infeasible")
        
    elif model.status == GRB.UNBOUNDED:
        print("\n⚠️  PROBLEM IS UNBOUNDED!")
        print("The objective function can be improved indefinitely.")
        logger.error("Problem is unbounded")
        
    elif model.status == GRB.TIME_LIMIT:
        print("\n⏰ TIME LIMIT REACHED!")
        if model.solCount > 0:
            print(f"Best solution found: {model.objVal:.2f} total machines")
            logger.info(f"Time limit reached. Best solution: {model.objVal:.2f}")
        else:
            print("No feasible solution found within time limit.")
            logger.warning("Time limit reached with no feasible solution")
    else:
        print(f"\n❓ OPTIMIZATION STATUS: {model.status}")
        logger.warning(f"Unexpected optimization status: {model.status}")

if __name__ == "__main__":
    main()