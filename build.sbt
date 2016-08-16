import com.typesafe.sbt.packager.archetypes.JavaAppPackaging

import com.typesafe.sbt.packager.docker.{Cmd, ExecCmd, CmdLike}

import com.aianonymous.sbt.standard.libraries.StandardLibraries._

name := """aianash"""

version := "0.1.0"

scalaVersion := Version.scala

lazy val analytics = (project in file("modules/analytics")).enablePlugins(PlayScala)
lazy val demo = (project in file("modules/demo")).enablePlugins(PlayScala)

lazy val main = (project in file(".")).enablePlugins(PlayScala)
                                      .dependsOn(analytics, demo)
                                      .aggregate(analytics, demo)

scalacOptions ++= Seq("-feature",  "-language:postfixOps", "-language:reflectiveCalls")

routesGenerator := InjectedRoutesGenerator