#!/bin/bash

echo "Generating LaTeX table rows..."

for dir in *; do
    if [[ -d "$dir" && -f "$dir/overall.txt" ]]; then
        name=$(head -n 1 "$dir/overall.txt")
        successRate=$(grep "Success rate:" "$dir/overall.txt" | awk '{print $3}')
	steps=$(grep "Steps total average:" "$dir/overall.txt" | awk '{print $4}')
        echo "$name & $successRate & $steps \\\\"  # LaTeX row format
    fi
done
