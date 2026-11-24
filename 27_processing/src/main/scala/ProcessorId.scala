package dev.coilix.processing

/**
 * The identifier of a background processor, usually a `case object` of a sealed trait of the application.
 *
 * The [[dev.coilix.processing.ProcessorManager]] addresses processors by their identifier, so a misspelt processor does
 * not compile. The name is used in configuration keys, thread names and logs:
 *
 * ```scala
 * sealed trait ShopProcessor extends ProcessorId
 *
 * object ShopProcessor {
 *   case object InvoiceExport extends ShopProcessor
 * }
 *
 * ShopProcessor.InvoiceExport.name // "invoice-export", configured under coilix.processors.invoice-export
 * ```
 */
trait ProcessorId {

  /** The name of the processor, by default the name of the object in kebab case. Override it to keep an older name. */
  def name: String = ProcessorId.kebabCase(getClass.getSimpleName.stripSuffix("$"))
}

/** Naming of processor identifiers. */
object ProcessorId {

  /** Turns a name in camel case, such as `InvoiceExport`, into kebab case, such as `invoice-export`. */
  def kebabCase(name: String): String =
    name
      .replaceAll("([a-z0-9])([A-Z])", "$1-$2")
      .replaceAll("([A-Z])([A-Z][a-z])", "$1-$2")
      .toLowerCase
}
