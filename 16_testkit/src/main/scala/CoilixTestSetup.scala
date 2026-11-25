package dev.coilix.testkit

import dev.coilix.boot.BasePackageResolver
import dev.coilix.common.TypeInfo
import dev.coilix.config.Profiles
import dev.coilix.context.ConfigurableApplicationContext
import scala.reflect.ClassTag
import scala.util.Using

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
 * `use` runs a block inside the context, and [[dev.coilix.testkit.CoilixSuite]] registers munit tests that do the same.
 *
 * A test context has no banner, and no startup log. The runners do not run unless
 * [[dev.coilix.testkit.CoilixTestSetup.withRunners]] is called, and asynchronous event listeners run on the task executor unless
 * [[dev.coilix.testkit.CoilixTestSetup.withSynchronousEvents]] is called.
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
  def replaceBean[T <: AnyRef: TypeInfo](instance: T): CoilixTestSetup =
    withBean(TestBean(defaultName[T], TypeInfo[T].genericType, instance, replaces = true))

  /** Adds `instance` as a bean of type `T` next to the beans of the application. */
  def addBean[T <: AnyRef: TypeInfo](instance: T): CoilixTestSetup =
    withBean(TestBean(defaultName[T], TypeInfo[T].genericType, instance, replaces = false))

  /** Adds a ready bean with a chosen name and mode. */
  def withBean(bean: TestBean): CoilixTestSetup =
    copy(beans = beans :+ bean)

  /** Sets the command-line arguments, for example `--mechanic=M-002`. Options also become configuration properties. */
  def withArguments(values: String*): CoilixTestSetup =
    copy(arguments = values.toList)

  /**
   * Runs asynchronous event listeners in the thread of the publisher, so a test checks their effects right after `publish`
   * without waiting. Their exceptions are still only logged, and their retries follow at once. It sets
   * `coilix.events.synchronous = true`.
   */
  def withSynchronousEvents: CoilixTestSetup =
    withProperties("coilix.events.synchronous" -> "true")

  /** Calls the [[dev.coilix.boot.ApplicationRunner]] beans after the context is built, as a `CoilixIOApp` does. */
  def withRunners: CoilixTestSetup =
    copy(runners = true)

  /**
   * Builds the context, runs `body` with the context given to it and closes the context afterwards, also when `body` fails.
   *
   * ```scala
   * setup.withProfiles("loud").use {
   *   assertEquals(bean[Notifier].channel, "loud")
   * }
   * ```
   *
   * @return
   *   the result of `body`
   */
  def use[A](body: ConfigurableApplicationContext ?=> A)(using factory: TestContextFactory = TestContextFactory.default): A =
    Using.resource(factory.create(this))(context => body(using context))

  /** The properties to load the configuration with, including the active profiles. */
  def configurationOverrides: Map[String, String] =
    if (profiles.isEmpty) properties else properties + (Profiles.ActivePath -> profiles.mkString(","))

  private def defaultName[T: TypeInfo]: String = {
    val simpleName = TypeInfo[T].runtimeClass.getSimpleName.stripSuffix("$")
    simpleName.take(1).toLowerCase + simpleName.drop(1)
  }
}

/** Starting points for a [[dev.coilix.testkit.CoilixTestSetup]]. */
object CoilixTestSetup {

  /** Scans the packages of application `T`, annotated with `@CoilixApplication`, as a `CoilixIOApp` does. */
  def forApplication[T: ClassTag]: CoilixTestSetup =
    empty(_.resolve[T])

  /** Scans the given packages and their subpackages. */
  def forPackages(first: String, more: String*): CoilixTestSetup =
    empty(_ => first :: more.toList)

  private def empty(packages: BasePackageResolver => List[String]): CoilixTestSetup =
    CoilixTestSetup(packages, Map.empty, Nil, Nil, Nil, runners = false)
}
