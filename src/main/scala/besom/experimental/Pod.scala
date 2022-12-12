package besom.api.experimental

import liftable.*

case class Pod(id: Output[String], ports: Output[List[Int]])
