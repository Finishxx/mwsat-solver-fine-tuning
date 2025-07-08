#!/bin/bash

# Check if a directory is provided as an argument
if [ -z "$1" ]; then
  echo "Usage: $0 <folder>"
  exit 1
fi

folder="$1"

# Ensure the provided folder exists
if [ ! -d "$folder" ]; then
  echo "Directory $folder does not exist."
  exit 1
fi

# Get a list of files in the folder, shuffle them, and remove all but 100 files
find "$folder" -type f | shuf | tail -n +31 | xargs rm -f

echo "Deleted all but 100 random files from $folder."

