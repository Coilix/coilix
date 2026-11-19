package dev.coilix.testkit

import dev.coilix.boot.BasePackageResolver
import dev.coilix.common.ClassTags.runtimeTypeOf
import dev.coilix.config.Profiles
import scala.reflect.ClassTag

/**
 * What a test context contains: the scanned packages, configuration properties, active profiles, ready beans, command-line
 * arguments and whether the [[dev.coilix.boot.ApplicationRunner]] beans run.
 *
 * Each `with*`, `replaceBean` and `addBean` call returns a new setup, so a suite can share a base setup and change it per test.
 *
 * ```scala
 * val setup = CoilixTestSetup
 *   .forApplication[ShopApplication.type]
 *   .withProperties("shop.currency" -> "EUR")
 *   .withProfiles("test")
 *   .replaceBean[Clock](Clock.fixed(Instant.parse("2026-11-19T10:00:00Z"), ZoneOffset.UTC))
 * ```
 *
 * A test context has no banner, no startup log and no shutdown hook. The runners do not run unless
 * [[dev.coilix.testkit.CoilixTestSetup.withRunners]] is called.
 *
 * @param packages
 *   decides which packages to scan, given the configured [[dev.coilix.boot.BasePackageResolver]]
 * @param properties
 *   configuration properties that win over command-line options, system properties and configuration files
 * @param profiles
 *   the active profiles; when not empty, they replace `coilix.profiles.active` from every other source
 * @param beans
 *   ready instances put into the context
 * @param arguments
 *   command-line arguments, available as the `applicationArguments` bean
 * @param runners
 *   whether the runners are called after the context is built
 */
final case class CoilixTestSetup(
  packages: BasePackageResolver => List[String],
  properties: Map[String, String],
  profiles:   List[String],
  beans:      List[TestBean],
  arguments:  List[String],
  runners:    Boolean
) {

  /** Adds configuration properties; a later value for the same key wins. */
  def withProperties(entries: (String, String)*): CoilixTestSetup =
    copy(properties = properties ++ entries)

  /** Makes `names` the active profiles, in this order. */
  def withProfiles(names: String*): CoilixTestSetup =
    copy(profiles = names.toList)

  /**
   * Replaces every bean of type `T` found by scanning, by `@Bean` methods or in modules with `instance`, which is injected
   * wherever a `T` is needed.
   *
   * Building the context fails with [[dev.coilix.boot.UnmatchedBeanReplacementException]] when the application has no bean of
   * type `T`; use [[dev.coilix.testkit.CoilixTestSetup.addBean]] to add a bean the application does not define.
   */
  def replaceBean[T <: AnyRef: ClassTag](instance: T): CoilixTestSetup =
    withBean(TestBean(defaultName[T], runtimeTypeOf[T], instance, replaces = true))

  /** Adds `instance` as a bean of type `T` next to the beans of the application. */
  def addBean[T <: AnyRef: ClassTag](instance: T): CoilixTestSetup =
    withBean(TestBean(defaultName[T], runtimeTypeOf[T], instance, replaces = false))

  /** Adds a ready bean with a chosen name and mode. */
  def withBean(bean: TestBean): CoilixTestSetup =
    copy(beans = beans :+ bean)

  /** Sets the command-line arguments, for example `--mechanic=M-002`. Options also become configuration properties. */
  def withArguments(values: String*): CoilixTestSetup =
    copy(arguments = values.toList)

  /** Calls the [[dev.coilix.boot.ApplicationRunner]] beans after the context is built, as `Coilix.run` does. */
  def withRunners: CoilixTestSetup =
    copy(runners = true)

  /** The properties to load the configuration with, including the active profiles. */
  def configurationOverrides: Map[String, String] =
    if (profiles.isEmpty) properties else properties + (Profiles.ActivePath -> profiles.mkString(","))

  private def defaultName[T: ClassTag]: String = {
    val simpleName = runtimeTypeOf[T].getSimpleName.stripSuffix("$")
    simpleName.take(1).toLowerCase + simpleName.drop(1)
  }
}

/** Starting points for a [[dev.coilix.testkit.CoilixTestSetup]]. */
object CoilixTestSetup {

  /** Scans the packages of application `T`, annotated with `@CoilixApplication`, as `Coilix.run` does. */
  def forApplication[T: ClassTag]: CoilixTestSetup =
    empty(_.resolve[T])

  /** Scans the given packages and their subpackages. */
  def forPackages(first: String, more: String*): CoilixTestSetup =
    empty(_ => first :: more.toList)

  private def empty(packages: BasePackageResolver => List[String]): CoilixTestSetup =
    CoilixTestSetup(packages, Map.empty, Nil, Nil, Nil, runners = false)
}
