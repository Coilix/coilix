package com.example
package processing

import dev.coilix.annotation.Component
import dev.coilix.processing.InMemoryStream

@Component
final class OrderStream extends InMemoryStream[Order](256, _.customer)
