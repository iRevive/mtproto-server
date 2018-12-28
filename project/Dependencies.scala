import sbt._

object Settings {
  val organization = "com.mtproto"
  val name         = "server"
}

object Versions {
  val scala         = "2.12.7"
  val akka          = "2.5.19"
  val scodecCore    = "1.10.3"
  val scodecBits    = "1.1.7"
  val catsEffect    = "1.1.0"
  val logback       = "1.2.3"
  val scalaLogging  = "3.9.2"
  val scalatest     = "3.0.5"
  val catsScalatest = "2.4.0"
}

object Dependencies {

  val root: Seq[ModuleID] = Seq(
    "com.typesafe.akka"          %% "akka-stream"         % Versions.akka,
    "org.typelevel"              %% "cats-effect"         % Versions.catsEffect,
    "org.scodec"                 %% "scodec-core"         % Versions.scodecCore,
    "org.scodec"                 %% "scodec-bits"         % Versions.scodecBits,
    "com.typesafe.scala-logging" %% "scala-logging"       % Versions.scalaLogging,
    "ch.qos.logback"             % "logback-classic"      % Versions.logback,
    "com.typesafe.akka"          %% "akka-testkit"        % Versions.akka % Test,
    "com.typesafe.akka"          %% "akka-stream-testkit" % Versions.akka % Test,
    "org.scalatest"              %% "scalatest"           % Versions.scalatest % Test,
    "com.ironcorelabs"           %% "cats-scalatest"      % Versions.catsScalatest % Test
  )

}
