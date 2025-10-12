from pathlib import Path
import argparse
import json

outdir = Path("./instances")

# Extraídos daqui: https://www.satisfactorytools.com/1.0/production
DEFAULT_INPUTS = [
    {"name": "Desc_OreIron_C", "quantity_per_min": 92100.0},
    {"name": "Desc_Coal_C", "quantity_per_min": 42300.0},
    {"name": "Desc_Sulfur_C", "quantity_per_min": 10800.0},
    {"name": "Desc_SAM_C", "quantity_per_min": 10200.0},
    {"name": "Desc_OreBauxite_C", "quantity_per_min": 12300.0},
    {"name": "Desc_OreGold_C", "quantity_per_min": 15000.0},
    {"name": "Desc_OreCopper_C", "quantity_per_min": 36900.0},
    {"name": "Desc_RawQuartz_C", "quantity_per_min": 13500.0},
    {"name": "Desc_Stone_C", "quantity_per_min": 69900.0},
    {"name": "Desc_OreUranium_C", "quantity_per_min": 2100.0},
    {"name": "Desc_LiquidOil_C", "quantity_per_min": 12600.0},
    {"name": "Desc_Water_C", "quantity_per_min": 9007199254740991.0},  # Infinite water
    {"name": "Desc_NitrogenGas_C", "quantity_per_min": 12000.0}
]

def clean_name(name: str, data: dict) -> str:
    if name in data['items']:
        return data['items'][name]['name']
    else:
        raise ValueError(f"Item {name} not found in data.json items.")

def main():
    parser = argparse.ArgumentParser(description="Instance Generator")
    parser.add_argument("--data-json", type=str, required=False, default="./data1.0.json", help="Path to data.json")
    parser.add_argument("--outdir", type=str, required=False, default=outdir, help="Output directory for instances")
    parser.add_argument("--instance-name", type=str, required=True, help="Name of the generated instance")
    parser.add_argument("--include-all-raw-inputs", action='store_true', help="Include all raw inputs as available inputs")
    args = parser.parse_args()
    generate_instance(args.data_json, args.outdir, args.instance_name, args.include_all_raw_inputs)

def generate_instance(data_json: str, outdir: str, instance_name: str, include_all_raw_inputs: bool) -> None:
    with open(data_json, 'r', encoding='utf-8') as f:
        data = json.load(f)
    result = {
        "recipes": [],
        "available_inputs": [],
        "desired_outputs": []
    }
    if include_all_raw_inputs:
        result["available_inputs"] = [{"name": clean_name(item['name'], data), "quantity_per_min": item['quantity_per_min']} for item in DEFAULT_INPUTS]
    else:
        result["available_inputs"] = [{"name": clean_name(item['name'], data), "quantity_per_min": 0} for item in DEFAULT_INPUTS]


    for recipe in data['recipes'].values():
        if not recipe.get('inMachine', False):
            continue
        time_in_seconds = recipe['time']
        inputs = [{'name': clean_name(item['item'], data), 'quantity_per_min': item['amount'] * 60.0 / time_in_seconds} for item in recipe['ingredients']]
        outputs = [{'name': clean_name(item['item'], data), 'quantity_per_min': item['amount'] * 60.0 / time_in_seconds} for item in recipe['products']]
        result["recipes"].append({
            'name': recipe['name'],
            'inputs': inputs,
            'outputs': outputs,
        })
    with open(outdir / f"{instance_name}.json", 'w', encoding='utf-8') as f:
        json.dump(result, f, ensure_ascii=False, indent=4)


if __name__=="__main__":
    main()
    