package dev.coilix.annotation;

import java.lang.annotation.*;

/**
 * Marks a method of a bean that runs repeatedly on the task scheduler of the concurrency module.
 *
 * <p>Set exactly one of {@link #fixedRate()} and {@link #fixedDelay()}. Durations are written like
 * configuration values, such as {@code "10s"} or {@code "5 minutes"}, or as a placeholder such as
 * {@code "${shop.refresh-interval:1m}"}. The method takes no parameters, returns {@code Unit} and
 * may be private. The runs start when every bean of the context exists and stop when the context
 * starts closing, before any bean is destroyed. A failed run is logged and the next runs still
 * happen.
 *
 * <pre>
 * {@literal @}Component
 * final class PriceRefresher(prices: PriceCache) {
 *   {@literal @}Scheduled(fixedDelay = "${shop.refresh-interval:1m}", initialDelay = "10s")
 *   def refresh(): Unit = prices.reload()
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@Documented
public @interface Scheduled {

    /**
     * The time between the starts of two runs. A run that takes longer delays the next one.
     *
     * @return the period, or an empty text when {@link #fixedDelay()} is used
     */
    String fixedRate() default "";

    /**
     * The pause between the end of one run and the start of the next.
     *
     * @return the pause, or an empty text when {@link #fixedRate()} is used
     */
    String fixedDelay() default "";

    /**
     * The time before the first run.
     *
     * @return the delay, by default {@code "0s"}
     */
    String initialDelay() default "0s";
}
