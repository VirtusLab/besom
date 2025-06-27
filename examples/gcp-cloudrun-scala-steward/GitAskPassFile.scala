object GitAskPassFile:
  private val passFileName      = "pass.sh"
  private val gitPassFolderPath = "/opt/git"
  val gitPassEnvName            = "GIT_PASSWORD"
  val gitPassPath               = s"$gitPassFolderPath/$passFileName"

  val gitPassFile: String =
    List(
      s"mkdir $gitPassFolderPath",
      s"echo '#!/bin/sh' >> $gitPassPath",
      s"echo 'echo $$$gitPassEnvName' >> $gitPassPath",
      s"chmod +x $gitPassPath"
    ).mkString(" && ")
