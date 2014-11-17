import scala.language.experimental.macros
import scala.reflect.macros.Context
import scala.annotation.StaticAnnotation
import scala.reflect.macros._
import language.experimental.macros

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

class mkCompanion extends StaticAnnotation {
  def macroTransform(annottees: Any*) = macro mkCompanionMacro.impl
}

object mkCompanionMacro {
  def impl(c: Context)(annottees: c.Expr[Any]*): c.Expr[Any] = {
    import c.universe._

    c.warning(c.enclosingPosition, s"expanding annotation")

    val inputs : List[Tree] = annottees.map(_.tree)(collection.breakOut)
    val outputs: List[Tree] = inputs match {
      case (cd @ ClassDef(_, cName, _, _)) :: tail =>
        val mod0: ModuleDef = tail match {
          case (md @ ModuleDef(_, mName, mTemp)) :: Nil
               if cName.decoded == mName.decoded => md

          case Nil =>
            val cMod  = cd.mods
            var mModF = NoFlags
            if (cMod hasFlag Flag.PRIVATE  ) mModF |= Flag.PRIVATE
            if (cMod hasFlag Flag.PROTECTED) mModF |= Flag.PROTECTED
            if (cMod hasFlag Flag.LOCAL    ) mModF |= Flag.LOCAL
            val mMod = Modifiers(mModF, cMod.privateWithin, Nil)

            // XXX TODO: isn't there a shortcut for creating the constructor?
            val mkSuperSelect = Select(Super(This(tpnme.EMPTY), tpnme.EMPTY),
                                       nme.CONSTRUCTOR)
            val superCall     = Apply(mkSuperSelect, Nil)
            val constr        = DefDef(NoMods, nme.CONSTRUCTOR, Nil, List(Nil),
              TypeTree(), Block(List(superCall), Literal(Constant())))

            val mTemp = Template(parents = List(TypeTree(typeOf[AnyRef])),
              self = noSelfType, body = constr :: Nil)
            val mName = TermName(cName.decoded) // or encoded?

            ModuleDef(mMod, mName, mTemp)

          case _ => c.abort(c.enclosingPosition, "Expected a companion object")
        }

        val Template(mTempParents, mTempSelf, mTempBody0) = mod0.impl

        // cf. http://stackoverflow.com/questions/21044957/type-of-a-macro-annottee
        val cTpe        = Ident(TypeName(cd.name.decoded))
        val fooName     = TermName("hasFoo")
        val fooDef      = q"implicit def $fooName: Foo[$cTpe] = ???"
        val mTempBody1  = fooDef :: mTempBody0
        val mTemp1      = Template(mTempParents, mTempSelf, mTempBody1)
        val mod1        = ModuleDef(mod0.mods, mod0.name, mTemp1)

        cd :: mod1 :: Nil

      case _ => c.abort(c.enclosingPosition, "Must annotate a class or trait")
    }

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

    def modifiedClass(c: Context, classDecl: ClassDef, compDeclOpt: Option[ModuleDef]) = {
      // Class modification logic goes here
      val (className, fields) = try {
        val q"case class $className(..$fields) extends ..$bases { ..$body }" = classDecl
        (className, fields)
      } catch {
        case _: MatchError => c.abort(c.enclosingPosition, "Annotation is only supported on case class")
      }
      val format = fields.length match {
        case 0 => c.abort(c.enclosingPosition, "Cannot create json formatter for case class with no fields")
        case 1 =>
          // Only one field, use the serializer for the field
          q"""
            implicit val jsonAnnotationFormat = {
              import play.api.libs.json._
              Format(
                __.read[${fields.head.tpt}].map(s => ${className.toTermName}(s)),
                new Writes[$className] { def writes(o: $className) = Json.toJson(o.${fields.head.name}) }
              )
            }
          """
        case _ =>
          // More than one field, use Play's Json.format[T] macro
          q"implicit val jsonAnnotationFormat = play.api.libs.json.Json.format[$className]"
      }
//      val q"object $obj extends ..$bases { ..$body }" = compDecl
//      q"""
//        object $obj extends ..$bases {
//          ..$body
//          $format
//        }
//      """
    }

    annottees.map(_.tree) match {
      case (classDecl: ClassDef) :: Nil => modifiedClass(c, classDecl, None)
      case (classDecl: ClassDef) :: (compDecl: ModuleDef) :: Nil => modifiedClass(c, classDecl, Some(compDecl))
      case _ => c.abort(c.enclosingPosition, "Invalid annottee")
    }

    val hello: c.Tree = q"""println("hello world!")"""
    c.info(c.enclosingPosition, "Finished macro compilation.", force = true)
    c.Expr(hello)
  }
}