package yaga.sbt

import _root_.sbt._
import _root_.sbt.Keys._
import _root_.sbt.AutoPlugin

object YagaAwsLambdaPlugin extends AutoPlugin {
  override def trigger = allRequirements

  object autoImport {
    implicit class ProjectYagaOps(project: Project) {
      def withYagaDependencies(dependencies: YagaDependency*) = {
        dependencies.foldLeft(project) { (acc, dep) =>
          dep.addSelfToProject(acc)
        }
      }
    }
  }
}
