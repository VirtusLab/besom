package yaga.codegen.core.generator

import yaga.codegen.core.generator.scalameta

import tastyquery.Types.*
import tastyquery.Contexts.*

import scala.meta

object ScalaMetaUtils:
  def packageRefFromParts(parts: Seq[String]): meta.Term.Ref =
    // Do we need to sanitize the parts somehow?
    scalameta.ref(parts*)
