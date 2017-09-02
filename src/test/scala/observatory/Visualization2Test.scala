package observatory

import org.scalatest.FunSuite
import org.scalatest.prop.Checkers

trait Visualization2Test extends FunSuite with Checkers {

  test("Visualization test1") {

    val colors = List((5.0,Color(255,0,0)), (30.0,Color(0,0,255)))
    val temps = List( (Location(45, -90), 5.0) , (Location(-45,90), 30.0) )

    val grid = Manipulation.makeGrid( temps )
    val img = Visualization2.visualizeGrid(grid, colors, 0, 0, 0)

    val file = new java.io.File(s"test-viz2.png")

    if( file.exists() ) { file.delete() }

    val file2 = new java.io.File(s"test-viz2.png")
    img.output( file2 )


  }

  test("InteractionX test1") {

    val colors = List((5.0, Color(255, 0, 0)), (30.0, Color(0, 0, 255)))
    val temps = List((Location(45, -90), 5.0), (Location(-45, 90), 30.0))

    val img = Interaction.tile(temps, colors, 0, 0, 0)

    val file = new java.io.File(s"test-inter.png")

    if( file.exists() ) { file.delete() }

    val file2 = new java.io.File(s"test-inter.png")

    img.output(file2)


  }
}
