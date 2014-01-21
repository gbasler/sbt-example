import scala.language.experimental.macros
import scala.reflect.macros.Context

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