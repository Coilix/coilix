package dev.coilix.common
package validation

class ValidatorTest extends munit.FunSuite {

  private val noAt     = Violation("email.no-at", "Email must contain @")
  private val tooShort = Violation("email.too-short", "Email is too short")

  private val emails: Validator[String] = Validator[String]
    .test(_.contains("@"))(noAt)
    .test(_.length >= 5)(tooShort)

  test("a valid value is returned unchanged") {
    assertEquals(emails.validate("ann@example.com"), Right("ann@example.com"))
    assertEquals(emails("ann@example.com"), Right("ann@example.com"))
    assert(emails.isValid("ann@example.com"))
  }

  test("every broken rule is reported in the order it was added") {
    assertEquals(emails.validate("ab"), Left(List(noAt, tooShort)))
    assertEquals(emails.violations("abcdef"), List(noAt))
    assert(!emails.isValid("ab"))
  }

  test("a validator without rules accepts every value") {
    assertEquals(Validator[Int].validate(-1), Right(-1))
  }

  test("validOrThrow returns a valid value and throws with every violation otherwise") {
    assertEquals(emails.validOrThrow("ann@example.com"), "ann@example.com")
    val error = intercept[ValidationException](emails.validOrThrow("ab"))
    assertEquals(error.violations, List(noAt, tooShort))
    assertEquals(error.getMessage, "Validation failed: Email must contain @; Email is too short")
  }

  test("testNot reports the violation when the condition holds") {
    val spaces    = Violation("name.spaces", "Name must not contain spaces")
    val validator = Validator[String].testNot(_.contains(" "))(spaces)
    assertEquals(validator.violations("ann lee"), List(spaces))
    assertEquals(validator.violations("ann"), Nil)
  }

  test("testAll needs every condition and testAny at least one") {
    val outside = Violation("range", "Outside range")
    val all     = Validator[Int].testAll(_ >= 0, _ <= 10)(outside)
    val any     = Validator[Int].testAny(_ < 0, _ > 10)(outside)
    assertEquals(List(-1, 5, 11).map(all.isValid), List(false, true, false))
    assertEquals(List(-1, 5, 11).map(any.isValid), List(true, false, true))
  }

  test("combine and and add the rules of other validators") {
    val digit  = Violation("password.digit", "Password needs a digit")
    val digits = Validator[String].test(_.exists(_.isDigit))(digit)
    assertEquals(emails.and(digits).violations("ab"), List(noAt, tooShort, digit))
    assertEquals(emails.combine(digits, digits).violations("ab@cd"), List(digit, digit))
  }

  test("subValidator checks a part of the value") {
    final case class User(email: String)
    val users = Validator[User].subValidator(_.email, emails)
    assertEquals(users.violations(User("ab")), List(noAt, tooShort))
  }

  test("field prefixes the path of the field violations") {
    final case class Address(city: String)
    final case class User(address: Address)
    val blank     = Violation("city.blank", "City must not be blank")
    val addresses = Validator[Address].field("city", _.city, Validator[String].test(_.nonEmpty)(blank))
    val users     = Validator[User].field("address", _.address, addresses)
    assertEquals(users.violations(User(Address(""))), List(blank.withPath("address.city")))
  }

  test("onCaseOf checks only values of the given subtype") {
    val weak      = Violation("password.weak", "Password is too short")
    val passwords = Validator[Credentials.Password].test(_.value.length >= 8)(weak)
    val validator = Validator[Credentials].onCaseOf(passwords)
    assertEquals(validator.violations(Credentials.Password("short")), List(weak))
    assertEquals(validator.violations(Credentials.Token("x")), Nil)
  }

  test("optional accepts None and checks a present value") {
    assertEquals(emails.optional.violations(None), Nil)
    assertEquals(emails.optional.violations(Some("ab")), List(noAt, tooShort))
  }

  test("of builds a validator from statements") {
    assertEquals(Validator.of(Statement[Int](_ > 0, tooShort)).violations(0), List(tooShort))
  }

  test("list validators check size and elements") {
    assertEquals(Validator.validateNonEmpty[Int].violations(Nil), List(StandardViolations.emptyList))
    assertEquals(Validator.validateMinSize[Int](2).violations(List(1)).map(_.message), List("List must have at least 2 elements"))
    assertEquals(Validator.validateMaxSize[Int](1).violations(List(1, 2)).map(_.code), List("list.too-long"))
    assertEquals(Validator.validateEach(emails).violations(List("ann@example.com", "ab")), List(StandardViolations.invalidElements))
    assertEquals(Validator.validateEach(emails).violations(List("ann@example.com")), Nil)
  }
}
