import com.typesafe.sbt.packager.archetypes.JavaAppPackaging
import com.typesafe.sbt.packager.docker.Cmd

import com.aianonymous.sbt.standard.libraries.StandardLibraries._

name := """aianash"""

version := "0.1.0"

scalaVersion := Version.scala

lazy val analytics = (project in file("modules/analytics")).enablePlugins(PlayScala, JavaAppPackaging, DockerPlugin)
lazy val customer = (project in file("modules/customer")).enablePlugins(PlayScala, JavaAppPackaging, DockerPlugin)
lazy val demo = (project in file("modules/demo")).enablePlugins(PlayScala, JavaAppPackaging, DockerPlugin)

lazy val root = (project in file("."))
  .enablePlugins(PlayScala, JavaAppPackaging, DockerPlugin)
  .settings(
    dockerExposedPorts := Seq(9000),
    dockerRepository := Some("aianonymous"),
    dockerBaseImage := "aianonymous/baseimage",
    dockerEntrypoint := Seq("sh", "-c",
                            """export NASH_HOST=`ifdata -pa eth0` && \
                            |  export NASH_PORT=7474 && \
                            |  bin/aianash -Dakka.cluster.seed-nodes.0=akka.tcp://aianonymous@172.17.0.3:4848 $*""".stripMargin
                            ),
    dockerCommands ++= Seq(
      Cmd("USER", "root")
    ),
    aggregate in Docker := false

  )
  .dependsOn(analytics, customer, demo)
  .aggregate(analytics, customer, demo)

libraryDependencies += filters

scalacOptions ++= Seq("-feature",  "-language:postfixOps", "-language:reflectiveCalls")

routesGenerator := InjectedRoutesGenerator