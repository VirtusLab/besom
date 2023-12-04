package besom.model.hcl2.syntax

import besom.model.hcl2.*

/**
 * LexConfig performs lexical analysis on the given buffer, treating it as a
 * whole HCL config file, and returns the resulting tokens.
 *
 * Only minimal validation is done during lexical analysis, so the returned
 * diagnostics may include errors about lexical issues such as bad character
 * encodings or unrecognized characters, but full parsing is required to
 * detect _all_ syntax errors.
 *
 * @param src The source byte array content to be analysed.
 * @param filename The name of the file being analysed.
 * @param start The position to start analysing.
 * @return A tuple containing the scanned tokens and any diagnostic information.
 */
def lexConfig(src: Vector[Byte], filename: String, start: Pos): (Vector[Token], Vector[Diagnostic]) = {
    val tokens = scanTokens(src, filename, start, scanNormal)
    val diags = checkInvalidTokens(tokens)
    (tokens, diags)
}

/** Represents a sequence of bytes from some HCL code that has been
  * tagged with a type and its range within the source area.
  *
  * @param tokenType Type of the token
  * @param bytes Input sequence of bytes
  * @param range Range of the token in source area
  */
case class Token(tokenType: TokenType, bytes: Vector[Byte], range: Range) {
  /** @return the string representation of the token. */
  override def toString: String = String(bytes, "UTF-8")
}

type TokenType = Char