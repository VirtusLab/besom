package yaga.sbt

import _root_.sbt.Project
import java.nio.file.Path

trait YagaDependency {
  def addSelfToProject(baseProject: Project): Project
}
