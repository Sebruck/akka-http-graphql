import Dependencies._

ThisBuild / scalaVersion     := "2.12.10"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "com.sebruck"
ThisBuild / organizationName := "sebruck"

lazy val root = (project in file("."))
  .settings(
    name := "akka-http-graphql",
    libraryDependencies ++= dependencies
  )

