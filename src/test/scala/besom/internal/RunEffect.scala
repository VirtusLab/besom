package besom.internal

import scala.util.Try

trait RunEffect[F[+_]]:
  def run[A](fa: F[A]): Try[A]
