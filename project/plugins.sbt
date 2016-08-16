// Comment to get more information during initialization
logLevel := Level.Warn

// The Typesafe repository
resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

// Use the Play sbt plugin for Play projects
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.5.4")

addSbtPlugin("com.aianonymous" % "sbt-standard-libraries" % "0.2.0")

addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.0.2")