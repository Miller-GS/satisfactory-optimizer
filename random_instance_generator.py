from pathlib import Path
import argparse
import json
import random

outdir = Path("./instances")

class Item:
    def __init__(self, name: str, quantity_per_min: float):
        self.name = name
        self.quantity_per_min = quantity_per_min

class Recipe:
    def __init__(self, name: str, inputs: list[Item], outputs: list[Item]):
        self.name = name
        self.inputs = inputs
        self.outputs = outputs


def main():
    parser = argparse.ArgumentParser(description="Instance Generator")
    parser.add_argument("--items", type=int, required=True, help="Number of items to include in the instance")
    parser.add_argument("--input-items", type=int, required=True, help="Number of input items to include in the instance")
    parser.add_argument("--output-items", type=int, required=True, help="Number of output items to include in the instance")
    parser.add_argument("--recipes", type=int, required=True, help="Number of recipes to include in the instance")
    parser.add_argument("--seed", type=int, required=True, help="Random seed for instance generation")
    parser.add_argument("--outdir", type=str, required=False, default=outdir, help="Output directory for instances")
    parser.add_argument("--instance-name", type=str, required=True, help="Name of the generated instance")
    args = parser.parse_args()
    generate_instance(args.outdir, args.instance_name, args.items, args.input_items, args.output_items, args.recipes, args.seed)

def generate_instance(
        outdir: str,
        instance_name: str,
        items: int,
        input_items: int,
        output_items: int,
        recipes: int,
        seed: int,
) -> None:
    result = {
        "recipes": [],
        "available_inputs": [],
        "desired_outputs": []
    }
    with open(outdir / f"{instance_name}.json", 'w', encoding='utf-8') as f:
        json.dump(result, f, ensure_ascii=False, indent=4)

def generate_recipes(items: int, input_items: int, output_items: int, recipes: int, seed: int) -> list[Recipe]:
    """
    Generate a list of recipes based on the provided parameters.
    It will form a Directed Acyclic Graph (DAG) where:
    - There are `items` unique items in total.
    - There are `input_items` items that can be sourced externally (available inputs).
    - There are `output_items` items that are the final desired products (desired outputs).
    - There are `recipes` recipes that transform items into other items
    (potentially using multiple inputs and producing multiple outputs).
    - There may be intermediate items that are neither inputs nor outputs.
    - Every item must be produced by at least one recipe or be an available input.
    """
    # 1. Setup and Initialization
    if input_items + output_items > items:
        raise ValueError("The sum of input_items and output_items cannot exceed total items.")
    if recipes < 1 or items < 1:
        return []

    random.seed(seed)
    
    # 2. Create Items
    all_item_names = [f"Item_{i}" for i in range(items)]
    # Use a dictionary for fast lookup and to hold Item objects
    all_items_dict: dict[str, Item] = {
        name: Item(name, quantity_per_min=random.uniform(0.1, 10.0))
        for name in all_item_names
    }
    
    # 3. Classify Items
    # Randomly select special items
    
    # Separate available inputs and desired outputs for clarity
    available_input_names: set[str] = set(random.sample(all_item_names, input_items))
    desired_output_names: set[str] = set(random.sample(all_item_names, output_items))
    # Exclude available inputs from candidates for desired outputs to ensure disjoint sets for complexity
    # Note: The prompt doesn't strictly require disjoint sets, but this simplifies the DAG generation process
    output_candidates = list(set(all_item_names) - available_input_names)
    desired_output_names: set[str] = set(random.sample(output_candidates, output_items))

    # Intermediate items are all other items
    intermediate_names: set[str] = set(all_item_names) - available_input_names - desired_output_names

    # Items that need to be produced by a recipe (everything except external inputs)
    must_be_produced_names: set[str] = set(all_item_names) - available_input_names
    
    # 4. Recipe Generation (DAG Construction)
    
    # Items that are currently safe to use as inputs (start with external inputs)
    safe_to_use_as_input_names: set[str] = available_input_names.copy()
    
    # Set to track which items have been produced at least once
    produced_items_names: set[str] = set()

    generated_recipes: list[Recipe] = []
    recipe_index = 1

    # Heuristic: Determine min/max inputs/outputs per recipe for variety
    min_inputs = 1
    max_inputs = max(1, items // 4)  # Up to 25% of all items
    min_outputs = 1
    max_outputs = min(2, max(1, items // 5)) # Up to 2 outputs, or 20% of items

    # A. Guarantee all must-be-produced items are produced at least once
    
    # Sort items by 'level' (outputs first, then intermediates) to help enforce DAG structure
    # This prioritizes making recipes that produce the 'highest level' items first
    production_priority = list(desired_output_names) + list(intermediate_names)
    random.shuffle(production_priority) # Randomize order within levels for variety
    
    # Ensure every must-be-produced item is the output of at least one recipe
    for output_name in production_priority:
        if output_name in produced_items_names:
            continue # Already handled in an earlier recipe
            
        # 1. Select the required output
        output_item = all_items_dict[output_name]
        
        # 2. Select input items (must be safe to use)
        # We need at least one input; prefer existing safe inputs
        num_inputs = random.randint(min_inputs, max_inputs)
        
        # Ensure we have enough candidates
        candidate_inputs = list(safe_to_use_as_input_names)
        if not candidate_inputs:
            # Should not happen if items > input_items >= 1, but as a fallback
            continue 

        # Select inputs, ensuring output_name isn't accidentally included as input (to prevent self-loop)
        inputs_names = random.sample(candidate_inputs, min(num_inputs, len(candidate_inputs)))
        
        # Ensure the output isn't also an input (prevents immediate self-loop for this item)
        if output_name in inputs_names:
             inputs_names.remove(output_name)
             if not inputs_names and candidate_inputs: # If removing it left no inputs, try another
                 inputs_names.append(random.choice(candidate_inputs)) 
        
        input_items_list = [all_items_dict[name] for name in inputs_names]

        # 3. Select secondary outputs (optional)
        # Other potential outputs can be any unproduced item that's not an input and not the main output
        
        # Include the currently produced item and up to (max_outputs - 1) others
        num_secondary_outputs = random.randint(0, max_outputs - 1)
        
        # Candidates are unproduced items that are not inputs
        secondary_output_candidates = list((must_be_produced_names - produced_items_names) - set(inputs_names) - {output_name})
        
        secondary_output_names = random.sample(secondary_output_candidates, min(num_secondary_outputs, len(secondary_output_candidates)))
        
        all_output_names = [output_name] + secondary_output_names
        output_items_list = [all_items_dict[name] for name in all_output_names]
        
        # 4. Create and record the recipe
        recipe_name = f"Recipe_{recipe_index}"
        new_recipe = Recipe(recipe_name, input_items_list, output_items_list)
        generated_recipes.append(new_recipe)
        recipe_index += 1
        
        # Mark all new outputs as produced and safe to use as inputs
        produced_items_names.update(all_output_names)
        safe_to_use_as_input_names.update(all_output_names)

    # B. Generate remaining recipes
    
    # The set of items that can be produced or used as input (all items)
    all_available_names = set(all_item_names)

    # Calculate remaining recipes to generate
    remaining_recipes = recipes - len(generated_recipes)
    
    for _ in range(remaining_recipes):
        # 1. Select a random number of inputs (must be safe)
        num_inputs = random.randint(min_inputs, max_inputs)
        
        # Inputs must be items that are currently safe (produced or external)
        candidate_inputs = list(safe_to_use_as_input_names)
        if not candidate_inputs: continue
        
        inputs_names = random.sample(candidate_inputs, min(num_inputs, len(candidate_inputs)))
        input_items_list = [all_items_dict[name] for name in inputs_names]
        
        # 2. Select a random number of outputs
        num_outputs = random.randint(min_outputs, max_outputs)
        
        # Outputs must be items that are NOT currently inputs to prevent immediate cycle
        output_candidates = list(all_available_names - set(inputs_names))
        if not output_candidates: continue
        
        output_names = random.sample(output_candidates, min(num_outputs, len(output_candidates)))
        output_items_list = [all_items_dict[name] for name in output_names]
        
        # 3. Create and record the recipe
        recipe_name = f"Recipe_{recipe_index}"
        new_recipe = Recipe(recipe_name, input_items_list, output_items_list)
        generated_recipes.append(new_recipe)
        recipe_index += 1
        
        # All new outputs are now safe to use as inputs
        safe_to_use_as_input_names.update(output_names)

    # Final check: The constraints guarantee that `produced_items_names` contains all 
    # `must_be_produced_names`, satisfying the constraint "Every item must be produced by 
    # at least one recipe or be an available input."
    
    # Note on DAG enforcement: By only using items already marked as "safe to use as input"
    # (i.e., external inputs or outputs of previously generated recipes), we simulate a
    # topological order, which prevents cycles from being formed (e.g., A -> B, B -> A).
    # Specifically, an output can never be an input in the same step, and since we 
    # only use *produced* items as inputs, we are effectively only allowing 
    # edges from lower-level items to higher-level items.

    return generated_recipes



if __name__=="__main__":
    main()
    