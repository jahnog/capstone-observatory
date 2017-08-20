package observatory

import com.sksamuel.scrimage.{Image, Pixel}

import math.{abs, acos, cos, sin}
import scala.annotation.tailrec

/**
  * 2nd milestone: basic visualization
  */
object Visualization {

  final val EarthRadius: Double = 6371d // Kilometers
  final val TooCloseDistance: Double = (1d / EarthRadius) // Radians

  final val WeightDistancePower: Double = 3

  /**
    * @param temperatures Known temperatures: pairs containing a location and the temperature at this location
    * @param location     Location where to predict the temperature
    * @return The predicted temperature at `location`
    */
  def predictTemperature(temperatures: Iterable[(Location, Double)], location: Location): Double = {

    def greatCircleDistanceRadians(aRadians: Location, bRadians: Location): Double = {
      acos(sin(aRadians.lat) * sin(bRadians.lat) + cos(aRadians.lat) * cos(bRadians.lat) * cos(abs(aRadians.lon - bRadians.lon)))
    }

    // TODO Implement kd-tree and the modified Shepard method
    // Shepard method
    def weight(aRadians: Location, bRadians: Location): Double = {
      1d / math.pow(greatCircleDistanceRadians(aRadians, bRadians), WeightDistancePower)
    }

    @tailrec
    def acumTemps(tAcum: Double, dAcum: Double, temps: Iterable[(Location, Double)]): (Double, Double) = {
      if (temps.isEmpty) {
        (tAcum, dAcum)
      } else {
        val elem: (Location, Double) = temps.head
        val distRadians = greatCircleDistanceRadians(elem._1, location)
        if (distRadians < TooCloseDistance) {
          (elem._2, 1) // If we have a close point, we return it's temperature.
        } else {
          val w: Double = weight(elem._1, location)
          acumTemps(tAcum + w * elem._2, dAcum + w, temps.tail)
        }
      }
    }

    val acumTuple: (Double, Double) = acumTemps(0d, 0d, temperatures)

    acumTuple._1 / acumTuple._2
  }

  def interp(min: Double, minColor: Color, max: Double, maxColor: Color, value: Double): Color = {
    val distDif = (max - min).toInt
    val valDif = (value - min).toInt

    val newRed = minColor.red + (valDif * (maxColor.red - minColor.red) / distDif)

    val newGreen = minColor.green + (valDif * (maxColor.green - minColor.green) / distDif)

    val newBlue = minColor.blue + (valDif * (maxColor.blue - minColor.blue) / distDif)

    Color(newRed, newGreen, newBlue)
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

    val pixels = Array.ofDim[Pixel](360 * 180)

    var x = 0
    var y = 0
    var pos = 0

    while (y < 180) {
      while (x < 360) {

        val loc = Location(90 - y, -180 + x)
        val temp = predictTemperature(temperatures, loc)
        val color = interpolateColor(colors, temp)

        val pixel = Pixel(color.red, color.green, color.blue, 128)

        pixels(pos) = pixel

        pos = pos + 1
        x = x + 1
      }
      y = y + 1
    }

    val image = Image(360, 180, pixels)

    image.output(new java.io.File("target/some-image.png"))

    image
  }
}

