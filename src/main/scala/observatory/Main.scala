package observatory

object Main extends App {


  val l = Extraction.locateTemperatures(2000, "/stations.csv", "/2000.csv")

  val list = l.toList

  println(s"Length: ${list.length}")

  val lp = Extraction.locationYearlyAverageRecords(l)

  val list2 = lp.toList

  println(s"Length2: ${list2.length}")
}
