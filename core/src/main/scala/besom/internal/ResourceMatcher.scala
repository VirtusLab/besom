package besom.internal

import scala.reflect.Typeable

trait ResourceMatcher:
  def matchesResource(resourceInfo: TransformedResourceInfo): Boolean

trait TypedResourceMatcher extends ResourceMatcher:
  self =>

  type Args

  def matchesResource(resourceInfo: TransformedResourceInfo): Boolean

  def transformArgs(fun: TransformedResourceInfo ?=> Args => Args): ResourceArgsTransformation =
    new ResourceArgsTransformation:
      def transformArgs(args: Any)(using info: TransformedResourceInfo): Any =
        if self.matchesResource(info) then fun(args.asInstanceOf[Args])
        else args

object TypedResourceMatcher:
  extension (matcher: TypedResourceMatcher)
    def when(filter: TransformedResourceInfo => Boolean): TypedResourceMatcher { type Args = matcher.Args } =
      new TypedResourceMatcher:
        type Args = matcher.Args

        override def matchesResource(resourceInfo: TransformedResourceInfo): Boolean =
          matcher.matchesResource(resourceInfo) && filter(resourceInfo)

    def whenNamed(name: String): TypedResourceMatcher { type Args = matcher.Args } =
      when(_.name == name)

    def transformNestedArgs[A](using typeable: Typeable[A], argsEncoder: ArgsEncoder[A])(
      fun: TransformedResourceInfo ?=> PartialFunction[A, A]
    )(using updater: Update[matcher.Args]): ResourceArgsTransformation =
      new ResourceArgsTransformation:
        def transformArgs(args: Any)(using info: TransformedResourceInfo): Any =
          if matcher.matchesResource(info) then
            val untypedFun: PartialFunction[Any, Any] = new PartialFunction[Any, Any] {
              def isDefinedAt(x: Any) =
                x match {
                  case a: A => fun.isDefinedAt(a)
                  case _    => false
                }
              def apply(x: Any) = fun(using info)(x.asInstanceOf[A])
            }

            updater.update(args.asInstanceOf[matcher.Args], untypedFun)
          else args
