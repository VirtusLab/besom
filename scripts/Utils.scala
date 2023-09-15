import java.util.concurrent.atomic.AtomicInteger

extension [A](a: A)
  def finalize(f: A => Unit): A = try a
  finally f(a)

class Progress(var label: String, val act: (String, Int) => Unit):
  def increment(amount: Int = 1, lbl: String = label): Unit =
    label = lbl
    act(lbl, amount)

def withProgress[A](label: String, total: Int)(f: Progress ?=> A): A =
  val counter = new AtomicInteger(0)

  val progress = Progress(
    label,
    (lbl, amount) =>
      val current    = counter.addAndGet(amount)
      val percentage = (current.toDouble / total * 100).toInt
      Console.out.synchronized {
        print(s"\r$lbl: $percentage% [$current/$total]")
      }
  )

  try f(using progress)
  finally println()

def reportProgress(using progress: Progress): Unit =
  progress.increment()

def reportProgress(amount: Int)(using progress: Progress): Unit =
  progress.increment(amount)

def reportProgress(label: String)(using progress: Progress): Unit =
  progress.increment(lbl = label)

def reportProgress(amount: Int, label: String)(using progress: Progress): Unit =
  progress.increment(amount, lbl = label)
