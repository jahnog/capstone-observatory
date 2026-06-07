package observatory

import java.io.File

import scala.util.Try

object Main extends App {

  ProcessPriority.ensureLowPriority()

  private val defaultTemperatureYears = for (year <- (2015 until 1974 by -1)) yield year

  private val defaultDeviationYears = for (year <- (2015 until 1989 by -1)) yield year

  private val defaultBaselineYears = for (year <- 1975 until 1990) yield year

  private def rawConfig(propertyName: String, envName: String): Option[String] =
    sys.props.get(propertyName).orElse(sys.env.get(envName)).map(_.trim)

  private def stringConfig(propertyName: String, envName: String, default: String): String =
    rawConfig(propertyName, envName).filter(_.nonEmpty).getOrElse(default)

  private def intConfig(propertyName: String, envName: String, default: Int): Int =
    rawConfig(propertyName, envName)
      .filter(_.nonEmpty)
      .map(value => Try(value.toInt).getOrElse(throw new IllegalArgumentException(s"Invalid integer for $propertyName: $value")))
      .getOrElse(default)

  private def yearsConfig(propertyName: String, envName: String, default: Seq[Int]): Seq[Int] =
    rawConfig(propertyName, envName)
      .map(parseYears)
      .getOrElse(default)

  private def parseYears(raw: String): Seq[Int] = {
    if (raw.isEmpty) {
      Vector.empty
    } else {
      raw.split(",").iterator
        .map(_.trim)
        .filter(_.nonEmpty)
        .map(value => Try(value.toInt).getOrElse(throw new IllegalArgumentException(s"Invalid year value: $value")))
        .toVector
    }
  }

  private def enabledLayersConfig(): Set[String] =
    rawConfig("tile.generation.layers", "TILE_GENERATION_LAYERS")
      .map { raw =>
        if (raw.isEmpty) {
          Set.empty[String]
        } else {
          raw.split(",").iterator.map(_.trim.toLowerCase).filter(_.nonEmpty).toSet
        }
      }
      .getOrElse(Set("temperatures", "deviations"))

  val scale = List(
    (60d, Color(255, 255, 255)),
    (32d, Color(255, 0, 0)),
    (12d, Color(255, 255, 0)),
    (0d, Color(0, 255, 255)),
    (-15d, Color(0, 0, 255)),
    (-27d, Color(255, 0, 255)),
    (-50d, Color(33, 0, 107)),
    (-60d, Color(0, 0, 0)))


  val scaled = Seq(
    (7d, Color(0, 0, 0)),
    (4d, Color(255, 0, 0)),
    (2d, Color(255, 255, 0)),
    (0d, Color(255, 255, 255)),
    (-2d, Color(0, 255, 255)),
    (-7d, Color(0, 0, 255)))

  val maxZoom = intConfig("tile.generation.maxZoom", "TILE_GENERATION_MAX_ZOOM", 3)

  val layers = Interaction2.availableLayers
  val targetRoot = new File(stringConfig("tile.generation.targetRoot", "TILE_GENERATION_TARGET_ROOT", "target"))
  val temperatureOutputRoot = new File(targetRoot, "temperatures")
  val deviationOutputRoot = new File(targetRoot, "deviations")
  val enabledLayers = enabledLayersConfig()
  val years = yearsConfig("tile.generation.temperatureYears", "TILE_GENERATION_TEMPERATURE_YEARS", defaultTemperatureYears)
  val devYears = yearsConfig("tile.generation.deviationYears", "TILE_GENERATION_DEVIATION_YEARS", defaultDeviationYears)
  val baselineYears = yearsConfig("tile.generation.baselineYears", "TILE_GENERATION_BASELINE_YEARS", defaultBaselineYears)

  println(s"Layers: $layers")
  println(s"Tile generation config: target=${targetRoot.getPath}, maxZoom=$maxZoom, enabled=${enabledLayers.toSeq.sorted.mkString(",")}")

  if (enabledLayers.contains("temperatures")) {
    val missingYears = years.map(year => year -> TileGeneration.missingTiles(temperatureOutputRoot, year, maxZoom))
      .filter { case (_, missingTiles) => missingTiles.nonEmpty }

    missingYears.foreach { case (year, missingTiles) =>

      val l = Extraction.locateTemperatures(year, "/stations.csv", s"/$year.csv")

      val lp = Extraction.locationYearlyAverageRecords(l)

      //    println("Making grid")
      // val grid = Manipulation.makeGrid(lp)
      //    println("Generating grid")

      // val grid = Manipulation.makeGridTree(lp)

      TileGeneration.renderMissingTiles("temperatures", temperatureOutputRoot, year, missingTiles) { tile =>
        val image2 = Interaction.tile(lp, scale, tile.zoom, tile.x, tile.y)
        // val image2 = Visualization2.visualizeGrid(grid, scale, tile.zoom, tile.x, tile.y)
        image2
      }
    }
  }

  if (enabledLayers.contains("deviations")) {
    val missingDevYears = devYears.map(year => year -> TileGeneration.missingTiles(deviationOutputRoot, year, maxZoom))
      .filter { case (_, missingTiles) => missingTiles.nonEmpty }

    if (missingDevYears.nonEmpty) {
      val baseYears = baselineYears
        .filter(year => TileGeneration.missingTiles(temperatureOutputRoot, year, maxZoom).isEmpty)
        .map(year => {
          val localTemp = Extraction.locateTemperatures(year, "/stations.csv", s"/$year.csv")
          println(s"Calculating averages: $year")
          Extraction.locationYearlyAverageRecords(localTemp)
        })

      if (baseYears.nonEmpty) {
        val normals = Manipulation.average(baseYears)

        missingDevYears.foreach { case (year, missingTiles) =>
          val l = Extraction.locateTemperatures(year, "/stations.csv", s"/$year.csv")
          val lp = Extraction.locationYearlyAverageRecords(l)

          val deviations = Manipulation.deviation(lp, normals)

          TileGeneration.renderMissingTiles("deviations", deviationOutputRoot, year, missingTiles) { tile =>
            Visualization2.visualizeGrid(deviations, scaled, tile.zoom, tile.x, tile.y)
          }
        }
      } else {
        println("Skipping deviations: no complete baseline temperature years were found.")
      }
    }
  }
}
