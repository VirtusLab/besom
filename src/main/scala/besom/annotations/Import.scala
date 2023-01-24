package besom.annotations

import scala.annotation.StaticAnnotation
import besom.util.NotProvided

class Import(name: String | NotProvided = NotProvided, required: Boolean = false, json: Boolean = false)
    extends StaticAnnotation
