package observatory

import com.sksamuel.scrimage.{Image, Pixel}
import observatory.Interaction.tileLocation
import observatory.Visualization.{interpolateColor, predictTemperature}

/**
  * 5th milestone: value-added information visualization
  */
object Visualization2 {

  /**
    * @param x   X coordinate between 0 and 1
    * @param y   Y coordinate between 0 and 1
    * @param d00 Top-left value
    * @param d01 Bottom-left value
    * @param d10 Top-right value
    * @param d11 Bottom-right value
    * @return A guess of the value at (x, y) based on the four known values, using bilinear interpolation
    *         See https://en.wikipedia.org/wiki/Bilinear_interpolation#Unit_Square
    */
  def bilinearInterpolation(
                             x: Double,
                             y: Double,
                             d00: Double,
                             d01: Double,
                             d10: Double,
                             d11: Double
                           ): Double = {

    val result = d00 * (1 - x) * (1 - y) + d10 * x * (1 - y) + d01 * (1 - x) * y + d11 * x * y

    result
  }

  /**
    * @param grid   Grid to visualize
    * @param colors Color scale to use
    * @param zoom   Zoom level of the tile to visualize
    * @param x      X value of the tile to visualize
    * @param y      Y value of the tile to visualize
    * @return The image of the tile at (x, y, zoom) showing the grid using the given color scale
    */
  def visualizeGrid(
                     grid: (Int, Int) => Double,
                     colors: Iterable[(Double, Color)],
                     zoom: Int,
                     tilex: Int,
                     tiley: Int
                   ): Image = {

    val pixels = Array.fill[Pixel](256 * 256)(Pixel(0, 0, 0, 127))

    print(s"Tile 256: ")
    for (y <- (0 until 256).par) {
      print(".")
      for (x <- 0 until 256) {

        val loc = tileLocation(zoom + 8, tilex * 256 + x, tiley * 256 + y)
        val ceilLat = math.ceil(loc.lat).toInt
        val floorLat = math.floor(loc.lat).toInt
        val ceilLon = math.ceil(loc.lon).toInt match {
          case 180 => 0
          case _ => math.ceil(loc.lon).toInt
        }
        val floorLon = math.floor(loc.lon).toInt

        val d00 = grid(ceilLat, floorLon)
        val d01 = grid(floorLat, floorLon)
        val d10 = grid(ceilLat, ceilLon)
        val d11 = grid(floorLat, ceilLon)

        val ydiff = math.ceil(loc.lat) - loc.lat
        val xdiff = loc.lon - math.floor(loc.lon)

        val temp = bilinearInterpolation(x, y, d00, d01, d10, d11)
        val color = interpolateColor(colors, temp)

        val pixel = Pixel(color.red, color.green, color.blue, 127)

        val pos = y * 256 + x

        pixels(pos) = pixel
      }
    }
    println("")

    val colList = colors.toList

    val image = Image(256, 256, pixels)

    image
  }

}
