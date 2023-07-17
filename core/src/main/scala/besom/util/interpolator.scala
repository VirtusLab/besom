package besom.util

import besom.internal.*

private[util] def interleave[T](xs: List[T], ys: List[T]): List[T] = (xs, ys) match
  case (Nil, _) => ys
  case (_, Nil) => xs
  case (x :: xs, y :: ys) => x :: y :: interleave(xs, ys)

extension (sc: StringContext)
  def pulumi(args: Any*)(using Context): Output[String] =
    interleave(sc.parts.toList, args.toList).foldLeft(Output("")){ case (acc, e) => e match
      case o: Output[?] => acc.flatMap(s => o.map(s + _.toString))
      case s: Any => acc.map(_ + s.toString)
    }

  def p(args: Any*)(using Context): Output[String] = pulumi(args*)

extension (output: Output[String])
  def stripMargin: Output[String] = output.map(_.stripMargin)
