package observatory

/**
  * 4th milestone: value-added information
  */
object Manipulation {

  /**
    * @param temperatures Known temperatures
    * @return A function that, given a latitude in [-89, 90] and a longitude in [-180, 179],
    *         returns the predicted temperature at this location
    */
  def makeGrid(temperatures: Iterable[(Location, Double)]): (Int, Int) => Double = {

    def calculate(latitud: Int, longitud: Int): Double = {
      Visualization.predictTemperature(temperatures, Location(latitud.toDouble, longitud.toDouble))
    }

    calculate
  }

  /**
    * @param temperaturess Sequence of known temperatures over the years (each element of the collection
    *                      is a collection of pairs of location and temperature)
    * @return A function that, given a latitude and a longitude, returns the average temperature at this location
    */
  def average(temperaturess: Iterable[Iterable[(Location, Double)]]): (Int, Int) => Double = {

    def calcAverage(latitud: Int, longitud: Int): Double = {
      val loc = Location(latitud.toDouble, longitud.toDouble)

      val ttuple = temperaturess.foldLeft((0.0, 0))((tuple, temperatures) =>
        (tuple._1 + Visualization.predictTemperature(temperatures, loc), tuple._2 + 1))

      val avg = ttuple._1 / ttuple._2

      avg
    }

    calcAverage
  }

  /**
    * @param temperatures Known temperatures
    * @param normals      A grid containing the “normal” temperatures
    * @return A grid containing the deviations compared to the normal temperatures
    */
  def deviation(temperatures: Iterable[(Location, Double)], normals: (Int, Int) => Double): (Int, Int) => Double = {

    def calcDeviation(latitud: Int, longitud: Int): Double = {

      val normalTemp = normals(latitud, longitud)

      val loc = Location(latitud.toDouble, longitud.toDouble)

      val predictTemp = Visualization.predictTemperature(temperatures, loc)

//      println(s"Location: $loc")
      //      println(s"Temps: $temperatures")
      //      println(s"Dev: ${predictTemp - normalTemp}")

      predictTemp - normalTemp
    }

    calcDeviation
  }
}

