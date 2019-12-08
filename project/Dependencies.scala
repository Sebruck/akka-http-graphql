import sbt._

object Dependencies {
  val akkaHttpVersion      = "10.1.11"
  val akkaVersion          = "2.6.1"
  val scalaTestVersion     = "3.1.0"
  val akkaHttpCirceVersion = "1.29.1"
  val circeVersion         = "0.12.3"
  val circeOpticsVersion   = "0.12.0"
  val sangriaVersion       = "2.0.0-M1"
  val sangriaCirceVersion  = "1.3.0"

  lazy val scalaTest = Seq("org.scalatest" %% "scalatest" % scalaTestVersion % Test)

  lazy val akka = Seq(
    "com.typesafe.akka" %% "akka-http"           % akkaHttpVersion,
    "com.typesafe.akka" %% "akka-http-testkit"   % akkaHttpVersion % Test,
    "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % Test
  )

  lazy val akkaHttpCirce = Seq(
    "de.heikoseeberger" %% "akka-http-circe" % akkaHttpCirceVersion
  )

  lazy val circe = Seq(
    "io.circe" %% "circe-core",
    "io.circe" %% "circe-generic",
    "io.circe" %% "circe-parser"
  ).map(_ % circeVersion) ++ Seq("io.circe" %% "circe-optics" % circeOpticsVersion)

  lazy val sangria = Seq(
    "org.sangria-graphql" %% "sangria"         % sangriaVersion,
    "org.sangria-graphql" %% "sangria-circe"   % sangriaCirceVersion,
    "org.sangria-graphql" %% "sangria-slowlog" % sangriaVersion
  )

  lazy val dependencies = scalaTest ++ akka ++ akkaHttpCirce ++ circe ++ sangria
}
