package dev.coilix.common
package validation

enum Credentials {
  case Password(value: String)
  case Token(value: String)
}
