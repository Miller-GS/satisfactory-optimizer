import os
import csv
import argparse
import re

def main() -> None:
    parser = argparse.ArgumentParser(description="Process Gurobi log files.")
    parser.add_argument("gurobi_directory", type=str, help="Directory containing Gurobi log files")
    parser.add_argument("genetic_directory", type=str, help="Directory containing Genetic Algorithm log files")
    args = parser.parse_args()

    gurobi_results = read_gurobi_logs_directory(args.gurobi_directory)
    genetic_results = read_genetic_algorithm_logs_directory(args.genetic_directory)
    merged_results = {}
    for instance_name, gurobi_data in gurobi_results.items():
        merged_results[instance_name] = gurobi_data
        if instance_name in genetic_results:
            merged_results[instance_name].update(genetic_results[instance_name])

    with open("execution_results.csv", "w", newline="") as csvfile:    
        fieldnames = ["instance_name", "gurobi_result", "gurobi_dual_bound"] + sorted(
            set().union(*(genetic_results.get(name, {}).keys() for name in merged_results))
        )
        writer = csv.DictWriter(csvfile, fieldnames=fieldnames)
        writer.writeheader()
        for instance_name, data in merged_results.items():
            row = {"instance_name": instance_name}
            row.update(data)
            writer.writerow(row)
    

def read_gurobi_logs_directory(directory: str) -> dict[str, dict[str, float]]:
    results = {}
    for filename in os.listdir(directory):
        if filename.endswith(".log"):
            file_path = os.path.join(directory, filename)
            instance_name = filename[:-4]  # Remove .log extension
            results[instance_name] = read_gurobi_log_file(file_path)

    return results

def read_gurobi_log_file(file_path: str) -> dict[str, float]:
    with open(file_path, "r") as f:
        lines = f.readlines()
    gurobi_result = float(lines[-1].split()[-1].strip())
    dual_bound = float(lines[-3].split("best bound ")[-1].split(",")[0].strip())
    return {"gurobi_result": gurobi_result, "gurobi_dual_bound": dual_bound}

def read_genetic_algorithm_logs_directory(directory: str) -> dict[str, dict[str, float]]:
    results = {}
    for filename in os.listdir(directory):
        if filename.endswith(".log"):
            file_path = os.path.join(directory, filename)
            joined_name = filename.split(".")[0]
            match = re.search(r"(_[a-z])", joined_name)
            snake_case_start = match.group(1)
            strategy_name = joined_name.split(snake_case_start)[0]
            instance_name = joined_name[len(strategy_name)+1:]
            
            if instance_name not in results:
                results[instance_name] = {}
            results[instance_name][strategy_name] = read_genetic_algorithm_log_file(file_path)

    return results

def read_genetic_algorithm_log_file(file_path: str) -> float:
    with open(file_path, "r") as f:
        lines = f.readlines()
    try:
        return float(lines[-1].split()[-1].strip())
    except (ValueError, IndexError):
        return None

if __name__ == "__main__":  
    main()
