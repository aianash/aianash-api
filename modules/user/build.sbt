import com.aianonymous.sbt.standard.libraries.StandardLibraries._

name := """aianash-user"""

scalaVersion := Version.scala

libraryDependencies ++= Seq(
) ++ Libs.akka ++ Libs.maleorang ++ Libs.commonsCore

routesGenerator := InjectedRoutesGenerator