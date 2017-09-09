package observatory

object Main extends App {

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

  val maxZoom = 3
  val maxTile = (math.pow(2, maxZoom) - 1).toInt

  val layers = Interaction2.availableLayers

  println(s"Layers: $layers")

  val years = for (year <- (2015 until 1974 by -1)) yield year

  val missingYears = years.filter(y => {
    val file = new java.io.File(s"target/temperatures/$y/$maxZoom/$maxTile-$maxTile.png")
    !file.exists()
  })

  missingYears.foreach(year => {

    val l = Extraction.locateTemperatures(year, "/stations.csv", s"/$year.csv")

    val lp = Extraction.locationYearlyAverageRecords(l)

    //    println("Making grid")
    // val grid = Manipulation.makeGrid(lp)
    //    println("Generating grid")

    //    println("Making grid")
    //
    //    val grid = Manipulation.makeGrid(lp.take(10))
    //
    //    println("Generating grid")

    //    for (y <- 0 until 180;
    //         x <- 0 until 360) {
    //
    //      val latitud = 90 - y
    //      val longitud = x - 180
    //
    //      val temp = grid(latitud, longitud)
    //    }

    for (zoom <- 0 until (maxZoom + 1);
         x <- 0 until math.pow(2, zoom).toInt;
         y <- 0 until math.pow(2, zoom).toInt
    ) {

      val file = new java.io.File(s"target/temperatures/$year/$zoom/$x-$y.png")

      if (!file.exists()) {
        println(s"Generating tile: $zoom - $x - $y")

        val image2 = Interaction.tile(lp, scale, zoom, x, y)
        // val image2 = Visualization2.visualizeGrid(grid, scale, zoom, x, y)

        val folder = new java.io.File(s"target/temperatures/$year/$zoom")

        if (!folder.exists()) {
          folder.mkdirs()
        }

        image2.output(file)
      }
    }
  })

  val baseYears = (for (year <- 1975 until 1990) yield year)
    .filter(year => {
      val file = new java.io.File(s"target/temperatures/$year/$maxZoom/$maxTile-$maxTile.png")
      file.exists()
    })
    .map(year => {
      val localTemp = Extraction.locateTemperatures(year, "/stations.csv", s"/$year.csv")
      println(s"Calculating averages: $year")
      val localAvg = Extraction.locationYearlyAverageRecords(localTemp)
      localAvg
    })

  val devYears = for (year <- (2015 until 1989 by -1)) yield year

  val missingDevYears = devYears.filter(y => {
    val file = new java.io.File(s"target/deviations/$y/$maxZoom/$maxTile-$maxTile.png")
    !file.exists()
  })

  val normals = Manipulation.average(baseYears)

  missingDevYears.foreach(year => {
    val l = Extraction.locateTemperatures(year, "/stations.csv", s"/$year.csv")
    val lp = Extraction.locationYearlyAverageRecords(l)

    val deviations = Manipulation.deviation(lp, normals)

    for (zoom <- 0 until (maxZoom + 1);
         x <- 0 until math.pow(2, zoom).toInt;
         y <- 0 until math.pow(2, zoom).toInt
    ) {

      val file = new java.io.File(s"target/deviations/$year/$zoom/$x-$y.png")

      if (!file.exists()) {
        println(s"Generating tile: $zoom - $x - $y")

        val imgdev = Visualization2.visualizeGrid(deviations, scaled, zoom, x, y)

        val folder = new java.io.File(s"target/deviations/$year/$zoom")

        if (!folder.exists()) {
          folder.mkdirs()
        }

        imgdev.output(file)
      }
    }
  })
}
