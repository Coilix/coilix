package dev.coilix.config

import munit.FunSuite

final class ProfilesTest extends FunSuite {

  test("reads a list of profiles") {
    assertEquals(Profiles.active(ConfigTrees.parse("coilix.profiles.active = [dev, local]")), Right(List("dev", "local")))
  }

  test("reads profiles separated by commas and drops blanks and duplicates") {
    assertEquals(Profiles.active(ConfigTrees.parse("coilix.profiles.active = \" dev,,local,dev \"")), Right(List("dev", "local")))
  }

  test("has no profiles when the key is missing") {
    assertEquals(Profiles.active(ConfigTree.empty), Right(Nil))
  }

  test("returns an error when the profiles are a section") {
    assert(Profiles.active(ConfigTrees.parse("coilix.profiles.active { dev = true }")).isLeft)
  }
}
