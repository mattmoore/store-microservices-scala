package io.mattmoore.scala.distributedtracing

class MainSuite extends munit.FunSuite {
  test("Scala 3 compiles") {
    assertEquals("I was compiled by Scala 3. :)", Main.msg)
  }
}