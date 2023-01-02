package besom.zio

// import besom.internal.{Monad, MonadConformanceSpec}
// import zio.*
// import scala.util.Try
// import scala.concurrent.duration._

// given Monad[Task] = ZIOMonad()

// class IOMonadConformanceTest extends MonadConformanceSpec[Task]:

//   def run[A](fa: Task[A]): Try[A] = Try(Unsafe.unsafe { implicit unsafe =>
//     zio.Runtime.default.unsafe.run(fa).getOrThrow()
//   })

//   def sleep(durationMs: Long): Task[Unit] = ZIO.sleep(durationMs.millis)
