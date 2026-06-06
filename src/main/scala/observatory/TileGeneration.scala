package observatory

import java.io.File
import java.nio.file.{AtomicMoveNotSupportedException, Files, StandardCopyOption}

import com.sksamuel.scrimage.Image

object TileGeneration {

  final case class TileCoordinate(zoom: Int, x: Int, y: Int)

  def expectedTiles(maxZoom: Int): Vector[TileCoordinate] = {
    (for {
      zoom <- 0 to maxZoom
      x <- 0 until (1 << zoom)
      y <- 0 until (1 << zoom)
    } yield TileCoordinate(zoom, x, y)).toVector
  }

  def tileFile(layerRoot: File, year: Int, tile: TileCoordinate): File =
    new File(layerRoot, s"$year/${tile.zoom}/${tile.x}-${tile.y}.png")

  def missingTiles(layerRoot: File, year: Int, maxZoom: Int): Vector[TileCoordinate] =
    expectedTiles(maxZoom).filterNot(tile => tileFile(layerRoot, year, tile).exists())

  def renderMissingTiles(layerName: String, layerRoot: File, year: Int, tiles: Seq[TileCoordinate])(render: TileCoordinate => Image): Unit = {
    if (tiles.nonEmpty) {
      println(s"Generating $layerName for $year: ${tiles.size} missing tiles")
    }

    tiles.zipWithIndex.foreach { case (tile, index) =>
      val outputFile = tileFile(layerRoot, year, tile)
      val folder = outputFile.getParentFile
      ensureFolderExists(folder)
      cleanupTemporaryFiles(outputFile)

      val temporaryFile = temporaryTileFile(outputFile)

      try {
        render(tile).output(temporaryFile)
        moveIntoPlace(temporaryFile, outputFile)
      } finally {
        if (temporaryFile.exists()) {
          val _ = temporaryFile.delete()
        }
      }

      if ((index + 1) == tiles.size || ((index + 1) % 10) == 0) {
        println(s"Progress $layerName $year: ${index + 1}/${tiles.size} tiles")
      }
    }
  }

  private def temporaryTileFile(outputFile: File): File =
    new File(outputFile.getParentFile, s".${outputFile.getName}.${System.nanoTime()}.tmp.png")

  private def cleanupTemporaryFiles(outputFile: File): Unit = {
    val temporaryPrefix = s".${outputFile.getName}."
    val temporaryLegacyName = s".${outputFile.getName}.tmp.png"

    Option(outputFile.getParentFile.listFiles()).getOrElse(Array.empty)
      .filter { file =>
        val fileName = file.getName
        fileName == temporaryLegacyName || (fileName.startsWith(temporaryPrefix) && fileName.endsWith(".tmp.png"))
      }
      .foreach { file =>
        val _ = file.delete()
      }
  }

  private def ensureFolderExists(folder: File): Unit = {
    if (!folder.exists() && !folder.mkdirs()) {
      throw new IllegalStateException(s"Unable to create output directory: ${folder.getAbsolutePath}")
    }
  }

  private def moveIntoPlace(source: File, target: File): Unit = {
    try {
      val _ = Files.move(source.toPath, target.toPath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
    } catch {
      case _: AtomicMoveNotSupportedException =>
        val _ = Files.move(source.toPath, target.toPath, StandardCopyOption.REPLACE_EXISTING)
    }
  }
}