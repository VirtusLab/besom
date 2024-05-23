package besom.util

import besom.internal.Output
import pprint.*

private def escapeSpecialCharacters(input: String): String =
  input.flatMap {
    case '\n'               => "\\n"
    case '\t'               => "\\t"
    case '\r'               => "\\r"
    case '\b'               => "\\b"
    case '\f'               => "\\f"
    case '\u001B'           => "\\x1b"
    case ch if ch.isControl => "\\u%04x".format(ch.toInt) // other control characters
    case ch                 => ch.toString
  }

object printer
    extends PPrinter(
      defaultWidth = 140,
      defaultHeight = Int.MaxValue,
      defaultIndent = 2,
      additionalHandlers = {
        case o: Output[?]            => Tree.Literal("Output(?)")
        case s: String if s.nonEmpty => Tree.Literal(escapeSpecialCharacters(s))
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
  ): fansi.Str =
    try this.apply(x, width, height, indent, initialOffset)
    catch
      case e: Exception =>
        fansi.Color.Red("Error rendering: ") ++ fansi.Str(e.toString) ++ fansi.Str("\n") ++ fansi.Str(e.getStackTrace.mkString("\n"))

end printer
