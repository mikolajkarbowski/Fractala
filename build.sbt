ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "3.3.7"
ThisBuild / organization := "com.fractala"

lazy val core = (project in file("core"))
  .settings(
    name := "fractala-core",
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-core" % "2.10.0",
      "org.scalatest" %% "scalatest" % "3.2.19" % Test,
      "org.scalanlp" %% "breeze" % "2.1.0",
      "com.lihaoyi" %% "fastparse" % "3.1.1"
    )
  )

lazy val api = (project in file("api"))
  .dependsOn(core)
  .settings(
    name := "fractala-api",
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.tapir" %% "tapir-http4s-server" % "1.9.0"
    )
  )

lazy val root = (project in file("."))
  .aggregate(core, api)
  .settings(
    name := "fractala-root"
  )
