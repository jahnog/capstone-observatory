package observatory

import org.scalatest.FunSuite
import org.scalatest.prop.Checkers

import scala.collection.concurrent.TrieMap

trait InteractionTest extends FunSuite with Checkers {

  test("Interaction test1") {

    val colors = List((5.0, Color(255, 0, 0)), (30.0, Color(0, 0, 255)))
    val temps = List((Location(80, -80), 5.0), (Location(-80, 80), 30.0))

    val img = Interaction.tile(temps, colors, 0, 0, 0)

    val file = new java.io.File(s"target/test-inter.png")

    if( file.exists() ) { file.delete() }

    img.output(file)


  }
}
