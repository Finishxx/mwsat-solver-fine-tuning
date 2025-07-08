import mwsat.{Instance, Suite}
import os.Path
import solver.*

import scala.collection.mutable
import scala.util.Random
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.*

private val runExtension: String = ".out"

// Add a common `results` field to the hierarchy
sealed trait InstanceResults:
  def instance: Instance
  def results: Seq[CoolingResult]
  def seeds: Seq[String]

case class StandardInstanceResults(
  instance: Instance,
  results: Seq[StandardCoolingResult],
  seeds: Seq[String]
) extends InstanceResults

case class ExtendedInstanceResults(
  instance: Instance,
  results: Seq[ExtendedCoolingResult],
  seeds: Seq[String]
) extends InstanceResults

/** Runs and exports suites */
def runner(
  solverPath: Path,
  suiteDir: Path,
  seed: String,
  profiler: Suite => CoolingProfile,
  runCount: Int,
  outputDir: Path,
  isExtended: Boolean
): Unit =
  val solver = SimulatedCoolingSolver(solverPath)
  val suites: Seq[Suite] = os.list(suiteDir).filter(os.isDir).map(Suite(_))
  val random = Random(java.lang.Long.parseLong(seed.split('x')(1), 16))

  for suite: Suite <- suites do
    val profile = profiler(suite)
    println(s"Profiling $profile")
    val results = runSuite(solver, suite, random, runCount, profile, isExtended)
    exportRuns(results, outputDir / suite.toString / "runs")
    exportSummary(results, suite, outputDir / suite.toString / "summaries")
  end for
end runner

/** Runs each instance in parallel */
private def runSuite(
  solver: SimulatedCoolingSolver,
  suite: Suite,
  random: Random,
  runCount: Int,
  profile: CoolingProfile,
  extended: Boolean
): Seq[InstanceResults] =

  val solve = if extended then solver.runExtended else solver.run

  def runInstance(instance: Instance): InstanceResults =
    val seeds: mutable.Buffer[String] = mutable.Buffer.empty
    val results = Seq.fill(runCount) {
      val seed = random.nextLong().toHexString
      seeds.append(seed)
      solve(instance.path, seed, profile, None)
    }
    println(s"Solved instance $instance")

    if extended then
      // Collect extended cooling results
      val extendedResults =
        results.map(_.asInstanceOf[ExtendedCoolingResult])
      ExtendedInstanceResults(instance, extendedResults, seeds.toSeq)
    else
      // Collect standard cooling results
      val standardResults =
        results.map(_.asInstanceOf[StandardCoolingResult])
      StandardInstanceResults(instance, standardResults, seeds.toSeq)
    end if
  end runInstance

  Await.result(
    Future.sequence(suite.instances.map(instance =>
      Future(runInstance(instance))
    )),
    Duration.Inf
  )
end runSuite

// Simplified exportRuns using common `results: Seq[CoolingResult]`
private def exportRuns(
  results: Seq[InstanceResults],
  outputDir: Path
): Unit =
  results.foreach { result =>
    val lines =
      for (res, seed) <- result.results.zip(result.seeds) yield s"0x$seed $res"

    os.write.over(
      outputDir / s"${result.instance.id}$runExtension",
      lines.mkString("\n"),
      createFolders = true
    )
  }
end exportRuns

class InstanceSummary(
  val instance: Instance,
  val suite: Suite,
  results: Seq[StandardCoolingResult]
):
  val runCount: Int = results.size
  val optimum: Int = suite.optimum.get(instance.id).get
  val successRate: Double =
    results.count(_.weight == optimum).toDouble / results.size

  override def toString: String =
    s"$instance\n" +
      f"  Runs total: $runCount\n" +
      f"  Optimum: $optimum\n" +
      f"  Success rate: $successRate%.2f\n"
end InstanceSummary

class ExtendedInstanceSummary(
  instance: Instance,
  suite: Suite,
  results: Seq[ExtendedCoolingResult]
) extends InstanceSummary(instance, suite, results.map(_.standard)):
  val satisfiedCount: Int = results.count(_.isSatisfied)
  val satisfiedRate: Double = satisfiedCount.toDouble / results.size
  val stepsTotalAverage: Double =
    results.map(_.stepsTotal).sum.toDouble / results.size
  val stepsSinceChangeAverage: Double =
    results.map(_.stepsSinceChange).sum.toDouble / results.size
  val stepsSinceGainAverage: Double =
    results.map(_.stepsSinceGain).sum.toDouble / results.size
  val temperatureStopRatio: Double =
    results.count(_.stopCause == StopCause.Temperature).toDouble / results.size
  val maxIterationsStopRatio: Double =
    results.count(_.stopCause == StopCause.MaxIterations).toDouble / results.size
  val changeStopRatio: Double =
    results.count(_.stopCause == StopCause.NoChange).toDouble / results.size
  val gainStopRatio: Double =
    results.count(_.stopCause == StopCause.NoGain).toDouble / results.size
  val averageConvergence: Double =
    val converged =
      results.filter(res => res.isSatisfied && res.standard.weight == optimum)
    if converged.isEmpty then 0
    else
      converged.map(_.stepsSinceGain).sum.toDouble / converged.size

  override def toString: String =
    super.toString +
      f"  Satisfied count: $satisfiedCount\n" +
      f"  Satisfied rate: $satisfiedRate%.2f\n" +
      f"  Steps total average: $stepsTotalAverage%.2f\n" +
      f"  Steps since change average: $stepsSinceChangeAverage%.2f\n" +
      f"  Steps since gain average: $stepsSinceGainAverage%.2f\n" +
      f"  Temperature stop ratio: $temperatureStopRatio%.2f\n" +
      f"  Max iterations stop ratio: $maxIterationsStopRatio%.2f\n" +
      f"  Change stop ratio: $changeStopRatio%.2f\n" +
      f"  Gain stop ratio: $gainStopRatio%.2f\n" +
      f"  Average convergence: $averageConvergence%.2f"
end ExtendedInstanceSummary

class SuiteSummary(val instances: Seq[InstanceSummary]):
  val suite: Suite = instances.head.suite
  val successRate: Double = instances.map(_.successRate).sum / instances.size

  override def toString: String =
    s"$suite\n" +
      f"  Instances overall: ${instances.size}\n" +
      f"  Runs per instance: ${instances.head.runCount}\n" +
      f"  Runs total: ${instances.map(_.runCount).sum}\n" +
      f"  Success rate: $successRate%.2f\n"
end SuiteSummary

class ExtendedSuiteSummary(instances: Seq[ExtendedInstanceSummary])
    extends SuiteSummary(instances):
  val satisfiedCount: Int = instances.map(_.satisfiedCount).sum
  val satisfiedRate: Double =
    satisfiedCount.toDouble / instances.map(_.runCount).sum
  val stepsTotalAverage: Double =
    instances.map(_.stepsTotalAverage).sum / instances.size
  val stepsSinceChangeAverage: Double =
    instances.map(_.stepsSinceChangeAverage).sum / instances.size
  val stepsSinceGainAverage: Double =
    instances.map(_.stepsSinceGainAverage).sum / instances.size
  val temperatureStopRatio: Double =
    instances.map(_.temperatureStopRatio).sum / instances.size
  val maxIterationsStopRatio: Double =
    instances.map(_.maxIterationsStopRatio).sum / instances.size
  val changeStopRatio: Double =
    instances.map(_.changeStopRatio).sum / instances.size
  val gainStopRatio: Double =
    instances.map(_.gainStopRatio).sum / instances.size
  val averageConvergence: Double =
    val converged = instances.filter(_.averageConvergence > 0)
    if converged.isEmpty then 0
    else converged.map(_.averageConvergence).sum / converged.size

  override def toString: String =
    super.toString +
      f"  Satisfied count: $satisfiedCount\n" +
      f"  Satisfied rate: $satisfiedRate%.2f\n" +
      f"  Steps total average: $stepsTotalAverage%.2f\n" +
      f"  Steps since change average: $stepsSinceChangeAverage%.2f\n" +
      f"  Steps since gain average: $stepsSinceGainAverage%.2f\n" +
      f"  Temperature stop ratio: $temperatureStopRatio%.2f\n" +
      f"  Max iterations stop ratio: $maxIterationsStopRatio%.2f\n" +
      f"  Change stop ratio: $changeStopRatio%.2f\n" +
      f"  Gain stop ratio: $gainStopRatio%.2f\n" +
      f"  Average convergence: $averageConvergence%.2f"
end ExtendedSuiteSummary

private def exportSummary(
  results: Seq[InstanceResults],
  suite: Suite,
  outputDir: Path
): Unit =
  val summaries = results.map {
    case ExtendedInstanceResults(instance, extendedResults, _) =>
      ExtendedInstanceSummary(instance, suite, extendedResults)

    case StandardInstanceResults(instance, standardResults, _) =>
      InstanceSummary(instance, suite, standardResults)
  }

  // Export individual instance summaries
  for summary <- summaries do
    os.write.over(
      outputDir / summary.instance.toString,
      summary.toString,
      createFolders = true
    )
  end for

  val extended: Seq[ExtendedInstanceSummary] =
    summaries.collect { case s: ExtendedInstanceSummary => s }
  val overallSummary =
    if extended.nonEmpty && extended.size == summaries.size then
      ExtendedSuiteSummary(extended)
    else SuiteSummary(summaries)

  os.write.over(
    outputDir / os.up / s"overall.txt",
    overallSummary.toString,
    createFolders = true
  )
  println(overallSummary)
end exportSummary
