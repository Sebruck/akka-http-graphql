import sbt._

object Dependencies {
  val akkaHttpVersion       = "10.1.10"
  val akkaVersion           = "2.5.26"
  val scalaTestVersion      = "3.0.8"
  val akkaHttpCirceVersion  = "1.29.1"
  val circeVersion          = "0.12.3"
  val circeOpticsVersion    = "0.12.0"
  val sangriaVersion        = "1.4.2"
  val sangriaCirceVersion   = "1.2.1"
  val sangriaSlowLogVersion = "0.1.8"

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
    "org.sangria-graphql" %% "sangria-slowlog" % sangriaSlowLogVersion
  )

  lazy val dependencies = scalaTest ++ akka ++ akkaHttpCirce ++ circe ++ sangria
}
