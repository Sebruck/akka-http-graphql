import Dependencies._

ThisBuild / scalaVersion     := "2.12.10"
ThisBuild / version          := "0.1.1-SNAPSHOT"
ThisBuild / organization     := "com.github.sebruck"
ThisBuild / organizationName := "sebruck"
ThisBuild / publishTo := Some(
  if (isSnapshot.value)
    Opts.resolver.sonatypeSnapshots
  else
    Opts.resolver.sonatypeStaging
)

lazy val root = (project in file("."))
  .settings(
    name := "akka-http-graphql",
    libraryDependencies ++= dependencies
  )

