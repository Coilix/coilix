package dev.coilix.annotation;

import java.lang.annotation.*;

/**
 * Marks a method of a bean that receives application events published through the event publisher
 * of the events module.
 *
 * <p>The method takes one parameter, the type of the events it receives, returns {@code
 * cats.effect.IO[Unit]} and may be private. It receives every published event of that type and of
 * its subtypes, so a parameter of a sealed trait receives each of its cases. Type arguments are
 * part of the match.
 *
 * <p>By default its effect runs in the fiber of the publisher before the publication ends, and its
 * error reaches the publisher. With {@link #async()} it runs on a fiber of its own, and its error
 * is logged, optionally after retries configured under {@code
 * coilix.events.listeners.<bean>.<method>.retry}. A listener that waits, for example for a mail
 * server, wraps the call in {@code IO.blocking}.
 *
 * <pre>
 * {@literal @}Component
 * final class ConfirmationMailer(mail: MailSender) {
 *   {@literal @}EventListener(async = true)
 *   def onOrderPlaced(event: OrderPlaced): IO[Unit] =
 *     IO.blocking(mail.sendConfirmation(event.customer, event.orderId))
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@Documented
public @interface EventListener {

    /**
     * Whether the method runs on a fiber of its own instead of the fiber of the publisher.
     *
     * @return {@code true} for an asynchronous listener, {@code false} by default
     */
    boolean async() default false;
}
