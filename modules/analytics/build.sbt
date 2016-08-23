import com.aianonymous.sbt.standard.libraries.StandardLibraries._

name := """aianash-analytics"""

scalaVersion := Version.scala

libraryDependencies ++= Seq(
) ++ Libs.akka ++ Libs.microservice ++ Libs.commonsEvents

routesGenerator := InjectedRoutesGenerator