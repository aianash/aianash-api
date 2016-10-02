import com.typesafe.sbt.packager.archetypes.JavaAppPackaging
import com.typesafe.sbt.packager.docker.Cmd

import com.aianonymous.sbt.standard.libraries.StandardLibraries._

name := """aianash"""

version := "0.1.0"

scalaVersion := Version.scala

lazy val analytics = (project in file("modules/analytics")).enablePlugins(PlayScala)
lazy val customer = (project in file("modules/customer")).enablePlugins(PlayScala)
lazy val user = (project in file("modules/user")).enablePlugins(PlayScala)
lazy val demo = (project in file("modules/demo")).enablePlugins(PlayScala)
lazy val behavior = (project in file("modules/behavior")).enablePlugins(PlayScala)
lazy val trail = (project in file("modules/trail")).enablePlugins(PlayScala)

lazy val root = (project in file("."))
  .enablePlugins(PlayScala, JavaAppPackaging)
  .settings(
    dockerExposedPorts := Seq(9000),
    dockerRepository := Some("aianonymous"),
    dockerBaseImage := "aianonymous/baseimage",
    dockerEntrypoint := Seq("sh", "-c",
                            """export NASH_HOST=`ifdata -pa eth0` && \
                            |  export NASH_PORT=7474 && \
                            |  bin/aianash $*""".stripMargin
                            ),
    dockerCommands ++= Seq(
      Cmd("USER", "root")
    ),
    aggregate in Docker := false
  )
  .dependsOn(analytics, customer, user, demo, behavior, trail)
  .aggregate(analytics, customer, user, demo, behavior, trail)

libraryDependencies += filters

scalacOptions ++= Seq("-feature",  "-language:postfixOps", "-language:reflectiveCalls")

routesGenerator := InjectedRoutesGenerator