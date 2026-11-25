package dev.coilix.processing
package stateless

import dev.coilix.annotation.Component

/** The numbers of the shipped orders. */
@Component
final class Shipped extends InMemoryOutput[Int]
