# Fine-tuning Maximal Weighted SAT solver 

> Made as a homework to FIT CTU's combinatorial optimization class. Scored 27/30 points.

The goal of this project is to fine-tune a [MWSAT](https://en.wikipedia.org/wiki/Maximum_satisfiability_problem),
which uses simulated annealing as an advanced heuristic.

## Tech stack
- Scala, with the help of scala-cli and libraries MainArgs, OS-Lib and nspl 
- Shell
- Mathematica

## Project structure
- **kop-du2.pdf** 
  Document describing the fine-tuning in detail in czech
- **scripts** 
  Scripts used for running experiments or processing data.
- **sources**  
  Source code of the solver.
- **input**  
  Contains the problem instances used.
- **output**
    - **phaseN**  
      Results of the N-th white box phase.
    - **black**  
      Results of the black box phase.
- **stat**  
  Contains the Mathematica notebook used to define the estimator.


