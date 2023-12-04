package besom.model.hcl2

import besom.model.hcl2.internal.InternalTree

trait Tree extends InternalTree:
  def parent: Option[Tree]
  def children: List[Tree]

/** Represents a single definition in a Scope.
  */
trait Definition {

  /** Returns the syntax node.
    *
    * @return
    *   the syntax node
    */
  def syntaxNode: Node
}

/** Node is the trait that every AST node extends. This trait is sealed, so it cannot be extended from outside of this package.
  */
sealed trait Node {

  /** @return
    *   the [[Range]]
    */
  def range: Range
}

/** Represents a single position within a source file, defined at the start byte of a unicode character encoded in UTF-8.
  *
  * This class is typically used only within the context of a Range, which defines the source file associated with the position.
  *
  * @param line
  *   Line number in the source code where the position is placed. Lines are counted starting at 1 and increment for each newline character
  *   encountered.
  * @param column
  *   Column number in the source code at the position, counting in unicode characters, started at 1. Columns are counted visually, so a
  *   latin letter with a combining diacritic mark counts as one character. Specifically, columns are count as grapheme clusters as used in
  *   unicode normalization.
  * @param byte
  *   Byte offset into the file where the indicated character begins. This is a zero-based offset to the first byte of the first UTF-8
  *   codepoint sequence in the character, providing a position that can be resolved without awareness of Unicode characters.
  */
sealed case class Pos(line: Int, column: Int, byte: Int)

/** InitialPos is a suitable position to use to mark the start of a file. */
object InitialPos extends Pos(0, 1, 1)

/** Class [[Range]] represents a span of characters between two positions in a source. This class is usually used by value in types that
  * represent AST nodes, but by pointer in types that refer to the positions of other objects.
  *
  * @return
  *   create a new range with a filename, start and end.
  * @param filename
  *   the name of the file into which this range's positions point.
  * @param start
  *   represents the start of this range. Start is inclusive.
  * @param end
  *   represents the end of this range. End is exclusive.
  */
case class Range(
  filename: String,
  start: Pos,
  end: Pos
)
