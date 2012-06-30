import AssemblyKeys._

name := "search"

version := "1.7"

scalaVersion := "2.9.2"

mainClass := Some("JettyLauncher")

seq(webSettings :_*)

seq(assemblySettings: _*)

libraryDependencies ++= Seq(
  "org.scalatra" % "scalatra_2.9.1" % "2.0.4",
  "org.scalatra" % "scalatra-specs_2.9.1" % "2.0.4" % "test",
  "org.mortbay.jetty" % "servlet-api" % "3.0.20100224" % "provided",
  "org.eclipse.jetty" % "jetty-server" % "8.0.0.M3" % "container, compile",
  "org.eclipse.jetty" % "jetty-util" % "8.0.0.M3" % "container, compile",
  "org.eclipse.jetty" % "jetty-webapp" % "8.0.0.M3" % "container, compile",
  "net.databinder" %% "dispatch-http" % "0.8.8",
  "org.slf4j" % "slf4j-api" % "1.6.1",
  "org.slf4j" % "slf4j-simple" % "1.6.1",
  "net.liftweb" % "lift-json_2.9.1" % "2.4",
  "com.mongodb.casbah" % "casbah_2.9.0-1" % "2.1.5.0"
)

resolvers ++= Seq(
  "Sonatype OSS" at "http://oss.sonatype.org/content/repositories/releases/",
  "Sonatype OSS Snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/",
  "Web plugin repo" at "http://siasia.github.com/maven2"
)

port in container.Configuration := 9999
