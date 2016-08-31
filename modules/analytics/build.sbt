import com.aianonymous.sbt.standard.libraries.StandardLibraries._

name := """aianash-analytics"""

scalaVersion := Version.scala

libraryDependencies ++= Seq(
  "com.aianonymous" %% "cassie-core" % "0.1.0"
) ++ Libs.akka ++ Libs.microservice ++ Libs.commonsEvents ++ Libs.commonsCore

routesGenerator := InjectedRoutesGenerator