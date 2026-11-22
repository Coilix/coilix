package dev.coilix.annotation;

import java.lang.annotation.*;

/**
 * Marks a method of a bean that receives application events published through the event publisher
 * of the events module.
 *
 * <p>The method takes one parameter, the type of the events it receives, returns {@code Unit} and
 * may be private. It receives every published event of that type and of its subtypes, so a
 * parameter of a sealed trait receives each of its cases. Type arguments are part of the match.
 *
 * <p>By default the method runs in the thread of the publisher before the publication returns, and
 * its exception reaches the publisher. With {@link #async()} it runs later on the task executor,
 * and its exception is logged, optionally after retries configured under {@code
 * coilix.events.listeners.<bean>.<method>.retry}. An asynchronous listener that waits, for example
 * for a mail server, also sets {@link #blocking()}, so it runs on the blocking executor instead of
 * the compute pool.
 *
 * <pre>
 * {@literal @}Component
 * final class ConfirmationMailer(mail: MailSender) {
 *   {@literal @}EventListener(async = true)
 *   def onOrderPlaced(event: OrderPlaced): Unit = mail.sendConfirmation(event.customer, event.orderId)
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@Documented
public @interface EventListener {

    /**
     * Whether the method runs on the task executor instead of the thread of the publisher.
     *
     * @return {@code true} for an asynchronous listener, {@code false} by default
     */
    boolean async() default false;

    /**
     * Whether an asynchronous listener waits, for example for I/O, and runs on the blocking
     * executor instead of the task executor. It has no effect on a synchronous listener.
     *
     * @return {@code true} for an asynchronous listener that blocks, {@code false} by default
     */
    boolean blocking() default false;
}
