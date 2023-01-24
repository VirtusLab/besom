package besom.annotations

import scala.annotation.StaticAnnotation
import besom.util.NotProvided

class Export(name: String | NotProvided = NotProvided) extends StaticAnnotation
