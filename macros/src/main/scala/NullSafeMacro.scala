import scala.reflect.macros.whitebox.Context
import scala.language.experimental.macros

/**
  * Returns None if the given expression returns a NPE.
  * Shamelessly copied from https://stackoverflow.com/questions/15775699/how-to-develop-macro-to-short-circuit-null
  */
object NullSafeMacro {

  def nullSafe[T](expr: T): Option[T] = macro withNullGuards_impl[T]

  def withNullGuards_impl[T](c: Context)(expr: c.Expr[T]): c.Expr[Option[T]] = {
    import c.universe._

    def eqOp = TermName("==").encodedName
    def nullTree = c.literalNull.tree
    def noneTree = reify(None).tree
    def someApplyTree = Select(reify(Some).tree, TermName("apply"))

    def wrapInSome(tree: Tree) = Apply(someApplyTree, List(tree))

    def canBeNull(tree: Tree) = {
      val sym = tree.symbol
      val tpe = tree.tpe

      sym != null &&
        !sym.isModule && !sym.isModuleClass &&
        !sym.isPackage && !sym.isPackageClass &&
        !(tpe <:< typeOf[AnyVal])
    }

    def isInferredImplicitConversion(apply: Tree, fun: Tree, arg: Tree) =
      fun.symbol.isImplicit && (!apply.pos.isDefined || apply.pos == arg.pos)

    def nullGuarded(originalPrefix: Tree, prefixTree: Tree, whenNonNull: Tree => Tree): Tree =
      if (canBeNull(originalPrefix)) {
        val prefixVal = c.freshName()
        Block(
          ValDef(Modifiers(), prefixVal, TypeTree(null), prefixTree),
          If(
            Apply(Select(Ident(prefixVal), eqOp), List(nullTree)),
            noneTree,
            whenNonNull(Ident(prefixVal))
          )
        )
      } else whenNonNull(prefixTree)

    def addNullGuards(tree: Tree, whenNonNull: Tree => Tree): Tree = tree match {
      case Select(qualifier, name) =>
        addNullGuards(qualifier, guardedQualifier =>
          nullGuarded(qualifier, guardedQualifier, prefix => whenNonNull(Select(prefix, name))))
      case Apply(fun, List(arg)) if (isInferredImplicitConversion(tree, fun, arg)) =>
        addNullGuards(arg, guardedArg =>
          nullGuarded(arg, guardedArg, prefix => whenNonNull(Apply(fun, List(prefix)))))
      case Apply(Select(qualifier, name), args) =>
        addNullGuards(qualifier, guardedQualifier =>
          nullGuarded(qualifier, guardedQualifier, prefix => whenNonNull(Apply(Select(prefix, name), args))))
      case Apply(fun, args) =>
        addNullGuards(fun, guardedFun => whenNonNull(Apply(guardedFun, args)))
      case _ => whenNonNull(tree)
    }

    val tree1 = addNullGuards(expr.tree, tree => wrapInSome(tree))
    c.echo(c.enclosingPosition, s"Nullsafe expansion:\n${tree1}")
    c.Expr[Option[T]](tree1)
  }
}
