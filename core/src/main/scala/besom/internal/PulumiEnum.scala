package besom.internal

sealed trait PulumiEnum

trait BooleanEnum extends PulumiEnum
trait IntegerEnum extends PulumiEnum
trait NumberEnum extends PulumiEnum
trait StringEnum extends PulumiEnum