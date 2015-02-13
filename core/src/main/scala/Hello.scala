import scala.reflect.runtime.universe
import scala.tools.reflect.ToolBox

object Hello extends App {
  val mirror = universe.runtimeMirror(getClass.getClassLoader)
  val toolbox = ToolBox(mirror).mkToolBox()
  import toolbox.u._

  val code = reify {
    println("Hello world")
  }

  val compiledCode = toolbox.compile(code.tree)

  compiledCode()
}