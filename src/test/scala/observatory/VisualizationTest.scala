package observatory


import org.scalatest.FunSuite
import org.scalatest.prop.Checkers

trait VisualizationTest extends FunSuite with Checkers {

  // Color(255,0,0). Expected: Color(191,0,64) (scale = List((-1.0,Color(255,0,0)), (0.0,Color(0,0,255))), value = -0.75)

  test("Linear Color interpolation") {
    val scale = List((-1.0,Color(255,0,0)), (0.0,Color(0,0,255)))
    val value = -0.75

    assert(Color(191,0,64) == Visualization.interpolateColor(scale, value))
  }
}
