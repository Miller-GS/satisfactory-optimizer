import argparse
import json
import random
import os


class Item:
    def __init__(self, name: str, quantity_per_min: float):
        self.name = name
        self.quantity_per_min = quantity_per_min

    def to_dict(self):
        return {
            "name": self.name,
            "quantity_per_min": self.quantity_per_min,
        }

class Recipe:
    def __init__(self, name: str, inputs: list[Item], outputs: list[Item]):
        self.name = name
        self.inputs = inputs
        self.outputs = outputs

    def to_dict(self):
        return {
            "name": self.name,
            "inputs": [item.to_dict() for item in self.inputs],
            "outputs": [item.to_dict() for item in self.outputs],
        }


def main():
    parser = argparse.ArgumentParser(description="Instance Generator")
    parser.add_argument("--items", type=int, required=True, help="Number of items to include in the instance")
    parser.add_argument("--input-items", type=int, required=True, help="Number of input items to include in the instance")
    parser.add_argument("--output-items", type=int, required=True, help="Number of output items to include in the instance")
    parser.add_argument("--recipes", type=int, required=True, help="Number of recipes to include in the instance")
    parser.add_argument("--seed", type=int, required=True, help="Random seed for instance generation")
    parser.add_argument("--outdir", type=str, required=False, default="instances/", help="Output directory for instances")
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
        "recipes": [recipe.to_dict() for recipe in generate_recipes(items, input_items, output_items, recipes, seed)],
        "available_inputs": [generate_item_with_random_quantity(f"Item_{i}", multiplier=10.0).to_dict() for i in range(input_items)],
        "desired_outputs": [generate_item_with_random_quantity(f"Item_{i}", multiplier=2.0).to_dict() for i in range(items - output_items, items)],
    }
    with open(os.path.join(outdir, f"{instance_name}.json"), 'w', encoding='utf-8') as f:
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
    if input_items + output_items > items:
        raise ValueError("The sum of input_items and output_items cannot exceed total items.")
    if recipes < 1 or items < 1:
        return []

    random.seed(seed)

    # Create a random topological order of items and use it to prevent cycles.
    # We'll assign each item a position in a linear order and only allow
    # recipes that consume items with strictly lower positions than the
    # items they produce. Also place available inputs at the low end and
    # desired outputs at the high end to make DAG construction straightforward.
    all_item_names = [f"Item_{i}" for i in range(items)]
    ordering = all_item_names.copy()
    random.shuffle(ordering)
    position = {name: idx for idx, name in enumerate(ordering)}

    # Choose available inputs from the lowest-position items and desired
    # outputs from the highest-position items. This guarantees that inputs
    # precede outputs in the topological order.
    available_input_names: set[str] = set(ordering[:input_items]) if input_items > 0 else set()
    desired_output_names: set[str] = set(ordering[-output_items:]) if output_items > 0 else set()

    intermediate_names: set[str] = set(ordering) - available_input_names - desired_output_names
    must_be_produced_names: set[str] = set(ordering) - available_input_names

    safe_to_use_as_input_names: set[str] = available_input_names.copy()
    produced_items_names: set[str] = set()

    generated_recipes: list[Recipe] = []
    recipe_index = 1

    min_inputs = 1
    max_inputs = min(4, max(1, items // 2))
    min_outputs = 1
    max_outputs = min(4, max(1, items // 2))

    # A. Guarantee all must-be-produced items are produced at least once
    # Process items in increasing topological position so inputs exist when needed.
    production_priority = sorted(list(must_be_produced_names), key=lambda n: position[n])

    for output_name in production_priority:
        if output_name in produced_items_names:
            continue

        # Choose inputs only from items with lower position than the chosen output
        candidate_inputs = [n for n in safe_to_use_as_input_names if position[n] < position[output_name]]
        if not candidate_inputs:
            # No valid earlier inputs available; skip producing this item now.
            # It may be produced later as other items become available.
            continue

        num_inputs = random.randint(min_inputs, max_inputs)
        inputs_names = random.sample(candidate_inputs, min(num_inputs, len(candidate_inputs)))
        input_items_list = [generate_item_with_random_quantity(name) for name in inputs_names]

        # Secondary outputs must also be strictly after all inputs to avoid cycles.
        max_input_pos = max(position[n] for n in inputs_names) if inputs_names else -1
        num_secondary_outputs = random.randint(0, max_outputs - 1)
        secondary_output_candidates = [n for n in (must_be_produced_names - produced_items_names)
                                       if n not in inputs_names and n != output_name and position[n] > max_input_pos]

        secondary_output_names = random.sample(secondary_output_candidates, min(num_secondary_outputs, len(secondary_output_candidates)))

        all_output_names = [output_name] + secondary_output_names
        output_items_list = [generate_item_with_random_quantity(name) for name in all_output_names]

        recipe_name = f"Recipe_{recipe_index}"
        new_recipe = Recipe(recipe_name, input_items_list, output_items_list)
        generated_recipes.append(new_recipe)
        recipe_index += 1

        produced_items_names.update(all_output_names)
        safe_to_use_as_input_names.update(all_output_names)

    # B. Generate remaining recipes while preserving the topological constraint
    all_available_names = set(ordering)
    remaining_recipes = recipes - len(generated_recipes)

    while remaining_recipes > 0:
        candidate_inputs = [n for n in safe_to_use_as_input_names]
        if not candidate_inputs:
            break

        num_inputs = random.randint(min_inputs, max_inputs)
        inputs_names = random.sample(candidate_inputs, min(num_inputs, len(candidate_inputs)))
        input_items_list = [generate_item_with_random_quantity(name) for name in inputs_names]

        # Only allow outputs at positions strictly greater than the max input position
        max_input_pos = max(position[n] for n in inputs_names) if inputs_names else -1
        output_candidates = [n for n in all_available_names if n not in inputs_names and position[n] > max_input_pos]
        if not output_candidates:
            # Can't create an acyclic recipe from these inputs; skip this extra recipe
            continue

        num_outputs = random.randint(min_outputs, max_outputs)
        output_names = random.sample(output_candidates, min(num_outputs, len(output_candidates)))
        output_items_list = [generate_item_with_random_quantity(name) for name in output_names]

        recipe_name = f"Alternate: Recipe_{recipe_index}"
        new_recipe = Recipe(recipe_name, input_items_list, output_items_list)
        generated_recipes.append(new_recipe)
        recipe_index += 1

        safe_to_use_as_input_names.update(output_names)
        remaining_recipes -= 1

    return generated_recipes

def generate_item_with_random_quantity(name: str, multiplier: float = 1.0) -> Item:
    quantity_per_min = random.uniform(0.1, 10.0) * multiplier
    return Item(name, quantity_per_min)


if __name__=="__main__":
    main()
    