import com.aianonymous.sbt.standard.libraries.StandardLibraries._

name := """aianash-demo"""

scalaVersion := Version.scala

libraryDependencies ++= Seq(
) ++ Libs.akka ++ Libs.microservice

routesGenerator := InjectedRoutesGenerator