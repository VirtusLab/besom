package besom.languageplugin.test.pulumiapp

object Main {
  def main(args: Array[String]): Unit = {
    // Making sure the plugin is actually on the classpath
    val x = besom.languageplugin.test.resourceplugin.standard.customVal
    val y = besom.languageplugin.test.resourceplugin.external.customVal

    // Throwing exception instead of printing because 'pulumi up' swallows stdout 
    throw new Exception("scala executor test got executed")
  }
}
