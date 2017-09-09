package observatory


import org.scalatest.FunSuite
import org.scalatest.prop.Checkers

trait VisualizationTest extends FunSuite with Checkers {

  // Color(255,0,0). Expected: Color(191,0,64) (scale = List((-1.0,Color(255,0,0)), (0.0,Color(0,0,255))), value = -0.75)

  test("Great circle acos formula") {
    val a = Location( 36.12 , -86.67 )
    val b = Location( 33.94, -118.40 )

    val dist = 6372.8 * Visualization.greatCircleDistanceRadians( a, b )

    assert(dist === 2887.2599506071083)
  }

  test("Great circle Harvesine formula") {
    val a = Location( 36.12 , -86.67 )
    val b = Location( 33.94 , -118.40 )

    val dist = 6372.8 * Visualization.greatCircleDistanceRadians( a, b )

    assert(dist === 2887.2599506071087)
  }

  test("Linear Color interpolation") {
    val scale = List((-1.0,Color(255,0,0)), (0.0,Color(0,0,255)))
    val value = -0.75

    assert(Visualization.interpolateColor(scale, value) === Color(191,0,64))
  }

  test("Linear Color interpolation lower bound") {
    val scale = List((-1.0,Color(255,0,0)), (0.0,Color(0,0,255)))
    val value = -2.0

    assert(Visualization.interpolateColor(scale, value) === Color(255,0,0))
  }

  test("Linear Color interpolation upper bound") {
    val scale = List((-1.0,Color(255,0,0)), (0.0,Color(0,0,255)))
    val value = 2.0

    assert(Visualization.interpolateColor(scale, value) === Color(0,0,255))
  }

}
