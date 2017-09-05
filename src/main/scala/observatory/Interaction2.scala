package observatory

/**
  * 6th (and last) milestone: user interface polishing
  */
object Interaction2 {

  val scale = Seq(
    (60d, Color(255, 255, 255)),
    (32d, Color(255, 0, 0)),
    (12d, Color(255, 255, 0)),
    (0d, Color(0, 255, 255)),
    (-15d, Color(0, 0, 255)),
    (-27d, Color(255, 0, 255)),
    (-50d, Color(33, 0, 107)),
    (-60d, Color(0, 0, 0)))

  val scaled = Seq(
    (7d, Color(0, 0, 0)),
    (4d, Color(255, 0, 0)),
    (2d, Color(255, 255, 0)),
    (0d, Color(255, 255, 255)),
    (-2d, Color(0, 255, 255)),
    (-7d, Color(0, 0, 255)))

  /**
    * @return The available layers of the application
    */
  def availableLayers: Seq[Layer] = {

    val maxZoom = 3
    val maxTile = (math.pow(2, maxZoom) - 1).toInt

    val years = for (year <- 2015 until 1974 by -1) yield year
    val temps = years.filter(year => {
//      val file = new java.io.File(s"target/temperatures/$year/$maxZoom/$maxTile-$maxTile.png")
//      val ex = file.exists()
//      ex
      true
    })

    val yearsd = for (year <- 1990 until 2016) yield year
    val devs = yearsd.filter(year => {
//      val file = new java.io.File(s"target/deviations/$year/$maxZoom/$maxTile-$maxTile.png")
//      file.exists()
      true
    })

    val layers = {
      if (temps.isEmpty) {
        Seq[Layer]()
      } else {
        val minTYear = temps.min(Ordering[Int])
        val maxTYear = temps.max(Ordering[Int])
        Seq[Layer](Layer(LayerName.Temperatures, scale, minTYear until (maxTYear + 1)))
      }
    } ++ {
      if (devs.isEmpty) {
        Seq[Layer]()
      } else {
        val minDYear = devs.min(Ordering[Int])
        val maxDYear = devs.max(Ordering[Int])
        Seq[Layer](Layer(LayerName.Deviations, scaled, minDYear until (maxDYear + 1)))
      }
    }

    layers
  }

  /**
    * @param selectedLayer A signal carrying the layer selected by the user
    * @return A signal containing the year bounds corresponding to the selected layer
    */
  def yearBounds(selectedLayer: Signal[Layer]): Signal[Range] = {
    Signal(selectedLayer().bounds)
  }

  /**
    * @param selectedLayer The selected layer
    * @param sliderValue   The value of the year slider
    * @return The value of the selected year, so that it never goes out of the layer bounds.
    *         If the value of `sliderValue` is out of the `selectedLayer` bounds,
    *         this method should return the closest value that is included
    *         in the `selectedLayer` bounds.
    */
  def yearSelection(selectedLayer: Signal[Layer], sliderValue: Signal[Int]): Signal[Int] = {
    Signal({
      val bounds = selectedLayer().bounds
      val slider = sliderValue()

      if (slider < bounds.min) {
        bounds.min
      }
      else if (slider > bounds.max) {
        bounds.max
      }
      else {
        slider
      }
    })
  }

  /**
    * @param selectedLayer The selected layer
    * @param selectedYear  The selected year
    * @return The URL pattern to retrieve tiles
    */
  def layerUrlPattern(selectedLayer: Signal[Layer], selectedYear: Signal[Int]): Signal[String] = {
    Signal({
      val layer = selectedLayer()
      val year = selectedYear()

      layer.layerName match {
        case LayerName.Deviations => s"target/deviations/$year/{z}/{x}-{y}.png"
        case LayerName.Temperatures => s"target/temperatures/$year/{z}/{x}-{y}.png"
      }
    })
  }

  /**
    * @param selectedLayer The selected layer
    * @param selectedYear  The selected year
    * @return The caption to show
    */
  def caption(selectedLayer: Signal[Layer], selectedYear: Signal[Int]): Signal[String] = {
    Signal({
      val layer = selectedLayer()
      val year = selectedYear()

      layer.layerName match {
        case LayerName.Deviations => s"Deviations ($year)"
        case LayerName.Temperatures => s"Temperatures ($year)"
      }
    })
  }
}

sealed abstract class LayerName(val id: String)

object LayerName {

  case object Temperatures extends LayerName("temperatures")

  case object Deviations extends LayerName("deviations")

}

/**
  * @param layerName  Name of the layer
  * @param colorScale Color scale used by the layer
  * @param bounds     Minimum and maximum year supported by the layer
  */
case class Layer(layerName: LayerName, colorScale: Seq[(Double, Color)], bounds: Range)

