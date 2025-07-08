package visualization

import os.Path

import org.nspl.*
import org.nspl.awtrenderer.*

case class Step(step: Int, satisfied: Int, weight: Int, bestWeight: Int)

private val maximumColor = Color.RED
private val satisfiabilityPointColor = Color.BLUE
private val weightPointColor = Color.BLUE

def visualizeRun(
  file: Path,
  satisfiability: Path,
  weight: Path,
  optimum: Option[Int],
  clauses: Option[Int]
): Unit =
  val lines = os.read.lines(file)
  val steps: Seq[Step] = lines.map(line =>
    val parts = line.split(' ')
    Step(parts(0).toInt, parts(1).toInt, parts(2).toInt, parts(3).toInt)
  )

  os.write.over(
    satisfiability,
    createPlot(
      steps = steps.map(step => step.step.toDouble -> step.satisfied.toDouble),
      maximumValue = clauses.map(_.toDouble),
      xLabel = "Krok",
      yLabel = "Počet splněných klauzulí",
      legendLabel = "Optimum",
      mainColor = satisfiabilityPointColor,
      maxColor = maximumColor
    )
  )

  os.write.over(
    weight,
    createPlot(
      steps = steps.map(step => step.step.toDouble -> step.weight.toDouble),
      maximumValue = optimum.map(_.toDouble),
      xLabel = "Krok",
      yLabel = "Součet vah",
      legendLabel = "Optimum",
      mainColor = weightPointColor,
      maxColor = maximumColor
    )
  )
end visualizeRun

private def createPlot(
  steps: Seq[(Double, Double)],
  maximumValue: Option[Double],
  xLabel: String,
  yLabel: String,
  legendLabel: String,
  mainColor: Color,
  maxColor: Color
): Array[Byte] =
  // Maximum line data
  val maxLine = maximumValue match
    case Some(max) =>
      Seq(0d -> max, steps.last._1 -> max) -> line(color = maxColor)
    case None => Seq() -> line(color = maxColor)

  // Create plot
  val plot = xyplot(
    steps -> line(color = mainColor),
    maxLine
  )(
    par.withXLab(xLabel)
      .withYLab(yLabel).withExtraLegend(
        Seq(
          legendLabel -> PointLegend(
            shape = Shape.rectangle(0, 0, 1, 1),
            color = maximumColor
          )
        )
      )
  )

  renderToByteArray(plot)
end createPlot
