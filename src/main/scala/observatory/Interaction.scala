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
    val lat_rad: Double = math.atan(math.sinh(math.Pi * (1 - 2 * y / n)))
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

    val pixels = Array.fill[Pixel](256 * 256)(Pixel(0, 0, 0, 127))

    for (y <- (0 until 256).par) {

      //      println("")
      //      print(s"Linea: ${y}: ")

      for (x <- 0 until 256) {

        val loc = tileLocation(zoom + 8, tilex + x, tiley + y)
        val temp = predictTemperature(temperatures, loc)
        val color = interpolateColor(colors, temp)

        //        if( x % 10 == 0 ) { print(".") }

        val pixel = Pixel(color.red, color.green, color.blue, 127)

        val pos = y * 256 + x

        pixels(pos) = pixel
      }

      //      val image = Image(360, 180, pixels)
      //
      //      image.output(new java.io.File("target/some-image.png"))
    }

    val image = Image(360, 180, pixels)

    image.output(new java.io.File(s"target/tile-$zoom-$tilex-$tiley.png"))

    image
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
    ???
  }

}
