package besom.auto.internal

import org.virtuslab.yaml.Node
import org.virtuslab.yaml.internal.load.parse.EventKind
import org.virtuslab.yaml.internal.load.parse.EventKind._
import org.virtuslab.yaml.internal.dump.serialize.Serializer

object HackedSerializerImpl extends Serializer {
  override def toEvents(node: Node): Seq[EventKind] =
    Seq(DocumentStart()) ++ convertNode(node) ++ Seq(DocumentEnd())

  private def convertNode(node: Node) = node match {
    case scalar: Node.ScalarNode     => convertScalarNode(scalar)
    case mapping: Node.MappingNode   => convertMappingNode(mapping)
    case sequence: Node.SequenceNode => convertSequenceNode(sequence)
  }

  private def convertMappingNode(node: Node.MappingNode): Seq[EventKind] = {
    val events = node.mappings.toSeq.flatMap {
      case (_, Node.ScalarNode(null, _))             => Seq.empty
      case (_, Node.SequenceNode(s, _)) if s.isEmpty => Seq.empty
      case (_, Node.MappingNode(m, _)) if m.isEmpty  => Seq.empty
      case (k, v)                                    => Seq(convertNode(k), convertNode(v))
    }.flatten
    Seq(MappingStart()) ++ events ++ Seq(MappingEnd)
  }

  private def convertSequenceNode(node: Node.SequenceNode): Seq[EventKind] = {
    val events = node.nodes.flatMap(convertNode(_))
    Seq(SequenceStart()) ++ events ++ Seq(SequenceEnd)
  }

  private def convertScalarNode(node: Node.ScalarNode): Seq[EventKind] =
    Seq(Scalar(node.value))
}
