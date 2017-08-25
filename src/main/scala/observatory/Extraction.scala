package observatory

import java.time.LocalDate

import scala.io.{BufferedSource}

/**
  * 1st milestone: data extraction
  */
object Extraction {

  /**
    * @param year             Year number
    * @param stationsFile     Path of the stations resource file to use (e.g. "/stations.csv")
    * @param temperaturesFile Path of the temperatures resource file to use (e.g. "/1975.csv")
    * @return A sequence containing triplets (date, location, temperature)
    */
  def locateTemperatures(year: Int, stationsFile: String, temperaturesFile: String): Iterable[(LocalDate, Location, Double)] = {

    val stationsReader = new BufferedSource(getClass().getResourceAsStream(stationsFile))

    if (stationsReader == null) {
      Iterable[(LocalDate, Location, Double)]()
    }
    val stations = Map[(String, String), (Double, Double)]()

    val allLines = stationsReader.getLines()

    val allStations = allLines
      .map(str => {
        val lineArr = str.split(",")
        lineArr.size match {
          case 0 => ("", "", -1000d, -1000d)
          case 1 => (lineArr(0), "", -1000d, -1000d)
          case 2 => (lineArr(0), lineArr(1), -1000d, -1000d)
          case 3 => (lineArr(0), lineArr(1), lineArr(2).toDouble, -1000d)
          case _ => (lineArr(0), lineArr(1), lineArr(2).toDouble, lineArr(3).toDouble)
        }
      })
      .filter(row => row._3 > -1000d && row._4 > -1000d)
      .foldLeft(stations)((stations: Map[(String, String), (Double, Double)], station: (String, String, Double, Double)) => {
        stations.updated((station._1, station._2), (station._3, station._4))
      })

    println(s"Stations count: ${allStations.size}")

    val tempReader = new BufferedSource(getClass().getResourceAsStream(temperaturesFile))

    val allTemps = tempReader
      .getLines()
      .map(str => {
        val lineArr = str.split(",")
        (lineArr(0), lineArr(1), lineArr(2).toInt, lineArr(3).toInt, (lineArr(4).toDouble - 32) * 5 / 9)
      })
      .map(tmp => {
        try {
          val station = allStations((tmp._1, tmp._2))
          val date = LocalDate.of(year, tmp._3, tmp._4)
          val location = Location(station._1, station._2)
          (date, location, tmp._5)
        } catch {
          case ex: java.util.NoSuchElementException => (LocalDate.now(), Location(-1000d, -1000d), -1000d)
        }
      })
      .filter(r => r._2.lat > -1000d && r._2.lon > -1000d)

    allTemps.toIterable
  }

  /**
    * @param records A sequence containing triplets (date, location, temperature)
    * @return A sequence containing, for each location, the average temperature over the year.
    */
  def locationYearlyAverageRecords(records: Iterable[(LocalDate, Location, Double)]): Iterable[(Location, Double)] = {
    val grouped = records.groupBy(_._2)

    val prom = grouped.mapValues(iter => {
      iter.foldLeft((0d, 0))((acum, tmp) => {
        (acum._1 + tmp._3, acum._2 + 1)
      })
    })

    prom.map(x => {
      (x._1, x._2._1 / x._2._2)
    })
  }
}
