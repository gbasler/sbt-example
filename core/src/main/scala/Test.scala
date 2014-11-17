object Test extends App {
  Macros.hello(this)
  println(Bar)
  println(new A(1, "2"))

  object Bar
  @mkCompanion class Bar

  assert(Foo.hasFoo == 33)
}

object Bar

@mkCompanion class Bar

@json case class A(field1: Int, field2: String)
