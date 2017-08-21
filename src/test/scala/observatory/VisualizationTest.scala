package observatory


import org.scalatest.FunSuite
import org.scalatest.prop.Checkers

trait VisualizationTest extends FunSuite with Checkers {

  // Color(255,0,0). Expected: Color(191,0,64) (scale = List((-1.0,Color(255,0,0)), (0.0,Color(0,0,255))), value = -0.75)

  test("Great circle acos formula") {
    val a = Location( 36.12 * math.Pi / 180d, -86.67 * math.Pi / 180d)
    val b = Location( 33.94 * math.Pi / 180d, -118.40 * math.Pi / 180d)

    val dist = 6372.8 * Visualization.greatCircleDistanceRadians( a, b )

    assert(dist === 2887.2599506071083)
  }

  test("Great circle Harvesine formula") {
    val a = Location( 36.12 * math.Pi / 180d, -86.67 * math.Pi / 180d)
    val b = Location( 33.94 * math.Pi / 180d, -118.40 * math.Pi / 180d)

    val dist = 6372.8 * Visualization.greatCircleDistanceRadiansH( a, b )

    assert(dist === 2887.2599506071087)
  }

  test("Linear Color interpolation") {
    val scale = List((-1.0,Color(255,0,0)), (0.0,Color(0,0,255)))
    val value = -0.75

    assert(Color(191,0,64) === Visualization.interpolateColor(scale, value))
  }
}
