package solver
import os.{Path, Shellable}

/** Input into the solver */
case class CoolingProfile(
  startTemperature: Double,
  endTemperature: Double,
  coolingRate: Double,
  equilibrium: Int,
  maxIterations: Int = 0,
  iterationsWithoutChange: Int = 0,
  iterationsWithoutGain: Int = 0
)

trait CoolingResult:
  def toString: String

/** Standard result */
case class StandardCoolingResult(weight: Int, variables: String)
    extends CoolingResult:
  override def toString: String = s"$weight $variables"

/** Extended result */
case class ExtendedCoolingResult(
  standard: StandardCoolingResult,
  stopCause: StopCause,
  isSatisfied: Boolean,
  satisfiedCount: Int,
  stepsTotal: Int,
  stepsSinceChange: Int,
  stepsSinceGain: Int
) extends CoolingResult:
  override def toString: String = standard.toString

/** Wraps around a solver executable and provides its functionality */
class SimulatedCoolingSolver(programPath: Path):
  def run(
    instance: Path,
    seed: String,
    cooling: CoolingProfile,
    debugPath: Option[Path] = None
  ): StandardCoolingResult =
    val result: Array[String] = os.call(
      cmd = (
        programPath,
        s"-f $instance",
        s"-s $seed",
        s"-t ${cooling.startTemperature}",
        s"-T ${cooling.endTemperature}",
        s"-e ${cooling.equilibrium}",
        s"-c ${cooling.coolingRate}",
        s"-i ${cooling.maxIterations}",
        s"-w ${cooling.iterationsWithoutChange}",
        s"-W ${cooling.iterationsWithoutGain}",
        debugPath.map(path => s"-d $path").getOrElse("")
      ),
      check = false
    ).out.text().split(' ').map(
      _.trim
    )
    StandardCoolingResult(result(1).toInt, result.drop(2).mkString(" "))
  end run

  def runExtended(
    instance: Path,
    seed: String,
    cooling: CoolingProfile,
    debugPath: Option[Path] = None
  ): ExtendedCoolingResult =
    val result: Vector[String] = os.call(
      cmd = (
        programPath,
        s"-f $instance",
        s"-s 0x$seed",
        s"-t ${cooling.startTemperature}",
        s"-T ${cooling.endTemperature}",
        s"-e ${cooling.equilibrium}",
        s"-c ${cooling.coolingRate}",
        s"-i ${cooling.maxIterations}",
        s"-w ${cooling.iterationsWithoutChange}",
        s"-W ${cooling.iterationsWithoutGain}",
        s"${debugPath.map(path => s"-d $path").getOrElse("")}"
          + "-E 1"
      ),
      check = true
    ).out.lines()
    val firstLine: Array[String] = result.head.split(' ').map(_.trim)
    val secondLine: Array[String] = result(1).split(' ').map(_.trim)

    ExtendedCoolingResult(
      StandardCoolingResult(
        firstLine(1).toInt,
        firstLine.drop(2).mkString(" ")
      ),
      StopCause.fromString(secondLine(0)),
      secondLine(1).toInt == 1,
      secondLine(2).toInt,
      secondLine(3).toInt,
      secondLine(4).toInt,
      secondLine(5).toInt
    )
  end runExtended
end SimulatedCoolingSolver

/** Stop cause enumeration */
enum StopCause:
  case MaxIterations
  case NoChange
  case NoGain
  case Unknown
  case Temperature
end StopCause

object StopCause:
  def fromString(string: String): StopCause = string match
    case "max"         => MaxIterations
    case "change"      => NoChange
    case "gain"        => NoGain
    case "unknown"     => Unknown
    case "temperature" => Temperature
    case _             => Unknown
end StopCause
