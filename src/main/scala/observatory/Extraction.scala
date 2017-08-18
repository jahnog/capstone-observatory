package observatory

import java.io.InputStream
import java.time.LocalDate

import scala.io.{BufferedSource, Source}

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

    val stations = Map[(String, String), (Double, Double)]()

    val allStations = stationsReader
      .getLines()
      .map(str => {
        val lineArr = str.split(",")
        var stn = ""
        var wban = ""
        var lat = -100d
        var long = -100d
        if (lineArr.size > 0) {
          stn = lineArr(0)
        }
        if (lineArr.size > 1) {
          wban = lineArr(1)
        }
        if (lineArr.size > 2) {
          lat = lineArr(2).toDouble
        }
        if (lineArr.size > 3) {
          long = lineArr(3).toDouble
        }
        (stn, wban, lat, long)
      })
      .filter(row => row._3 > -100d && row._4 > -100d)
      .foldLeft(stations)((stations: Map[(String, String), (Double, Double)], station: (String, String, Double, Double)) => {
        val newStations = stations.updated((station._1, station._2), (station._3, station._4))
        newStations
      })


    println(s"Stations count: ${allStations.size}")

    val tempReader = new BufferedSource(getClass().getResourceAsStream(temperaturesFile))

    val allTemps = tempReader
      .getLines()
      .map(str => {
        val lineArr = str.split(",")
        (lineArr(0), lineArr(1), lineArr(2).toInt, lineArr(3).toInt, lineArr(4).toDouble)
      })
      .map(tmp => {

        try {
          val station = allStations((tmp._1, tmp._2))
          val date = LocalDate.of(year, tmp._3, tmp._4)
          val location = Location(station._1, station._2)
          (date, location, tmp._5)
        } catch {
          case ex: java.util.NoSuchElementException => (LocalDate.now(), Location(-100d, -100d), -100d)

        }
      })
      .filter(r => r._2.lat > -100d && r._2.lon > -100d)

    allTemps.toIterable
  }

  /**
    * @param records A sequence containing triplets (date, location, temperature)
    * @return A sequence containing, for each location, the average temperature over the year.
    */
  def locationYearlyAverageRecords(records: Iterable[(LocalDate, Location, Double)]): Iterable[(Location, Double)] = {
    ???
  }

}
