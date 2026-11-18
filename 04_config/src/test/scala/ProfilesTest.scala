package dev.coilix.config

import munit.FunSuite

final class ProfilesTest extends FunSuite {

  test("reads a list of profiles") {
    assertEquals(Profiles.active(ConfigTrees.parse("coilix.profiles.active = [dev, local]")), List("dev", "local"))
  }

  test("reads profiles separated by commas and drops blanks and duplicates") {
    assertEquals(Profiles.active(ConfigTrees.parse("coilix.profiles.active = \" dev,,local,dev \"")), List("dev", "local"))
  }

  test("has no profiles when the key is missing") {
    assertEquals(Profiles.active(ConfigTree.empty), Nil)
  }
}
