@identity
object Test extends App {
  Macros.hello(this)
}

@identity case class L(a: Int)

object Outer extends App {
  val a = new A(1)
  val b = a.cpy(a)

}

@json case class A(a: Int)