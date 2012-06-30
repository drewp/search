name := "search"

version := "1.7"

scalaVersion := "2.9.2"

mainClass := Some("JettyLauncher")

seq(webSettings :_*)

libraryDependencies ++= Seq(
  "org.scalatra" % "scalatra_2.9.1" % "2.0.4",
  "org.scalatra" % "scalatra-specs_2.9.1" % "2.0.4" % "test",
  "org.mortbay.jetty" % "jetty" % "6.1.22" % "container",
  "org.mortbay.jetty" % "jetty-servlet-tester" % "6.1.22" % "provided-> default",
  "javax.servlet" % "servlet-api" % "2.5" % "provided->default",
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
