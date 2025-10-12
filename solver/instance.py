class Instance:
    def __init__(self, recipes: list['Recipe'], available_inputs: list['Item'], desired_outputs: list['Item']):
        self.recipes = recipes
        self.available_inputs = available_inputs
        self.desired_outputs = desired_outputs
        self.all_components = self._get_all_components()

    def _get_all_components(self) -> set[str]:
        components = set()
        for recipe in self.recipes:
            for item in recipe.inputs + recipe.outputs:
                components.add(item.name)
        for item in self.available_inputs + self.desired_outputs:
            components.add(item.name)
        return components
    
    @staticmethod
    def from_file(file_path: str) -> 'Instance':
        import json
        with open(file_path, 'r') as f:
            data = json.load(f)
        
        recipes = []
        for recipe_data in data['recipes']:
            inputs = [Item(item['name'], item['quantity_per_min']) for item in recipe_data['inputs']]
            outputs = [Item(item['name'], item['quantity_per_min']) for item in recipe_data['outputs']]
            recipe = Recipe(recipe_data['name'], inputs, outputs)
            recipes.append(recipe)

        available_inputs = [Item(item['name'], item['quantity_per_min']) for item in data['available_inputs']]
        desired_outputs = [Item(item['name'], item['quantity_per_min']) for item in data['desired_outputs']]

        return Instance(recipes, available_inputs, desired_outputs)

class Item:
    def __init__(self, name: str, quantity_per_min: float):
        self.name = name
        self.quantity_per_min = quantity_per_min

class Recipe:
    def __init__(self, name: str, inputs: list[Item], outputs: list[Item]):
        self.name = name
        self.inputs = inputs
        self.outputs = outputs

        self.input_rates_per_minute = self._get_input_rates_per_minute()
        self.output_rates_per_minute = self._get_output_rates_per_minute()

    def _get_input_rates_per_minute(self) -> dict[str, float]:
        return {item.name: item.quantity_per_min for item in self.inputs}

    def _get_output_rates_per_minute(self) -> dict[str, float]:
        return {item.name: item.quantity_per_min for item in self.outputs}