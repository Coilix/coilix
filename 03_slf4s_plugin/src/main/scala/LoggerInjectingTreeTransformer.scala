package io.github.coilix.slf4s

import dotty.tools.dotc.ast.untpd
import dotty.tools.dotc.ast.untpd.*
import dotty.tools.dotc.core.Contexts.Context
import dotty.tools.dotc.util.SourceFile

final class LoggerInjectingTreeTransformer(
  detector:     Slf4sAnnotationDetector,
  fieldFactory: LoggerFieldFactory
) extends Slf4sTreeTransformer {

  override def transform(tree: Tree, source: SourceFile)(using Context): Tree =
    transformIn(tree, Nil, source)

  private def transformIn(tree: Tree, owner: List[String], source: SourceFile)(using Context): Tree =
    tree match {
      case pkg:    PackageDef                => transformPackage(pkg, owner, source)
      case cls:    TypeDef if cls.isClassDef => transformClass(cls, owner, source)
      case module: ModuleDef                 => transformModule(module, owner, source)
      case other => other
    }

  private def transformPackage(pkg: PackageDef, owner: List[String], source: SourceFile)(using Context): Tree = {
    val path = owner ++ packagePath(pkg.pid)
    untpd.cpy.PackageDef(pkg)(pkg.pid, pkg.stats.map(transformIn(_, path, source)))
  }

  private def transformClass(cls: TypeDef, owner: List[String], source: SourceFile)(using Context): Tree =
    cls.rhs match {
      case template: Template => untpd.cpy.TypeDef(cls)(rhs = transformTemplate(cls, template, owner :+ cls.name.toString, source))
      case _ => cls
    }

  private def transformModule(module: ModuleDef, owner: List[String], source: SourceFile)(using Context): Tree =
    untpd.cpy.ModuleDef(module)(module.name, transformTemplate(module, module.impl, owner :+ module.name.toString, source))

  private def transformTemplate(definition: MemberDef, template: Template, path: List[String], source: SourceFile)(using
    Context
  ): Template =
    untpd.cpy.Template(template)(body = withLogger(definition, template.body.map(transformIn(_, path, source)), path, source))

  private def withLogger(definition: MemberDef, body: List[Tree], path: List[String], source: SourceFile)(using
    Context
  ): List[Tree] =
    if (needsLogger(definition, body)) fieldFactory.create(path.mkString("."), source) :: body
    else body

  private def needsLogger(definition: MemberDef, body: List[Tree])(using Context): Boolean =
    detector.isAnnotated(definition) && !body.exists(isLoggerField)

  private def isLoggerField(tree: Tree): Boolean =
    tree match {
      case field: ValDef => field.name == LoggerField.name
      case _ => false
    }

  private def packagePath(tree: Tree): List[String] =
    tree match {
      case select: Select => packagePath(select.qualifier) :+ select.name.toString
      case ident:  Ident  => List(ident.name.toString)
      case _ => Nil
    }
}
