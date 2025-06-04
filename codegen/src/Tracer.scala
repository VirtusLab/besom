package besom.codegen

import scala.jdk.CollectionConverters.*

object Tracer:
  private var enabled = false

  def enable(): Unit     = enabled = true
  def disable(): Unit    = enabled = false
  def isEnabled: Boolean = enabled

  private val stack = new java.util.concurrent.ConcurrentLinkedDeque[TraceNode]
  private val roots = java.util.Collections.synchronizedList(
    new java.util.ArrayList[TraceNode]()
  )

  inline def time[T](name: String)(body: => T): T =
    if !enabled then body
    else
      val node = TraceNode(name)
      push(node)
      val start = System.nanoTime()
      try body
      finally
        val dur = System.nanoTime() - start
        node.elapsedNanos = node.elapsedNanos + dur // single writer – safe
        val _ = stack.pop() // discard the popped node

  private def push(n: TraceNode): Unit =
    val parent = stack.peek
    if parent != null then parent.children += n
    else roots.add(n)
    stack.push(n)

  def rootNodes: List[TraceNode] =
    roots.asScala.toList

  def renderTree(): Unit =
    if !enabled then return
    def formatDuration(nanos: Long): String =
      val ms = nanos / 1_000_000
      if ms < 1000 then f"$ms%6d ms"
      else if ms < 60_000 then f"${ms / 1000.0}%6.2f s"
      else
        val minutes = ms / 60_000
        val seconds = (ms % 60_000) / 1000.0
        f"$minutes%2d min ${seconds}%4.2f s"

    def render(n: TraceNode, indent: Int): Unit =
      println(" " * indent + f"${formatDuration(n.elapsedNanos)}  ${n.name}")
      n.children.foreach(render(_, indent + 2))
    rootNodes.foreach(render(_, 0))
end Tracer

final class TraceNode(
  val name: String,
  var elapsedNanos: Long = 0L, // rolled-up time
  val children: scala.collection.mutable.Buffer[TraceNode] = scala.collection.mutable.Buffer.empty
)
