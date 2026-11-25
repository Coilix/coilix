package dev.coilix.processing
package stateless

import dev.coilix.annotation.Component

/** The orders waiting to be shipped, with a slot for each customer. */
@Component
final class Orders extends InMemoryStream[Order](256, _.customer)
