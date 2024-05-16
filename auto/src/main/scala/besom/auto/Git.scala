package besom.auto

import besom.util.*
import com.jcraft.jsch.JSch
import org.eclipse.jgit.api.{CloneCommand, TransportConfigCallback}
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.transport.ssh.jsch.JschConfigSessionFactory
import org.eclipse.jgit.transport.{CredentialsProvider, RefSpec, SshTransport, UsernamePasswordCredentialsProvider}

import java.util.Collections
import scala.util.Using

object Git:
  private val RefPrefix       = "refs/"
  private val RefHeadPrefix   = RefPrefix + "heads/"
  private val RefTagPrefix    = RefPrefix + "tags/"
  private val RefRemotePrefix = RefPrefix + "remotes/"

  private class RichCloneCommand(private var depth: Option[Int] = None) extends CloneCommand:
    def getCredentialsProvider: CredentialsProvider         = credentialsProvider
    def getTransportConfigCallback: TransportConfigCallback = transportConfigCallback
    def getDepth: Option[Int]                               = depth
    override def setDepth(depth: Int): CloneCommand =
      val cmd = super.setDepth(depth)
      this.depth = Some(depth)
      cmd

  def setupGitRepo(workDir: os.Path, repoArgs: GitRepo): Either[Exception, os.Path] =
    try
      val cloneCommand = new RichCloneCommand()

      cloneCommand
        .setRemote("origin") // be explicit so we can require it in remote refs
        .setURI(repoArgs.url)
        .setDirectory(workDir.toIO)

      if repoArgs.shallow then
        cloneCommand
          .setDepth(1)
          .setCloneAllBranches(false)

      repoArgs.auth match
        case GitAuth.UsernameAndPassword(username, password) =>
          cloneCommand.setCredentialsProvider(new UsernamePasswordCredentialsProvider(username, password))
        case GitAuth.PersonalAccessToken(token) =>
          // With Personal Access Token the username for use with a PAT can be
          // *anything* but an empty string so we are setting this to 'git'
          cloneCommand.setCredentialsProvider(new UsernamePasswordCredentialsProvider("git", token))
        case GitAuth.SSHPrivateKey(key, passphrase) =>
          val sshSessionFactory = new JschConfigSessionFactory():
            override protected def configureJSch(jsch: JSch): Unit =
              jsch.removeAllIdentity()
              jsch.addIdentity("key", key.getBytes(), null /* no pub key */, passphrase.asOption.map(_.getBytes()).orNull)
          cloneCommand.setTransportConfigCallback { transport =>
            transport.asInstanceOf[SshTransport].setSshSessionFactory(sshSessionFactory)
          }
        case GitAuth.SSHPrivateKeyPath(keyPath, passphrase) =>
          val sshSessionFactory = new JschConfigSessionFactory():
            override protected def configureJSch(jsch: JSch): Unit =
              jsch.removeAllIdentity()
              jsch.addIdentity(keyPath, passphrase.asOption.map(_.getBytes()).orNull)
          cloneCommand.setTransportConfigCallback { transport =>
            transport.asInstanceOf[SshTransport].setSshSessionFactory(sshSessionFactory)
          }
        case NotProvided => // do nothing
      repoArgs.branch match
        case branch: String =>
          // `Git.cloneRepository` will do appropriate fetching given a branch name. We must deal with
          // different varieties, since people have been advised to use these as a workaround while only
          // "refs/heads/<default>" worked.
          //
          // If a reference name is not supplied, then clone will fetch all refs (and all objects
          // referenced by those), and checking out a commit later will work as expected.
          val ref = {
            try
              val refSpec = RefSpec(branch)
              if refSpec.matchSource(RefRemotePrefix)
              then
                refSpec.getDestination match
                  case s"origin/$branch" => s"$RefHeadPrefix/$branch"
                  case _                 => throw AutoError("a remote ref must begin with 'refs/remote/origin/', but got: '$branch'")
              else if refSpec.matchSource(RefTagPrefix) then
                branch // looks like `refs/tags/v1.0.0` -- respect this even though the field is `.Branch`
              else if !refSpec.matchSource(RefHeadPrefix) then
                s"$RefHeadPrefix/$branch" // not a remote, not refs/heads/branch; treat as a simple branch name
              else
                // already looks like a full branch name or tag, so use as is
                refSpec.toString
            catch case e: IllegalArgumentException => throw AutoError(s"Invalid branch name: '$branch'", e)
          }
          cloneCommand.setBranchesToClone(Collections.singletonList(ref))

        case NotProvided => // do nothing
      end match
      // NOTE: pulumi has a workaround here for Azure DevOps requires, we might add if needed
      val git        = cloneCommand.call()
      val repository = git.getRepository

      repoArgs.commitHash match
        case commitHash: String =>
          // ensure that the commit has been fetched
          val fetchCommand = git
            .fetch()
            .setRemote("origin")
            .setRefSpecs(new RefSpec(s"$commitHash:$commitHash"))
            .setCredentialsProvider(cloneCommand.getCredentialsProvider)
            .setTransportConfigCallback(cloneCommand.getTransportConfigCallback)

          cloneCommand.getDepth.foreach {
            fetchCommand.setDepth(_)
          }
          val _ = fetchCommand.call()

          // If a commit hash is provided, then we must check it out explicitly. Otherwise, the
          // repository will be in a detached HEAD state, and the commit hash will be the only
          // commit in the repository.
          val commitId = ObjectId.fromString(commitHash)
          Using.resource(new RevWalk(repository)) { revWalk =>
            val commit = revWalk.parseCommit(commitId)
            val _ = git
              .checkout()
              .setName(commit.getName)
              .setForced(true) // this method guarantees 'git --force' semantics
              .call()
          }
        case NotProvided => // do nothing
      end match

      val finalWorkDir =
        if repoArgs.projectPath.asOption.nonEmpty then
          val projectPath = os.rel / repoArgs.projectPath.asOption.get
          workDir / projectPath
        else workDir

      Right(finalWorkDir)

    catch case e: Exception => Left(e)

  end setupGitRepo
end Git
