package besom.internal

// import scala.util.{Try, Either, Left, Right, Success, Failure}

// trait BesomModule:
//   type M[+A]
//   given F: Monad[M]

//   type Output[A] = besom.internal.Output[M, A]
//   type Context   = besom.internal.Context { type F[A] = M[A] }

//   type Outputs = Map[String, Output[Any]]

//   object Output extends OutputFactory

//   def run(program: Context ?=> Output[Outputs]): M[Unit] = ???

//   def exports(outputs: (String, Output[Any])*): Output[Map[String, Output[Any]]] = ???
