package besom.cats

// import besom.internal.{Monad, MonadConformanceSpec}
// import cats.effect.*
// import scala.util.Try
// import cats.effect.unsafe.IORuntime
// import scala.concurrent.duration._

// given Monad[IO] = IOMonad()

// class IOMonadConformanceTest extends MonadConformanceSpec[IO]:

//   import cats.effect.unsafe.IORuntime.global

//   def run[A](fa: IO[A]): Try[A] = Try(fa.unsafeRunSync()(using global))

//   def sleep(durationMs: Long): IO[Unit] = IO.sleep(durationMs.millis)
