package besom.internal

// import besom.FutureMonad
// import scala.concurrent._, duration._, ExecutionContext.Implicits.global
// import scala.util.Try
// import java.util.concurrent.Executor
// import java.util.concurrent.Executors

// given ExecutionContext = ExecutionContext.fromExecutor(Executors.newSingleThreadExecutor())
// given FutureMonad()

// class FutureMonadConformanceTest extends MonadConformanceSpec[Future], FutureMonadHelpers:
//   def run[A](fa: Future[A]): Try[A] = Try(Await.result(fa, Duration.Inf))
