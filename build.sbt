import org.scalajs.linker.interface.ModuleKind

ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "3.3.7"
ThisBuild / organization := "com.fractala"

val tapirVersion = "1.9.9"
val http4sVersion = "0.23.25"
val circeVersion = "0.14.6"

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
  .enablePlugins(JavaAppPackaging)
  .settings(
    name := "fractala-api",
    Compile / run / fork := true,
    scriptClasspath := Seq("*"),
    libraryDependencies ++= Seq(
      // Tapir & Http4s
      "com.softwaremill.sttp.tapir" %% "tapir-http4s-server" % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-bundle" % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-json-circe" % tapirVersion,
      "org.http4s" %% "http4s-ember-server" % http4sVersion,
      "org.http4s" %% "http4s-circe" % "0.23.26",

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

lazy val frontend = (project in file("frontend"))
  .enablePlugins(ScalaJSPlugin)
  .settings(
    name := "fractala-frontend",
    scalaJSUseMainModuleInitializer := true,
    // Emit ES modules so the output can be consumed by Vite (and modern browsers).
    scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.ESModule) },
    libraryDependencies ++= Seq(
      "org.scala-js" %%% "scalajs-dom" % "2.8.0",
      "com.lihaoyi" %%% "scalatags" % "0.12.0",
      "io.circe" %%% "circe-core" % "0.14.6",
      "io.circe" %%% "circe-generic" % "0.14.6",
      "io.circe" %%% "circe-parser" % "0.14.6"
    )
  )

lazy val root = (project in file("."))
  .aggregate(
    core,
    api,
    frontend
  )
  .settings(
    name := "fractala-root"
  )
