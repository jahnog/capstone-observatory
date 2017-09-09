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

    val grid: Array[Double] = Array.fill[Double](360 * 180)(0)

    print("Preparation to make grid")
    //    if (!Speed.slow) {
    for (y <- (0 until 180).par) {
      print(".")
      for (x <- 0 until 360) {
        val latitud = 90 - y
        val longitud = x - 180
        if( x == 102 && y == 122 ){
          println("Aqui")
        }
        val loc = Location(latitud, longitud)
        val temp = Visualization.predictTemperature(temperatures, loc)
        grid(y * 360 + x) = temp
      }
    }
    //    }
    println("")

    def calculate(latitud: Int, longitud: Int): Double = {

      //      if (Speed.slow) {
      //        val loc = Location(latitud, longitud)
      //        val temp1 = Visualization.predictTemperature(temperatures, loc)
      //        temp1
      //      }
      //      else {

      val y = if (latitud > -90) {
        90 - latitud
      } else {
        179
      }
      val x = if (longitud < 180) {
        180 + longitud
      } else {
        359
      }
      if( x == 102 && y == 122 ){
        println("Aqui")
      }
      var temp2 = 0.0
      if (y * 360 + x >= 0) {
        temp2 = grid(y * 360 + x)
      }
      else {
        println("Ehh")
      }

      //              if (temp1 != temp2) {
      //                println(s"Lat: $latitud - Lon: $longitud - temp1: $temp1 - temp2: $temp2")
      //              }

      temp2
    }

    //    }

    calculate
  }

  /**
    * @param temperaturess Sequence of known temperatures over the years (each element of the collection
    *                      is a collection of pairs of location and temperature)
    * @return A function that, given a latitude and a longitude, returns the average temperature at this location
    */
  def average(temperaturess: Iterable[Iterable[(Location, Double)]]): (Int, Int) => Double = {

    val averagegrid: Array[Double] = Array.fill[Double](360 * 180)(0)

    print(s"Creating grids averages")
    val grids = temperaturess.map(temps => {
      val tgrid = Manipulation.makeGrid(temps)
      tgrid
    })
    println("")

    print(s"Pre calculating averages")
    for (y <- (0 until 180).par) {
      print(".")
      for (x <- 0 until 360) {
        val acum = grids.foldLeft(0.0)((ac, grid) => {
          val latitud = 90 - y
          val longitud = x - 180
          ac + grid(latitud, longitud)
        })
        averagegrid(y * 360 + x) = acum / grids.size
      }
    }
    println("")

    //    for (y <- (0 until 180).par) {
    //      print(".")
    //      for (x <- 0 until 360) {
    //        val latitud = 90 - y
    //        val longitud = x - 180
    //        val loc = Location(latitud, longitud)
    //
    //        val ttuple = temperaturess.foldLeft((0.0, 0))((tuple, temperatures) =>
    //          (tuple._1 + Visualization.predictTemperature(temperatures, loc), tuple._2 + 1))
    //
    //        val avg = ttuple._1 / ttuple._2
    //        averagegrid(y * 360 + x) = avg
    //      }
    //    }
    println("")

    def calcAverage(latitud: Int, longitud: Int): Double = {
      //      val loc = Location(latitud.toDouble, longitud.toDouble)
      //
      //      val ttuple = temperaturess.foldLeft((0.0, 0))((tuple, temperatures) =>
      //        (tuple._1 + Visualization.predictTemperature(temperatures, loc), tuple._2 + 1))
      //
      //      val avg = ttuple._1 / ttuple._2
      //
      //      avg

      val y = 90 - latitud
      val x = 180 + longitud
      val avg = averagegrid(y * 360 + x)

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

    val devgrid: Array[Double] = Array.fill[Double](360 * 180)(0)

    print(s"Pre calculating deviations: ")

    for (y <- (0 until 180).par) {
      print(".")
      for (x <- 0 until 360) {
        val latitud = 90 - y
        val longitud = x - 180
        val loc = Location(latitud, longitud)

        val normalTemp = normals(latitud, longitud)
        val predictTemp = Visualization.predictTemperature(temperatures, loc)

        val dev = predictTemp - normalTemp

        devgrid(y * 360 + x) = dev
      }
    }
    println("")

    def calcDeviation(latitud: Int, longitud: Int): Double = {

      //      val normalTemp = normals(latitud, longitud)
      //      val loc = Location(latitud.toDouble, longitud.toDouble)
      //      val predictTemp = Visualization.predictTemperature(temperatures, loc)
      //      predictTemp - normalTemp

      val y = 90 - latitud
      val x = 180 + longitud
      val dev = devgrid(y * 360 + x)
      dev
    }

    calcDeviation
  }
}

