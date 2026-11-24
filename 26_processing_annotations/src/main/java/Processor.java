package dev.coilix.processing.annotation;

import java.lang.annotation.*;

/**
 * Marks a class as a background processor that the processor manager of {@code coilix-processing}
 * creates, splits into shards and supervises.
 *
 * <p>A processor is not a bean: no bean can inject it, and it is controlled only through the {@code
 * ProcessorManager} bean. Its constructor receives beans and {@code @Value} parameters like a bean,
 * and its {@code @PostConstruct} and {@code @PreDestroy} methods run when the manager creates and
 * closes it. The identifier of the processor and its number of shards are not set here: the
 * identifier is a value in the code of the processor and the number of shards is in the
 * configuration.
 *
 * <pre>
 * {@literal @}Processor
 * final class InvoiceExport(invoices: InvoiceRepository) extends BlockingProcessor {
 *   override val id: ShopProcessor = ShopProcessor.InvoiceExport
 *
 *   override def run(shard: Shard): Unit = invoices.exportSlots(shard.slots)
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Documented
public @interface Processor {}
