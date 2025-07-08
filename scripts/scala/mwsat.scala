package mwsat

import os.{Path, RelPath}
import solver.SimulatedCoolingSolver

import scala.util.Random

/** Expects e.g. .../ruf36-156-1000.mwcnf */
case class Instance(path: Path):
  require(os.isFile(path) && path.last.endsWith(".mwcnf"))
  private val pLine =
    os.read.lines(path).find(_.startsWith("p")).head.split(' ')
  private val filenameParts: Array[String] =
    path.last.split('.').head.split('-')
  def prefix: String = filenameParts.head.filter(c => c.isLetter)
  def id: Int = filenameParts.last.toInt
  def variable: Int = pLine(2).toInt
  def clauses: Int = pLine(3).toInt
  override def toString: String = s"$prefix-$variable-$clauses-$id"
end Instance

enum SetVariant(isMisleading: Boolean):
  case N extends SetVariant(false)
  case M extends SetVariant(false)
  case Q extends SetVariant(true)
  case R extends SetVariant(true)
object SetVariant:
  def fromString(string: String): SetVariant = string match
    case "N" => N
    case "M" => M
    case "Q" => Q
    case "R" => R
    case _   => throw new IllegalArgumentException(s"Unknown variant: $string")
end SetVariant

/**
 * Expects to be given a path to directory with instances and optimum file to be
 * in the same directory as the suite directory
 */
case class Suite(path: Path):
  require(os.isDir(path))
  private val directory = path.segments.toSeq.last.split('-')

  val prefix: String = directory(0).filter(c => c.isLetter)
  val variables: Int = directory(0).filter(c => c.isDigit).toInt
  val clauses: Int = directory(1).toInt
  val variant: SetVariant = SetVariant.fromString(directory(2))
  val instances: Seq[Instance] =
    os.list(path).filter(file =>
      os.isFile(file) && file.last.endsWith(".mwcnf")
    ).map(Instance(_))
  val optimum: SuiteOptimum =
    val optimumFile = os.list(path / os.up).filter(os.isFile).find(
      _.last == s"$prefix$variables-$clauses-${variant.toString}-opt.dat"
    )
    optimumFile match
      case Some(value) => SuiteOptimum(value)
      case None => throw new IllegalArgumentException(s"No optimum file found")

  /** Returns e.g. wruf36-157-R */
  override def toString: String =
    s"$prefix$variables-$clauses-${variant.toString}"
end Suite

/** Expects e.g. ../wruf36-157-N */
class SuiteOptimum(file: Path):
  private val entries: Map[Int, Int] =
    os.read.lines(file).map(_.split(' ')).map((words: Array[String]) =>
      val weight: Int = words(1).toInt
      val instanceNum: Int = words(0).split('-').last.toInt
      instanceNum -> weight
    ).toMap

  def get(instanceId: Int): Option[Int] = entries.get(instanceId)
end SuiteOptimum
