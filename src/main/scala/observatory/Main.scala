package observatory

object Main extends App {


  val l = Extraction.locateTemperatures(2000, "/stations.csv", "/2000.csv")

  val list = l.toList

  println(s"Length: ${list.length}")

  val lp = Extraction.locationYearlyAverageRecords(l)

  val list2 = lp.toList

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


  val image = Visualization.visualize(list2,scale)
}
