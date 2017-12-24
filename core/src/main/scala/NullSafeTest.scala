class A(val s: String)

class B(val a: A)

class C(val b: B)

import NullSafeMacro.nullSafe

object NullSafeMacroTest extends App {

  val a = new A(null)
  val o1 = nullSafe(a.s.toLowerCase)

  val b = new B(a)
  val c = new C(b)
  val o2 = nullSafe(c.b.a.s.toLowerCase)

}