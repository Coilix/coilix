package dev.coilix.testkit

import dev.coilix.boot.ApplicationArguments
import dev.coilix.common.TypeInfo
import dev.coilix.config.Environment
import dev.coilix.context.ApplicationContext
import dev.coilix.context.SynchronousLifecycle
import dev.coilix.events.EventBus
import scala.concurrent.duration.DurationInt
import scala.concurrent.duration.FiniteDuration
import scala.reflect.ClassTag

/**
 * Short helpers that read from the application context given to the current block, so a test asks for a bean by its type
 * alone.
 *
 * ```scala
 * setup.use {
 *   bean[WelcomeMail].sendTo("ann@example.com")
 *   assertEquals(property[String]("newsletter.sender-name"), Some("The Team"))
 * }
 * ```
 *
 * Mix it into a suite, or import the members of the companion object: `import dev.coilix.testkit.CoilixContextOps.*`.
 */
trait CoilixContextOps {

  /** The context given to the current block. */
  def context(using current: ApplicationContext): ApplicationContext = current

  /**
   * The single bean of type `T`, or the primary one when there are several.
   *
   * @throws dev.coilix.context.NoSuchBeanException
   *   if there is no bean of type `T`
   * @throws dev.coilix.context.AmbiguousBeanException
   *   if there are several beans of type `T` and none is primary
   */
  def bean[T: TypeInfo](using current: ApplicationContext): T = current.getBean[T]

  /** The bean of type `T`, or `None` when the context has none. */
  def beanOption[T: TypeInfo](using current: ApplicationContext): Option[T] = current.getBeanOption[T]

  /** Every bean of type `T`, in registration order. */
  def beans[T: TypeInfo](using current: ApplicationContext): List[T] = current.getBeansOfType[T]

  /** The configuration value at `path` converted to `T`, or `None` when the key is not set. */
  def property[T: ClassTag](path: String)(using current: ApplicationContext): Option[T] =
    current.getBean[Environment].get[T](path)

  /** The command-line arguments of the context. */
  def arguments(using current: ApplicationContext): ApplicationArguments = current.getBean[ApplicationArguments]

  /**
   * The application events of class `E` published in the context so far, oldest first, as recorded by the
   * [[dev.coilix.testkit.EventRecorder]] of a context built by the testkit.
   *
   * ```scala
   * setup.use {
   *   bean[Checkout].place(order)
   *   assertEquals(recordedEvents[OrderPlaced].map(_.orderId), List(order.id))
   * }
   * ```
   */
  def recordedEvents[E: TypeInfo](using current: ApplicationContext): List[E] = current.getBean[EventRecorder].eventsOf[E]

  /**
   * Waits until no asynchronous event listener runs or waits for a retry, like [[dev.coilix.events.EventBus.awaitIdle]], blocking
   * the calling thread. Tests in `IO` use `EventBus.awaitIdle` instead.
   *
   * @return
   *   `true` when the listeners finished, `false` when `timeout` passed first
   */
  def awaitEvents(timeout: FiniteDuration = 5.seconds)(using current: ApplicationContext): Boolean =
    SynchronousLifecycle.run(current, current.getBean[EventBus].awaitIdle(timeout))
}

/** The helpers of [[dev.coilix.testkit.CoilixContextOps]] for import, outside a suite that mixes the trait in. */
object CoilixContextOps extends CoilixContextOps
