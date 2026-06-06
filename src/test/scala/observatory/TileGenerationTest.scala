package observatory

import java.io.File
import java.nio.file.Files

import com.sksamuel.scrimage.{Image, Pixel}
import org.scalatest.FunSuite

trait TileGenerationTest extends FunSuite {

  test("missingTiles detects gaps even when the sentinel tile exists") {
    val root = Files.createTempDirectory("tile-generation-test").toFile
    val year = 2015
    val maxZoom = 1
    val allTiles = TileGeneration.expectedTiles(maxZoom)
    val sentinel = TileGeneration.TileCoordinate(maxZoom, 1, 1)
    val missing = TileGeneration.TileCoordinate(1, 0, 0)

    try {
      allTiles.foreach { tile =>
        if (tile != missing) {
          val file = TileGeneration.tileFile(root, year, tile)
          file.getParentFile.mkdirs()
          file.createNewFile()
        }
      }

      assert(TileGeneration.tileFile(root, year, sentinel).exists())

      val result = TileGeneration.missingTiles(root, year, maxZoom)

      assert(result == Vector(missing))
    } finally {
      deleteRecursively(root)
    }
  }

  test("missingTiles ignores leftover temporary png files") {
    val root = Files.createTempDirectory("tile-generation-temp-test").toFile
    val year = 2015
    val tile = TileGeneration.TileCoordinate(0, 0, 0)
    val output = TileGeneration.tileFile(root, year, tile)
    val temporary = new File(output.getParentFile, s".${output.getName}.tmp.png")

    try {
      output.getParentFile.mkdirs()
      temporary.createNewFile()

      val result = TileGeneration.missingTiles(root, year, 0)

      assert(result == Vector(tile))
    } finally {
      deleteRecursively(root)
    }
  }

  test("renderMissingTiles writes final png files without leaving temporary files") {
    val root = Files.createTempDirectory("tile-generation-render-test").toFile
    val year = 2015
    val maxZoom = 1
    val tiles = TileGeneration.expectedTiles(maxZoom)

    try {
      TileGeneration.renderMissingTiles("temperatures", root, year, tiles)(_ => sampleImage())

      assert(tiles.forall(tile => TileGeneration.tileFile(root, year, tile).exists()))
      assert(allFiles(root).forall(file => !file.getName.contains(".tmp.png")))
      assert(TileGeneration.missingTiles(root, year, maxZoom).isEmpty)
    } finally {
      deleteRecursively(root)
    }
  }

  test("a rerun skips tiles that are already complete") {
    val root = Files.createTempDirectory("tile-generation-rerun-test").toFile
    val year = 2015
    val maxZoom = 0
    var firstRunRenders = 0
    var secondRunRenders = 0

    try {
      val firstPlan = TileGeneration.missingTiles(root, year, maxZoom)
      TileGeneration.renderMissingTiles("temperatures", root, year, firstPlan) { _ =>
        firstRunRenders += 1
        sampleImage()
      }

      val secondPlan = TileGeneration.missingTiles(root, year, maxZoom)
      TileGeneration.renderMissingTiles("temperatures", root, year, secondPlan) { _ =>
        secondRunRenders += 1
        sampleImage()
      }

      assert(firstRunRenders == 1)
      assert(secondPlan.isEmpty)
      assert(secondRunRenders == 0)
    } finally {
      deleteRecursively(root)
    }
  }

  test("an interrupted temporary file is rerendered on the next run") {
    val root = Files.createTempDirectory("tile-generation-interrupted-test").toFile
    val year = 2015
    val tile = TileGeneration.TileCoordinate(0, 0, 0)
    val output = TileGeneration.tileFile(root, year, tile)
    val temporary = new File(output.getParentFile, s".${output.getName}.stale.tmp.png")
    var renders = 0

    try {
      output.getParentFile.mkdirs()
      temporary.createNewFile()

      val rerunPlan = TileGeneration.missingTiles(root, year, 0)

      TileGeneration.renderMissingTiles("temperatures", root, year, rerunPlan) { _ =>
        renders += 1
        sampleImage()
      }

      assert(rerunPlan == Vector(tile))
      assert(renders == 1)
      assert(output.exists())
      assert(allFiles(root).forall(file => !file.getName.contains(".tmp.png")))
    } finally {
      deleteRecursively(root)
    }
  }

  private def deleteRecursively(file: File): Unit = {
    if (file.isDirectory) {
      Option(file.listFiles()).getOrElse(Array.empty).foreach(deleteRecursively)
    }

    val _ = file.delete()
  }

  private def allFiles(file: File): Seq[File] = {
    if (!file.exists()) {
      Seq.empty
    } else if (file.isDirectory) {
      Option(file.listFiles()).getOrElse(Array.empty).flatMap(allFiles)
    } else {
      Seq(file)
    }
  }

  private def sampleImage(): Image = Image(1, 1, Array(Pixel(12, 34, 56, 255)))
}