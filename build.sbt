import Dependencies._

ThisBuild / scalaVersion := "2.13.1"
ThisBuild / version := "0.2.0-SNAPSHOT"
ThisBuild / organization := "com.github.sebruck"
ThisBuild / organizationName := "sebruck"
crossScalaVersions in ThisBuild := Seq("2.13.1", "2.12.10")
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
