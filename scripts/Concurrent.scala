import scala.concurrent.*, duration.*
import scala.collection.Factory

import ExecutionContext.Implicits.global

extension [A](futA: Future[A])
  def await: A   = Await.result(futA, Duration.Inf)
  def done: Unit = Await.result(futA, Duration.Inf)

extension [A, CC[_]](xs: CC[A])(using ev: CC[A] <:< Iterable[A])
  def parForeach(f: A => Unit): Unit =
    Future.sequence(xs.map(x => Future(f(x)))).await

  def parMap[B](f: A => B)(using factory: Factory[B, CC[B]]): CC[B] =
    factory.fromSpecific(Future.sequence(xs.map(x => Future(f(x)))).await)

  def parMap[B](maxConcurrent: Int)(f: A => B)(using factory: Factory[B, CC[B]]): CC[B] =
    val futuresQueue = new scala.collection.mutable.Queue[Future[B]]()
    val builder      = factory.newBuilder
    val iterator     = xs.iterator

    while (iterator.hasNext) do
      if futuresQueue.size < maxConcurrent then
        val next = iterator.next()
        futuresQueue.enqueue(Future(f(next)))
      else
        futuresQueue.dequeueFirst(_.isCompleted) match
          case Some(fut) =>
            val b = Await.result(fut, Duration.Inf)
            builder += b
          case None =>
            val b = Await.result(futuresQueue.dequeue(), Duration.Inf)
            builder += b

    futuresQueue.foreach { f =>
      val b = Await.result(f, Duration.Inf)
      builder += b
    }

    builder.result()
