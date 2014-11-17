import scala.language.experimental.macros
import scala.reflect.macros.Context
import scala.reflect.macros.Context
import scala.language.experimental.macros
import scala.annotation.StaticAnnotation

object Macros {
  def hello(root: Object) = macro impl

  def impl(c: Context)(root: c.Expr[Object]): c.Expr[Any] = {
    import c.universe._
    c.info(c.enclosingPosition, "Compiling macro...", force = true)
    //    val hello = c.universe.reify(println("hello world!"))
    val hello: c.Tree = q"""println("hello world!")"""
    c.info(c.enclosingPosition, "Finished macro compilation.", force = true)
    c.Expr(hello)
  }
}

class identity extends StaticAnnotation {
  def macroTransform(annottees: Any*) = macro identityMacro.impl
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