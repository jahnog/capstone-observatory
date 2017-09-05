package observatory

import com.sksamuel.scrimage.{Image, Pixel}
import observatory.Visualization.{interpolateColor, predictTemperature}

/**
  * 3rd milestone: interactive visualization
  */
object Interaction {

  /**
    * @param zoom Zoom level
    * @param x    X coordinate
    * @param y    Y coordinate
    * @return The latitude and longitude of the top-left corner of the tile, as per http://wiki.openstreetmap.org/wiki/Slippy_map_tilenames
    */
  def tileLocation(zoom: Int, x: Int, y: Int): Location = {
    val n = math.pow(2, zoom)
    val lon_deg: Double = x / n * 360.0 - 180.0
    val lat_rad: Double = math.atan(math.sinh(math.Pi * (1.0 - 2.0 * y / n)))
    val lat_deg: Double = lat_rad * 180.0 / math.Pi

    Location(lat_deg, lon_deg)
  }

  /**
    * @param temperatures Known temperatures
    * @param colors       Color scale
    * @param zoom         Zoom level
    * @param tilex        X coordinate
    * @param tiley        Y coordinate
    * @return A 256×256 image showing the contents of the tile defined by `x`, `y` and `zooms`
    */
  def tile(temperatures: Iterable[(Location, Double)], colors: Iterable[(Double, Color)], zoom: Int, tilex: Int, tiley: Int): Image = {
    if (Speed.slow) {
      tile256(temperatures, colors, zoom, tilex, tiley)
    } else {
      tile128(temperatures, colors, zoom, tilex, tiley)
    }
  }

  def tile256(temperatures: Iterable[(Location, Double)], colors: Iterable[(Double, Color)], zoom: Int, tilex: Int, tiley: Int): Image = {

    val pixels = Array.fill[Pixel](256 * 256)(Pixel(0, 0, 0, 127))

    print(s"Tile 256: ")
    for (y <- (0 until 256).par) {

      print(".")
      for (x <- 0 until 256) {
        val loc = tileLocation(zoom + 8, tilex * 256 + x, tiley * 256 + y)
        val temp = predictTemperature(temperatures, loc)
        val color = interpolateColor(colors, temp)

        // if( x % 10 == 0 ) { print(".") }
        val pixel = Pixel(color.red, color.green, color.blue, 127)
        val pos = y * 256 + x
        pixels(pos) = pixel
      }
    }
    println("")

    val image = Image(256, 256, pixels)

    image
  }

  def tile128(temperatures: Iterable[(Location, Double)], colors: Iterable[(Double, Color)], zoom: Int, tilex: Int, tiley: Int): Image = {

    val standardSize = 256
    val tileSize = 128
    val scale = standardSize / tileSize

    val pixels = Array.fill[Pixel](tileSize * tileSize)(Pixel(0, 0, 0, 127))

    print(s"Tile $tileSize: ")
    for (y <- (0 until tileSize).par) {

      print(".")
      for (x <- 0 until tileSize) {
        val loc = tileLocation(zoom + 8, tilex * standardSize + x * scale, tiley * standardSize + y * scale)
        val temp = predictTemperature(temperatures, loc)
        val color = interpolateColor(colors, temp)

        val pixel = Pixel(color.red, color.green, color.blue, 127)

        val pos = y * tileSize + x

        pixels(pos) = pixel
      }
    }
    println("")

    val image = Image(tileSize, tileSize, pixels)
    val scaled = image.scale(scale.toDouble)

    scaled
  }

  /**
    * Generates all the tiles for zoom levels 0 to 3 (included), for all the given years.
    *
    * @param yearlyData    Sequence of (year, data), where `data` is some data associated with
    *                      `year`. The type of `data` can be anything.
    * @param generateImage Function that generates an image given a year, a zoom level, the x and
    *                      y coordinates of the tile and the data to build the image from
    */
  def generateTiles[Data](
                           yearlyData: Iterable[(Int, Data)],
                           generateImage: (Int, Int, Int, Int, Data) => Unit
                         ): Unit = {
    yearlyData.foreach(yearly => {
      for (zoom <- 0 until 4;
           x <- 0 until math.pow(2, zoom).toInt;
           y <- 0 until math.pow(2, zoom).toInt
      ) generateImage(yearly._1, zoom, x, y, yearly._2)
    }
    )
  }

}
