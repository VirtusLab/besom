package yaga.extensions.aws.lambda

import besom.types.Archive

class LambdaHandlerMetadata[C, I, O](
  val codeArchive: Archive,
  val handlerClassName: String
)
