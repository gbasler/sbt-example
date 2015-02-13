import scala.collection.immutable.IndexedSeq
import scala.reflect.api.Universe
import scala.reflect.runtime.universe
import scala.tools.reflect.ToolBox

object HelloBlock extends App {
  val mirror = universe.runtimeMirror(getClass.getClassLoader)
  val toolbox = ToolBox(mirror).mkToolBox()

  import toolbox.u._

  val code = reify {
    val a = Array(1, 2, 3)
    a(1) = 1
  }

  def removeArrayOps(u: Universe)(tree: u.Tree): Unit = {
    import u._

    new u.Traverser {
      override def traverse(tree: u.Tree): Unit = tree match {
        case a@Apply(Select(qual, name), _) if name.toString == "update" =>
          println(a)
          println(a.tpe)
          println(qual)
          println(qual.tpe)
          a
        case _ => super.traverse(tree)
      }
    }.traverse(tree)
  }

  //  Apply(Select(Ident(scala.Array), newTermName("apply")), List(Literal(Constant(1))

  println(show(code.tree))
  println(showRaw(code.tree))

  val removed = removeArrayOps(toolbox.u)(code.tree)

  //  println(show(removed.tree))
  //  println(showRaw(removed.tree))

  toolbox.eval(code.tree)

}