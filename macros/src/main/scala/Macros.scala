import scala.annotation.StaticAnnotation
import scala.language.experimental.macros
import scala.reflect.macros.Context

object Macros {

  import scala.reflect.macros._

  def hello(root: Object): Any = macro impl

  def impl(c: Context)(root: c.Expr[Object]): c.Expr[Any] = {
    import c.universe._

    c.info(c.enclosingPosition, "Compiling macro...", force = true)
    //    val hello = c.universe.reify(println("hello world!"))
    val hello: c.Tree = q"""println("hello world!")"""
    c.info(c.enclosingPosition, "Finished macro compilation.", force = true)
    c.Expr(hello)
  }
}

class identity(prefix: String = "") extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro identityMacro.impl
}

object identityMacro {
  def impl(c: Context)(annottees: c.Expr[Any]*): c.Expr[Any] = {
    import c.universe._
    c.info(c.enclosingPosition, "identityMacro", force = true)
    println("identityMacro")
    val inputs = annottees.map(_.tree).toList
    val (annottee, expandees) = inputs match {
      case (param: ValDef) :: (rest @ (_ :: _)) => (param, rest)
      case (param: TypeDef) :: (rest @ (_ :: _)) => (param, rest)
      case _ => (EmptyTree, inputs)
    }
    println((annottee, expandees))
    val outputs = expandees
    c.Expr[Any](Block(outputs, Literal(Constant(()))))
  }
}


class json extends StaticAnnotation {
  def macroTransform(annottees: Any*) = macro json.impl
}

object json {
  def impl(c: Context)(annottees: c.Expr[Any]*): c.Expr[Any] = {
    import c.universe._

    c.warning(c.enclosingPosition, s"expanding annotation")

    val result = annottees map (_.tree) match {
      case (classDef @ q"$mods class $tpname[..$tparams] $ctorMods(...$paramss) extends { ..$earlydefns } with ..$parents { $self => ..$body }")
        :: Nil if mods.hasFlag(Flag.CASE) =>
        q"""
         $mods class $tpname[..$tparams] $ctorMods(...$paramss) extends { ..$earlydefns } with ..$parents {
            $self =>
            ..$body
            def cpy(other: $tpname) = ???
         }
         """
      case _ => c.abort(c.enclosingPosition, "Invalid annotation target: must be a case class")
    }

    c.Expr[Any](result)
  }
}