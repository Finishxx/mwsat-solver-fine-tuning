package estimator
import solver.CoolingProfile

def estimateInstance(variables: Int, clauses: Int): CoolingProfile =
  CoolingProfile(0.1, 0.001, coolingRate(variables.toDouble), 200)

def coolingRate(n: Double): Double =
  (0.95409 + (0.00284489 * n)
    - (0.000061625 * BigDecimal(n).pow(2))
    + (4.57828 * BigDecimal(10).pow(-7) * BigDecimal(n).pow(3))).toDouble
