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
        "available_inputs": [generate_item_with_random_quantity(f"Item_{i}", multiplier=10.0, addend=100.0).to_dict() for i in range(input_items)],
        "desired_outputs": [generate_item_with_random_quantity(f"Item_{i}", multiplier=2.0).to_dict() for i in range(items - output_items, items)],
    }
    with open(os.path.join(outdir, f"{instance_name}.json"), 'w', encoding='utf-8') as f:
        json.dump(result, f, ensure_ascii=False, indent=4)


def generate_recipes(items: int, input_items: int, output_items: int, recipes: int, seed: int) -> list[Recipe]:
    """
    Generate recipes forming a valid DAG:
      • Every non-input item is produced at least once.
      • Exactly `recipes` recipes are generated.
      • Acyclicity guaranteed via topological positions.
    """
    if input_items + output_items > items:
        raise ValueError("The sum of input_items and output_items cannot exceed total items.")
    if recipes < 1 or items < 1:
        return []

    random.seed(seed)

    # --- Assign topological positions ---
    all_items = [f"Item_{i}" for i in range(items)]
    intermediate_item_names = all_items[input_items:items - output_items]
    ordering = intermediate_item_names.copy()
    random.shuffle(ordering)
    ordering = all_items[:input_items] + ordering + all_items[items - output_items:]
    position = {name: idx for idx, name in enumerate(ordering)}

    inputs_set = set(ordering[:input_items])
    outputs_set = set(ordering[-output_items:])
    must_produce = set(ordering) - inputs_set

    safe_inputs = set(inputs_set)
    produced = set()

    min_in, max_in = 1, min(4, max(1, items // 2))
    min_out, max_out = 1, min(2, max(1, items // 2))

    recipes_list: list[Recipe] = []
    recipe_idx = 1

    # ---------- Phase 1: ensure coverage ----------
    for target in sorted(must_produce, key=lambda x: position[x]):
        if target in produced:
            continue

        # Inputs must come strictly before target
        candidates = [n for n in safe_inputs if position[n] < position[target]]
        if not candidates:
            # fallback: any available input with lower position
            candidates = [n for n in ordering if position[n] < position[target]]
        if not candidates:
            continue

        n_inputs = random.randint(min_in, max_in)
        chosen_inputs = random.sample(candidates, min(n_inputs, len(candidates)))

        max_pos = max(position[n] for n in chosen_inputs)
        possible_outs = [n for n in must_produce | outputs_set
                         if n != target and position[n] > max_pos]
        n_outs = random.randint(1, max_out)  # at least one output (target)
        chosen_outs = [target]
        if possible_outs:
            chosen_outs += random.sample(possible_outs, min(n_outs - 1, len(possible_outs)))

        recipes_list.append(
            Recipe(
                f"Recipe_{recipe_idx}",
                [generate_item_with_random_quantity(i) for i in chosen_inputs],
                [generate_item_with_random_quantity(o) for o in chosen_outs],
            )
        )
        recipe_idx += 1

        safe_inputs.update(chosen_outs)
        produced.update(chosen_outs)

        if len(recipes_list) >= recipes:
            break

    # ---------- Phase 2: fill to reach desired count ----------
    remaining = recipes - len(recipes_list)
    all_names = set(ordering)
    attempts = 0
    max_attempts = recipes * 20

    while remaining > 0 and attempts < max_attempts:
        attempts += 1
        if not safe_inputs:
            safe_inputs.update(inputs_set)

        n_inputs = random.randint(min_in, max_in)
        chosen_inputs = random.sample(list(safe_inputs), min(n_inputs, len(safe_inputs)))
        max_pos = max(position[i] for i in chosen_inputs)

        # Outputs strictly after all inputs
        outs = [n for n in all_names if position[n] > max_pos]
        if not outs:
            continue
        n_outs = random.randint(min_out, max_out)
        chosen_outs = random.sample(outs, min(n_outs, len(outs)))

        recipes_list.append(
            Recipe(
                f"Recipe_{recipe_idx}",
                [generate_item_with_random_quantity(i) for i in chosen_inputs],
                [generate_item_with_random_quantity(o) for o in chosen_outs],
            )
        )
        recipe_idx += 1
        safe_inputs.update(chosen_outs)
        produced.update(chosen_outs)
        remaining -= 1

    # ---------- Fallback if still short ----------
    while len(recipes_list) < recipes:
        a, b = random.sample(ordering, 2)
        if position[a] < position[b]:
            recipes_list.append(
                Recipe(
                    f"Recipe_{recipe_idx}",
                    [generate_item_with_random_quantity(a)],
                    [generate_item_with_random_quantity(b)],
                )
            )
            recipe_idx += 1

    return recipes_list



def generate_item_with_random_quantity(name: str, multiplier: float = 1.0, addend: float = 0.0) -> Item:
    quantity_per_min = random.uniform(0.5, 10.0) * multiplier + addend
    return Item(name, quantity_per_min)



if __name__=="__main__":
    main()
    