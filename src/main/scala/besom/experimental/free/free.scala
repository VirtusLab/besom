package besom.api.experimental.free

trait Lift[F[+_]]:
  def toMonad[A](fa: F[A]): Output.FromFuture[A]

trait Runtime[F[+_]]:
  def pure[Out](out: Out): F[Out]
  def fail(err: Throwable): F[Nothing]
  def defer[Out](thunk: => Out): F[Out]
  def flatMap[In, Out](fa: F[In])(f: In => F[Out]): F[Out]
  def recover[Out](fa: F[Out])(f: Throwable => F[Out]): F[Out]
  def fromFuture[Out](f: => scala.concurrent.Future[Out]): F[Out]
  def fork[Out](fa: F[Out]): F[Out]

enum Output[+Out]:
  private case Pure(value: Out)
  private case Fail(err: Throwable) extends Output[Nothing]
  private case Defer(thunk: () => Out)
  private case FlatMap[In, +Out](fa: Output[In], f: In => Output[Out]) extends Output[Out]
  private case Recover(fa: Output[Out], f: Throwable => Output[Out])
  private[free] case FromFuture(fut: () => scala.concurrent.Future[Out])
  private case Fork(fa: Output[Out])
  // private case Lifted[F[+_], In, +Out](fa: Output[In], f: In => F[Out], lift: Lift[F]) extends Output[Out]

  def flatMap[Out2](f: Out => Output[Out2]): Output[Out2] =
    FlatMap(this, f)

  def map[Out2](f: Out => Out2): Output[Out2] = flatMap { out =>
    try {
      Pure(f(out))
    } catch {
      case err: Throwable => Fail(err)
    }
  }

  def recover[Out2 >: Out](f: Throwable => Output[Out2]): Output[Out2] =
    Recover(this, f)

  def fork: Output[Out] = Fork(this)

  // def lifted[F[+_], Out2](f: Out => F[Out2])(using Lift[F]): Output[Out2] =
    // Lifted(this, f, summon[Lift[F]])

  def run[F[+_]](using F: Runtime[F]): F[Out] = this match
    case Pure(value)         => F.pure(value)
    case Fail(err)           => F.fail(err).asInstanceOf[F[Out]]
    case Defer(thunk)        => F.defer(thunk())
    case FlatMap(fa, f)      => F.flatMap(fa.run[F])(a => f(a).run[F])
    case Recover(fa, f)      => F.recover(fa.run[F])(err => f(err).run[F])
    case FromFuture(fut)     => F.fromFuture(fut())
    case Fork(fa)            => F.fork(fa.run[F])
    // case Lifted(fa, f, lift) => F.flatMap(fa.run[F])(f)

object Output:
  def pure[Out](value: Out): Output[Out] = Output.Pure(value)
  def fail(err: Throwable): Output[Nothing] = Output.Fail(err)
  def defer[Out](thunk: => Out): Output[Out] = Output.Defer(() => thunk)
  def fromFuture[Out](thunk: => scala.concurrent.Future[Out]): Output[Out] = Output.FromFuture(() => thunk)