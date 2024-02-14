package besom.auto

case class RemoteStack private (name: String, workspace: Workspace)(private val stack: Stack):
  def name = s.name
  // TODO
object RemoteStack:
  private[auto] def apply(s: Stack): RemoteStack = RemoteStack(s.name, s.workspace)(s)