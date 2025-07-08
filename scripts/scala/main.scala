//> using dep com.lihaoyi::os-lib:0.11.3
//> using dep com.lihaoyi::mainargs:0.7.6
//> using dep io.github.pityka::nspl-awt:0.10.0
//> using dep org.scalanlp::breeze:2.1.0

//> using file mwsat.scala
//> using file estimator.scala
//> using file solver.scala
//> using file visualize.scala
//> using file runner.scala
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.*
import estimator.estimateInstance
import os.Path
import mainargs.{Flag, ParserForMethods, TokensReader, arg, main}
import mwsat.{Instance, Suite}
import org.nspl.*
import org.nspl.awtrenderer.*
import org.nspl.data.DataMatrix
import solver.{
  CoolingProfile,
  ExtendedCoolingResult,
  SimulatedCoolingSolver,
  StandardCoolingResult
}
import visualization.visualizeRun

import scala.util.Random
import scala.math.BigDecimal.double2bigDecimal

implicit object PathReader extends TokensReader.Simple[Path]:
  override def shortName = "path"

  override def read(strings: Seq[String]): Either[String, Path] =
    Right(Path(strings.head, os.pwd))
end PathReader

@main(doc =
  "Estimates the cooling schedule for a given instance. Same estimates used in runWithEstimator"
)
def estimate(instance: Path): Unit =
  val ins = Instance(instance)
  val estimate = estimateInstance(ins.variable, ins.clauses)
  println(estimate);

end estimate

@main(doc =
  "Visualizes solver debug output - both satisfiability and weight progression"
)
def visualize(
  @arg(
    short = 'f',
    doc =
      "Expects <stepsTotal> <satisfied> <weight> <bestWeight> on each line per step"
  )
  debugFile: Path,
  @arg(short = 's', doc = "Satisfiability graph output file")
  satisfiability: Path = os.pwd / "satisfiability.png",
  @arg(short = 'w', doc = "Weight graph output file")
  weight: Path = os.pwd / "weight.png",
  @arg(short = 'o', doc = "Optimum weight")
  optimum: Option[Int] = None,
  @arg(short = 'c', doc = "NUmber of clauses overall")
  clauses: Option[Int] = None
): Unit =
  require(os.isFile(debugFile), s"File $debugFile does not exist")
  visualizeRun(debugFile, satisfiability, weight, optimum, clauses)
end visualize

@main(doc =
  "Runs the solver on all suites in a given dir and writes results to outputDir"
)
def run(
  @arg(doc = "Seed for java.Random")
  seed: String = "0xFF",
  @arg(short = 'r', doc = "Repeat count - how many times to run each instance")
  runCount: Int = 5,
  @arg(short = 's', doc = "Solver")
  solver: Path,
  @arg(short = 'd', doc = "Directory with instance folders and optimum files")
  dir: Path,
  @arg(short = 'o', doc = "Directory to write result to")
  outputDir: Path,
  @arg(short = 't')
  startTemperature: Double = 20,
  @arg(short = 'T')
  endTemperature: Double = 0.5,
  @arg(short = 'c')
  coolingRate: Double = 0.95,
  @arg(short = 'e')
  equilibrium: Int = 200,
  @arg(short = 'i')
  maxIterations: Int = 0,
  @arg(short = 'w')
  withoutChange: Int = 0,
  @arg(short = 'W')
  withoutGain: Int = 0,
  @arg(short = 'E')
  extended: Flag = Flag(true)
): Unit =
  val profiler: Suite => CoolingProfile =
    _ =>
      CoolingProfile(
        startTemperature,
        endTemperature,
        coolingRate,
        equilibrium,
        maxIterations,
        withoutChange,
        withoutGain
      )
  runner(solver, dir, seed, profiler, runCount, outputDir, extended.value)
end run

@main(doc = "Same as run, but uses CoolingProfile given by estimator")
def runWithEstimator(
  @arg(doc = "Seed for java.Random")
  seed: String = "0xFF",
  @arg(short = 'r', doc = "Repeat count - how many times to run each instance")
  runCount: Int = 5,
  @arg(short = 's', doc = "Solver")
  solver: Path,
  @arg(short = 'd', doc = "Directory with instance folders and optimum files")
  dir: Path,
  @arg(short = 'o', doc = "Directory to write result to")
  outputDir: Path,
  @arg(short = 'e', doc = "Extended cooling results")
  extended: Flag = Flag(true)
): Unit =
  val profiler: Suite => CoolingProfile =
    suite => estimateInstance(suite.variables, suite.clauses)
  runner(solver, dir, seed, profiler, runCount, outputDir, extended.value)
end runWithEstimator

@main(doc =
  "Exports into data.out files triplets of <instance> <weight> <successRate> per line"
)
def gridSearch(
  @arg(short = 's', doc = "Solver")
  solverPath: Path,
  @arg(short = 'R', doc = "Seed")
  seed: String,
  @arg(short = 't', doc = "Initial temperature range e.g. 0.2-20-0.1 or 20.")
  initialTemperatureRange: String,
  @arg(short = 'T', doc = "Stop temperature range e.g. 0.01-2-0.05 or 20.")
  stopTemperatureRange: String,
  @arg(short = 'c', doc = "Cooling coefficient range e.g. 0.8-0.99-0.01 or 20.")
  coolingCoefficientRange: String,
  @arg(short = 'e', doc = "How many times to repeat the inner loop")
  equilibrium: Int,
  @arg(short = 'f', doc = "Name of input file")
  input: Path,
  @arg(short = 'r', doc = "How many times to run the instance")
  runCount: Int = 5,
  @arg(short = 'O', doc = "Expected optimum")
  optimum: Int,
  @arg(short = 'i', doc = "Max iterations before end")
  maxIterations: Int = 0,
  @arg(short = 'w', doc = "Iterations without change before end")
  withoutChange: Int = 0,
  @arg(short = 'W', doc = "Iterations without improvement before end")
  withoutGain: Int = 0,
  @arg(short = 'x', doc = "Output file")
  stepsOutput: Path,
  @arg(short = 'X', doc = "Success output")
  successOutput: Path
): Unit =
  val random = Random(java.lang.Long.parseLong(seed.split('x')(1), 16))

  def parseRange(range: String): Iterable[BigDecimal] =
    range.split('-') match
      case Array(singleValue) =>
        Iterable(BigDecimal(singleValue))
      case Array(start, end, step) =>
        BigDecimal(start) to BigDecimal(end) by BigDecimal(step)
      case _ =>
        throw new IllegalArgumentException(s"Invalid range format: $range")

  val temperatures = parseRange(initialTemperatureRange)
  val stopTemperatures = parseRange(stopTemperatureRange)
  val coolingCoefficients = parseRange(coolingCoefficientRange)

  val profiles =
    for
      temperature <- temperatures
      stopTemperature <- stopTemperatures
      coolingCoefficient <- coolingCoefficients
    yield CoolingProfile(
      temperature.toDouble,
      stopTemperature.toDouble,
      coolingCoefficient.toDouble,
      equilibrium,
      maxIterations,
      withoutChange,
      withoutGain
    )
  println(s"Checking ${profiles.size} profiles")
  case class GridResult(
    profile: CoolingProfile,
    runs: Seq[ExtendedCoolingResult]
  ):
    def successRate: Double =
      runs.map(run =>
        run.isSatisfied && run.standard.weight == optimum
      ).count(identity).toDouble / runs.size

    def averageSteps: Double =
      runs.map(_.stepsTotal).sum.toDouble / runs.size
  end GridResult

  val solver = SimulatedCoolingSolver(solverPath)
  val results: Seq[GridResult] =
    Await.result(
      Future.sequence(
        profiles.toSeq.map { profile =>
          Future {
            val runs = Seq.fill(runCount)(
              solver.runExtended(
                input,
                random.nextLong().toHexString,
                profile
              )
            )

            val res = GridResult(profile, runs)
            println(
              s"(${profile.startTemperature}, ${profile.coolingRate}, ${profile.endTemperature}) done with ${res.averageSteps}"
            )
            res
          }
        }
      ),
      Duration.Inf
    )

  val success = results.map(res =>
    val profile = res.profile
    (
      profile.startTemperature,
      profile.coolingRate,
      profile.endTemperature,
      res.successRate
    )
  )

  val steps = results.map(res =>
    val profile = res.profile
    (
      profile.startTemperature,
      profile.coolingRate,
      profile.endTemperature,
      res.averageSteps
    )
  )

  os.write.over(
    successOutput,
    success.map(s => s"${s._1},${s._2},${s._3},${s._4}").mkString("\n") + "\n"
  )

  os.write.over(
    stepsOutput,
    steps.map(s => s"${s._1},${s._2},${s._3},${s._4}").mkString("\n") + "\n"
  )
end gridSearch

def main(args: Array[String]): Unit = ParserForMethods(this).runOrExit(args)
