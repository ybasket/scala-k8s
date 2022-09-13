package dev.hnaderi.k8s.generator

import DataModel.{Resource, SubResource, MetaResource, Primitive}

object ObjectGenerator {
  private implicit class HeaderWriter(val obj: DataModel) extends AnyVal {
    def header(imports: String*): String = {
      val sanitizedPkg = obj.pkg.replace('-', '_')

      s"""package $sanitizedPkg

${imports.map(s => s"import $s\n").mkString}
${Utils.generateDescription(obj.description)}"""
    }
  }

  def printProps: Seq[ModelProperty] => String =
    _.map(_.asParam).mkString(",\n  ")

  private def builderMethod(className: String, prop: ModelProperty): String = {
    val value = if (prop.required) "value" else "Some(value)"
    import prop.fieldName
    import prop.typeName
    import prop.capitalName

    def result = {
      val construct = if (typeName.isArray) "" else ".toMap"
      if (prop.required) s"$fieldName ++ newValues"
      else s"Some($fieldName.fold(newValues$construct)(_ ++ newValues))"
    }

    val helpers = typeName match {
      case ModelPropertyType.Object(valueType) =>
        s"""
  /** Adds new values to $fieldName */
  def add$capitalName(newValues: (String, $valueType)*) : $className = copy($fieldName = $result)"""
      case ModelPropertyType.List(valueType) =>
        s"""
  /** Appends new values to $fieldName */
  def add$capitalName(newValues: $valueType*) : $className = copy($fieldName = $result)"""
      case _ => ""
    }

    val transforms =
      if (prop.required) s"""
  /** transforms $fieldName to result of function */
  def map$capitalName(f: ${typeName.name} => ${typeName.name}) : $className = copy($fieldName = f($fieldName))"""
      else s"""
  /** if $fieldName has a value, transforms to the result of function*/
  def map$capitalName(f: ${typeName.name} => ${typeName.name}) : $className = copy($fieldName = $fieldName.map(f))"""

    s"""
  /** Returns a new data with $fieldName set to new value */
  def with$capitalName(value: ${typeName.name}) : $className = copy($fieldName = $value)$helpers$transforms"""
  }
  private def builderMethods(
      className: String,
      props: Seq[ModelProperty]
  ): String = props
    .filterNot(_.isKindOrAPIVersion)
    .map(builderMethod(className, _))
    .mkString("\n")

  private val resource: CodeGeneratorFor[Resource] = { t =>
    import t._
    s"""${t.header("dev.hnaderi.k8s._", "dev.hnaderi.k8s.utils._")}
final case class $name(
  ${printProps(properties.filterNot(_.isKindOrAPIVersion))}
) extends KObject {
  protected val _resourceKind = ResourceKind("${kind.group}", "${kind.kind}", "${kind.version}")

${builderMethods(name, properties)}

  override def foldTo[T : Builder] : T = $name.encoder[T].apply(this)
}

object $name {
${foldToTree(t)}
}
"""
  }

  private val metaResource: CodeGeneratorFor[MetaResource] = { t =>
    import t._

    val supportedKinds = kinds
      .map(k =>
        s"""    ResourceKind("${k.group}", "${k.kind}", "${k.version}")"""
      )
      .mkString(",\n")

    s"""${t.header("dev.hnaderi.k8s._", "dev.hnaderi.k8s.utils._")}

final case class $name(
  kind: String,
  apiVersion: String,
  ${printProps(properties)}
)

object $name {
  val knownKinds : Seq[ResourceKind] = Seq(
$supportedKinds
  )

${foldToTree(t)}
}
"""
  }

  private val subResource: CodeGeneratorFor[SubResource] = { t =>
    import t._
    s"""${t.header("dev.hnaderi.k8s.utils._")}
final case class $name(
  ${printProps(properties)}
) {
${builderMethods(name, properties)}
}

object $name {
${foldToTree(t)}
}
"""
  }

  private val primitive: CodeGeneratorFor[Primitive] = { t =>
    import t._
    s"""${t.header("dev.hnaderi.k8s.utils._")}
trait $name
object $name {
  implicit def encoder[T](implicit builder : Builder[T]) : Encoder[$name, T] = ???
}
"""
  }

  val print: CodeGenerator = {
    case o: Resource     => resource(o)
    case o: SubResource  => subResource(o)
    case o: MetaResource => metaResource(o)
    case o: Primitive    => primitive(o)
  }

  def write(scg: SourceCodeGenerator)(data: DataModel) = data match {
    case p: Primitive =>
      scg.unmanaged(pkg = p.pkg, name = p.name).write(print(p))
    case other =>
      scg.managed(pkg = other.pkg, name = other.name).write(print(other))
  }

  private def foldToTree(model: DataModel): String = {
    val tpe = s"${model.pkg.replace('-', '_')}.${model.name}"

    val (ps, hasAdditionalEnc, hasAdditionalDec) = model match {
      case r: Resource      => (r.properties, true, false)
      case sr: SubResource  => (sr.properties, false, false)
      case mr: MetaResource => (mr.properties, false, true)
      case _                => return ""
    }

    def encoderFieldFor(p: ModelProperty) =
      s""""${p.name}"""" -> s"o.${p.fieldName}"

    val additionalFields =
      if (hasAdditionalEnc)
        Seq(
          "\"kind\"" -> "o.kind",
          "\"apiVersion\"" -> "o.apiVersion"
        )
      else Nil

    val encoderFields =
      (ps.map(encoderFieldFor) ++ additionalFields)
        .map { case (field, value) => s"            .write($field, $value)" }
        .mkString("\n")

    s"""
    implicit def encoder[T](implicit builder : Builder[T]) : Encoder[$tpe, T] = new Encoder[$tpe, T] {
        def apply(o: $tpe) : T = {
          val obj = ObjectWriter[T]()
          obj
$encoderFields
            .build
        }
    }"""
  }
}
