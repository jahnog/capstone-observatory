package observatory

import com.sksamuel.scrimage.{Image, Pixel}

import math.{abs, acos, cos, sin, pow, sqrt, asin}
import scala.annotation.tailrec

/**
  * 2nd milestone: basic visualization
  */
object Visualization {

  final val EarthRadius: Double = 6371d // Kilometers
  final val TooCloseDistance: Double = (12d / EarthRadius) // Radians

  final val WeightDistancePower: Double = 5d

  def greatCircleDistanceRadians(aGrades: Location, bGrades: Location): Double = {
    val aRadiansLat = aGrades.lat * math.Pi / 180d
    val aRadiansLon = aGrades.lon * math.Pi / 180d
    val bRadiansLat = bGrades.lat * math.Pi / 180d
    val bRadiansLon = bGrades.lon * math.Pi / 180d

    val a = sin(aRadiansLat) * sin(bRadiansLat)
    val b = cos(aRadiansLat) * cos(bRadiansLat)
    val c = cos(abs(aRadiansLon - bRadiansLon))

    acos(a + b * c)
  }

  def greatCircleDistanceRadiansX(aGrades: Location, bGrades: Location): Double = {

    val aRadiansLat = aGrades.lat * math.Pi / 180d
    val aRadiansLon = aGrades.lon * math.Pi / 180d
    val bRadiansLat = bGrades.lat * math.Pi / 180d
    val bRadiansLon = bGrades.lon * math.Pi / 180d

    val dLat = bRadiansLat - aRadiansLat
    val dLon = bRadiansLon - aRadiansLon

    val sindLat = sin(dLat / 2)
    val sindLon = sin(dLon / 2)

    val a = pow(sindLat, 2) + pow(sindLon, 2) * cos(aRadiansLat) * cos(bRadiansLat)

    2 * asin(sqrt(a))
  }

  def moreOrLessClose(a: Location, b: Location): Boolean = {
    if (abs(a.lat - b.lat) < 15 && (abs(a.lon - b.lon) < 60 || abs(a.lon - b.lon) > 300)) {
      true
    } else if (a.lat < -65 && b.lat < -65) {
      true
    } else if (a.lat > 65 && b.lat > 65) {
      true
    } else {
      false
    }
  }

  /**
    * @param temperatures Known temperatures: pairs containing a location and the temperature at this location
    * @param location     Location where to predict the temperature
    * @return The predicted temperature at `location`
    */
  def predictTemperature(temperatures: Iterable[(Location, Double)], location: Location): Double = {

    // TODO Implement kd-tree and the modified Shepard method
    // Shepard method
    def weight(aGrades: Location, bGrades: Location, distRadians: Double): Double = {
      1d / math.pow(distRadians, WeightDistancePower)
    }

    @tailrec
    def acumTemps(tAcum: Double, dAcum: Double, temps: Iterable[(Location, Double)]): (Double, Double) = {
      if (temps.isEmpty) {
        (tAcum, dAcum)
      } else {
        val elem: (Location, Double) = temps.head

        if (Speed.slow || moreOrLessClose(elem._1, location)) {
          val distRadians = greatCircleDistanceRadians(elem._1, location)
          if (distRadians < TooCloseDistance) {
            (elem._2, 1) // If we have a close point, we return it's temperature.
          } else {
            val w: Double = weight(elem._1, location, distRadians)
            acumTemps(tAcum + w * elem._2, dAcum + w, temps.tail)
          }
        } else {
          acumTemps(tAcum, dAcum, temps.tail)
        }
      }
    }

    val acumTuple: (Double, Double) = acumTemps(0d, 0d, temperatures)

    acumTuple._1 / acumTuple._2
  }

  def interp(min: Double, minColor: Color, max: Double, maxColor: Color, value: Double): Color = {
    val distDif = max - min
    val valDif = value - min

    val newRed = minColor.red + (valDif * (maxColor.red - minColor.red) / distDif)

    val newGreen = minColor.green + (valDif * (maxColor.green - minColor.green) / distDif)

    val newBlue = minColor.blue + (valDif * (maxColor.blue - minColor.blue) / distDif)

    Color((newRed + 0.5).toInt, (newGreen + 0.5).toInt, (newBlue + 0.5).toInt)
  }

  /**
    * @param points Pairs containing a value and its associated color
    * @param value  The value to interpolate
    * @return The color that corresponds to `value`, according to the color scale defined by `points`
    */
  def interpolateColor(points: Iterable[(Double, Color)], value: Double): Color = {
    if (points.isEmpty) {
      Color(0, 0, 0)
    } else {

      val p = points.head
      var min = Double.MinValue
      var max = Double.MaxValue
      var minColor = p._2
      var maxColor = p._2

      points.foreach(pts => {
        if (pts._1 < value) {
          if (pts._1 > min) {
            min = pts._1
            minColor = pts._2
          }
          if (min > max) {
            max = min
            maxColor = minColor
          }
        } else {
          if (pts._1 < max) {
            max = pts._1
            maxColor = pts._2
          }
          if (max < min) {
            min = max
            minColor = maxColor
          }
        }
      })

      if (min < value && value < max) {
        interp(min, minColor, max, maxColor, value)
      } else {
        if (value <= min) {
          minColor
        }
        else {
          maxColor
        }
      }
    }
  }

  /**
    * @param temperatures Known temperatures
    * @param colors       Color scale
    * @return A 360×180 image where each pixel shows the predicted temperature at its location
    */
  def visualize(temperatures: Iterable[(Location, Double)], colors: Iterable[(Double, Color)]): Image = {

    val pixels = Array.fill[Pixel](360 * 180)(Pixel(0, 0, 0, 255))

    for (y <- (0 until 180).par) {

      //      println("")
      //      print(s"Linea: ${y}: ")

      for (x <- 0 until 360) {

        val loc = Location(90 - y, -180 + x)
        val temp = predictTemperature(temperatures, loc)
        val color = interpolateColor(colors, temp)

        //        if( x % 10 == 0 ) { print(".") }

        val pixel = Pixel(color.red, color.green, color.blue, 255)

        val pos = y * 360 + x

        pixels(pos) = pixel
      }

      //      val image = Image(360, 180, pixels)
      //
      //      image.output(new java.io.File("target/some-image.png"))
    }

    val image = Image(360, 180, pixels)

    image.output(new java.io.File("target/some-image.png"))

    image
  }
}

