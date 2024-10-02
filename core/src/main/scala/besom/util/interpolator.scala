package besom.util

import besom.internal.*

object interpolator:
  private[util] def interleave[T](xs: List[T], ys: List[T]): List[T] = (xs, ys) match
    case (Nil, _)           => ys
    case (_, Nil)           => xs
    case (x :: xs, y :: ys) => x :: y :: interleave(xs, ys)

  implicit final class PulumiInterpolationOps(sc: StringContext):
    def pulumi(args: Any*): Output[String] =
      interleave(sc.parts.toList, args.toList).foldLeft(Output.pure("")) { case (acc, e) =>
        e match
          case o: Output[?] => acc.flatMap(s => o.map(s + _.toString))
          case s: Any       => acc.map(_ + s.toString)
      }

    def p(args: Any*): Output[String] = pulumi(args*)

  implicit final class OutputStringStripMarginOps(output: Output[String]):
    def stripMargin: Output[String] = output.map(_.stripMargin)
