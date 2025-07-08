#!/bin/bash

echo "Generating LaTeX table rows..."

for dir in *; do
    if [[ -d "$dir" && -f "$dir/overall.txt" ]]; then
        name=$(head -n 1 "$dir/overall.txt")
        successRate=$(grep "Success rate:" "$dir/overall.txt" | awk '{print $3}')
        echo "$name & $successRate \\\"  # LaTeX row format
    fi
done
