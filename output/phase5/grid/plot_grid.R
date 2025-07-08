#!/usr/bin/env Rscript

library(ggplot2)

args <- commandArgs(trailingOnly=TRUE)

# Check if correct number of arguments is provided
if (length(args) < 2) {
  stop("Usage: script.R <path_to_data> <label>")
}

# Read data
file_path <- args[1]
data <- read.csv(file_path, header = FALSE, col.names = c("startTemperature", "stopTemperature", "success"))

# Create plot
ggplot(data, aes(x = startTemperature, y = stopTemperature, color = success)) +
  geom_point(size = 3) +
  scale_color_gradient(low = "blue", high = "red") +
  labs(x = "Start temperature", y = "Stop temperature", color = args[2]) +
  theme_minimal()
