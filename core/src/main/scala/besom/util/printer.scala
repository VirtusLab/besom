package besom.util

import besom.internal.Output
import pprint.*

object printer
    extends PPrinter(
      defaultWidth = 140,
      defaultHeight = Int.MaxValue,
      defaultIndent = 2,
      additionalHandlers = { case o: Output[?] =>
        Tree.Literal("Output(?)")
      }
    ):

  override def treeify(x: Any): Tree =
    val result = super.treeify(x)
    result match
      case Tree.Apply(p, body: Iterator[Tree]) =>
        Tree.Apply(
          p,
          body
            .filterNot {
              case Tree.KeyValue("unknownFields", _) => true
              case _                                 => false
            }
        )
      case t => t

  def render(
    x: Any,
    width: Int = defaultWidth,
    height: Int = defaultHeight,
    indent: Int = defaultIndent,
    initialOffset: Int = 0
  ): fansi.Str = this.apply(x, width, height, indent, initialOffset)

end printer
