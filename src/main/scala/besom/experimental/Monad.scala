package besom.api.experimental

import scala.util.{Either, Right, Left, Try, Success, Failure}

trait Monad[F[+_]]:
  def eval[A](a: => A): F[A]
  def evalTry[A](a: => Try[A]): F[A]
  def evalEither[A](a: => Either[Throwable, A]): F[A]

  def map[A, B](fa: F[A])(f: A => B): F[B]
  def flatMap[A, B](fa: F[A])(f: A => F[B]): F[B]

  def error[A](err: Throwable): F[A]

  def fork[A](fa: => F[A]): F[A]
  def fromFuture[A](futA: => scala.concurrent.Future[A]): F[A]

object Monad:
  def apply[F[+_]: Monad]: Monad[F] = summon[Monad[F]]

extension [F[+_]: Monad, A](fa: F[A])
  def map[B](f: A => B): F[B]        = Monad[F].map(fa)(f)
  def flatMap[B](f: A => F[B]): F[B] = Monad[F].flatMap(fa)(f)
  def fork: F[A]                     = Monad[F].fork(fa)

import scala.concurrent.{ExecutionContext, Future, Promise}

class FutureMonad(implicit val ec: ExecutionContext) extends Monad[Future]:

  def eval[A](a: => A): Future[A] = Future(a)
  def evalTry[A](a: => Try[A]): Future[A] = Future(a).flatMap {
    case Success(v) => Future.successful(v)
    case Failure(t) => Future.failed(t)
  }
  def evalEither[A](a: => Either[Throwable, A]): Future[A] = Future(a).flatMap {
    case Right(r) => Future.successful(r)
    case Left(l)  => Future.failed(l)
  }

  def map[A, B](fa: Future[A])(f: A => B): Future[B]             = fa.map(f)
  def flatMap[A, B](fa: Future[A])(f: A => Future[B]): Future[B] = fa.flatMap(f)

  def error[A](err: Throwable): Future[A] = Future.failed(err)

  def fork[A](fa: => Future[A]): Future[A] = Future.unit.flatMap(_ => fa)

  def fromFuture[A](futA: => Future[A]): Future[A] = Future.unit.flatMap(_ => futA)
