package observatory

import leaflet.{L, MapOptions, ZoomOptions}

import scala.scalajs.js
import org.scalajs.dom.{Element, Event, Node, document}
import org.scalajs.dom.html.Input

import scalatags.{DataConverters, LowPriorityImplicits}
import scalatags.JsDom.{Cap, Aggregate, tags, attrs, styles}

object Implicits extends Cap with Aggregate with DataConverters with LowPriorityImplicits
import Implicits._

object JSMain {

  private val MapHostId = "climate-observatory-map"
  private val ControlsHostId = "climate-observatory-controls"
  private val LegendHostId = "climate-observatory-legend"

  def main(args: Array[String]): Unit = main()

  def main(): Unit = {
    val availableLayers = Interaction2.availableLayers
    val (radioButtonElement, selectedLayer) = makeRadioButtons(availableLayers)
    val (sliderElement, selectedYear) = makeSlider(selectedLayer)
    val captionElement = makeCaptionElement(selectedLayer)
    setupMap(selectedLayer, selectedYear)
    mountTag(
      ControlsHostId,
      tags.div(
        attrs.cls := "observatory-control-stack",
        tags.div(
          attrs.cls := "observatory-card observatory-card--controls",
          tags.p(attrs.cls := "observatory-section-label")("Climate layers"),
          radioButtonElement,
          sliderElement
        )
      )
    )
    mountTag(
      LegendHostId,
      tags.div(
        attrs.cls := "observatory-card observatory-card--legend",
        tags.p(attrs.cls := "observatory-section-label")("Color scale"),
        captionElement
      )
    )
    ()
  }

  def setupMap(selectedLayer: Signal[Layer], selectedYear: Signal[Int]): Unit = {
    val mapElement = tags.div(styles.height := "100%").render
    val map = L.map(mapElement, js.Dynamic.literal(zoomControl = false, minZoom = 0, maxZoom = 3))
    map.setView(L.latLng(48.0, 14.0), 3)
    L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png").addTo(map)
    val urlSignal = Interaction2.layerUrlPattern(selectedLayer, selectedYear)
    val layer = L.tileLayer(urlSignal())
    layer.addTo(map)
    Signal {
      layer.setUrl(urlSignal())
    }
    map.addControl(L.control.zoom(ZoomOptions(position = "bottomright")))
    mountNode(MapHostId, mapElement)
    map.invalidateSize()
  }

  def makeRadioButtons(availableLayers: Seq[Layer]): (Frag, Signal[Layer]) = {
    val initialValue = availableLayers.head
    val radioButtonValue = Var[Layer](initialValue)
    def makeRadioButton(layer: Layer): Frag =
      tags.label(
        attrs.cls := "observatory-radio",
        tags.input(
          attrs.`type` := "radio",
          attrs.name := "layer",
          attrs.onclick := { (ev: Event) =>
            val input = ev.target.asInstanceOf[Input]
            if (input.checked) radioButtonValue() = layer
          },
          if (layer == initialValue) Some[Modifier](attrs.checked) else Option.empty[Modifier]
        ),
        tags.span(layer.layerName.toString)
      )
    val root =
      tags.div(
        attrs.cls := "observatory-radio-group"
      )(
        for (layer <- availableLayers) yield makeRadioButton(layer)
      )
    (root, radioButtonValue)
  }

  def makeSlider(selectedLayer: Signal[Layer]): (Frag, Signal[Int]) = {
    val yearBounds = Interaction2.yearBounds(selectedLayer)
    val sliderValue = Var[Int](yearBounds().max)
    val input = Signal {
      tags.input(
        attrs.`type` := "range",
        attrs.min := yearBounds().min,
        attrs.max := yearBounds().max,
        attrs.value := yearBounds().max,
        attrs.onchange := { (ev: Event) =>
          val input = ev.target.asInstanceOf[Input]
          sliderValue.update(input.value.toInt)
        },
        styles.width := 40.em
      )
    }

    val selectedYear = Interaction2.yearSelection(selectedLayer, sliderValue)
    val captionSignal = Interaction2.caption(selectedLayer, selectedYear)

    val caption = Signal {
      tags.span(attrs.cls := "observatory-slider-value")(captionSignal())
    }

    val root =
      tags.div(
        attrs.cls := "observatory-slider-group"
      )(
        tags.div(
          attrs.cls := "observatory-slider-header",
          tags.span(attrs.cls := "observatory-slider-label")("Year"),
          caption
        ),
        tags.label(
          attrs.cls := "observatory-slider-control",
          input
        )
      )
    (root, selectedYear)
  }

  def makeCaptionElement(selectedLayer: Signal[Layer]): Frag = {
    Signal {
      tags.div(
        attrs.cls := "observatory-legend-scale"
      )(
        for ((t, Color(red, green, blue)) <- selectedLayer().colorScale.reverse) yield {
          tags.div(
            attrs.cls := "observatory-legend-row"
          )(
            tags.span(attrs.cls := "observatory-legend-value")((if (t > 0) "+" else "") + t + " "),
            tags.span(
              attrs.cls := "observatory-legend-swatch",
              styles.backgroundColor := s"rgb($red, $green, $blue)",
              styles.display.`inline-block`
            )
          )
        }
      )
    }
  }

  private def mountTag(hostId: String, tag: Frag): Unit = {
    mountNode(hostId, tag.render)
  }

  private def mountNode(hostId: String, node: Node): Unit = {
    val host = resolveHost(hostId)
    clearHost(host)
    host.appendChild(node)
  }

  private def resolveHost(hostId: String): Element = {
    Option(document.getElementById(hostId)).getOrElse {
      val fallback = tags.div(attrs.id := hostId).render
      document.body.appendChild(fallback)
      fallback
    }
  }

  private def clearHost(host: Element): Unit = {
    while (host.firstChild != null) {
      host.removeChild(host.firstChild)
    }
  }

  implicit def signalFrag[A](signalA: Signal[A])(implicit aToFrag: A => Frag): Frag = {
    def render(a: A): Node = tags.span(a).render
    var last = render(signalA())
    Signal {
      val current = render(signalA())
      last.replaceChild(current, last.firstChild)
      last = current
    }
    last
  }

}
