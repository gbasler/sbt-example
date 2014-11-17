@identity
object Test extends App {
  Macros.hello(this)
  println(new C)
}

@identity class C