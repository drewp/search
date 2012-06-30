name := "search"

version := "1.6"

scalaVersion := "2.9.1"

mainClass := Some("JettyLauncher")

seq(webSettings :_*)

seq(assemblySettings: _*)

libraryDependencies ++= Seq(
  "org.scalatra" %% "scalatra" % "2.0.1",
  "org.scalatra" %% "scalatra-specs" % "2.0.1" % "test",
  "org.mortbay.jetty" % "jetty" % "6.1.22" % "compile, jetty",
  "org.mortbay.jetty" % "jetty-servlet-tester" % "6.1.22" % "provided-> default",
  "javax.servlet" % "servlet-api" % "2.5" % "provided->default",
  "net.databinder" %% "dispatch-http" % "0.7.8",
  "org.slf4j" % "slf4j-api" % "1.6.1",
  "org.slf4j" % "slf4j-simple" % "1.6.1",
  "net.liftweb" %% "lift-json" % "2.4-M4",
  "com.mongodb.casbah" % "casbah_2.9.0-1" % "2.1.5.0"
)

resolvers ++= Seq(
  "Sonatype OSS" at "http://oss.sonatype.org/content/repositories/releases/",
  "Sonatype OSS Snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/",
  "Web plugin repo" at "http://siasia.github.com/maven2"
)

jettyPort := 9999
