import com.aianonymous.sbt.standard.libraries.StandardLibraries._

name := """aianash-behavior"""

scalaVersion := Version.scala

libraryDependencies ++= Seq(
  "com.aianonymous" %% "cassie-core" % "0.1.0"
) ++ Libs.akka ++ Libs.microservice ++ Libs.commonsCore ++ Libs.commonsEvents ++ Libs.commonsCustomer ++ Libs.commonsBehavior

routesGenerator := InjectedRoutesGenerator