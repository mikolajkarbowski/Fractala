ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "3.3.7"
ThisBuild / organization := "com.fractala"

Global / cancelable := true

val tapirVersion = "1.9.9"
val http4sVersion = "0.23.25"
val circeVersion = "0.14.6"

lazy val core = (project in file("core"))
  .settings(
    name := "fractala-core",
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-core" % "2.10.0",
      "org.scalatest" %% "scalatest" % "3.2.19" % Test,
      "org.scalanlp" %% "breeze" % "2.1.0"
    )
  )

lazy val api = (project in file("api"))
  .dependsOn(core)
  .settings(
    name := "fractala-api",
    Compile / run / fork := true,
    libraryDependencies ++= Seq(
      // Tapir & Http4s
      "com.softwaremill.sttp.tapir" %% "tapir-http4s-server" % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-bundle" % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-json-circe" % tapirVersion,
      "org.http4s" %% "http4s-ember-server" % http4sVersion,

      // JSON (Circe)
      "io.circe" %% "circe-generic" % circeVersion,
      "io.circe" %% "circe-parser" % circeVersion,

      // Cats Effect
      "org.typelevel" %% "cats-effect" % "3.5.3",

      // Logowanie
      "ch.qos.logback" % "logback-classic" % "1.4.14",
      "com.typesafe.scala-logging" %% "scala-logging" % "3.9.5",
      "com.github.pureconfig" %% "pureconfig-core" % "0.17.6"
    )
  )

lazy val root = (project in file("."))
  .aggregate(
    core,
    api
  )
  .settings(
    name := "fractala-root"
  )
