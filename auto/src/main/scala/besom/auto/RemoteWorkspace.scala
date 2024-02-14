package besom.auto

trait RemoteWorkspace extends LocalWorkspace
object RemoteWorkspace {
  /** Creates a new [[RemoteWorkspace]] with the default options.
   *
   * @param options
   * the configuration options for the workspace
   * @return
   * a new [[RemoteWorkspace]]
   */
  def apply(options: RemoteWorkspaceOption*): Either[Exception, RemoteWorkspace] =
    remoteToLocalOptions(options)
      .map(LocalWorkspace(_))
      .map(_.asInstanceOf[RemoteWorkspace]) // FIXME: This is a hack

  def remoteToLocalOptions(options: RemoteToLocalOption*): Either[Exception, List[LocalWorkspaceOption]] = ???
}

/** The configuration options for a [[RemoteWorkspace]].
  * @see
  *   [[RemoteWorkspaceOptions]]
  */
sealed trait RemoteWorkspaceOption
object RemoteWorkspaceOption

case class RemoteWorkspaceOptions()
object RemoteWorkspaceOptions