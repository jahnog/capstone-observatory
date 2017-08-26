package observatory

object Main extends App {


  val l = Extraction.locateTemperatures(2015, "/stations.csv", "/2015.csv")

  val list = l.toList

  println(s"Length: ${list.length}")

  val lp = Extraction.locationYearlyAverageRecords(l)

  val list2 = lp.toList  // .take(1000)

  println(s"Length2: ${list2.length}")

  val scale = List(
    (60d, Color(255, 255, 255)),
    (32d, Color(255, 0, 0)),
    (12d, Color(255, 255, 0)),
    (0d, Color(0, 255, 255)),
    (-15d, Color(0, 0, 255)),
    (-27d, Color(255, 0, 255)),
    (-50d, Color(33, 0, 107)),
    (-60d, Color(0, 0, 0)))

  // val image = Visualization.visualize(list2,scale)

  for (zoom <- 0 until 4;
       x <- 0 until math.pow(2, zoom).toInt;
       y <- 0 until math.pow(2, zoom).toInt
  ) {

    val file = new java.io.File(s"target/temperatures/2015/$zoom/$x-$y.png")

    if( ! file.exists() ) {
      println( s"Generating tile: $zoom - $x - $y")

      val image2 = Interaction.tile(list2, scale, zoom, x, y)

      val folder = new java.io.File(s"target/temperatures/2015/$zoom")

      if (!folder.exists()) {
        folder.mkdirs()
      }

      image2.output( file )
    }
  }

}
