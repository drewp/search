import sbt._
class build(info: ProjectInfo) extends DefaultWebProject(info) {
  // scalatra
  val sonatypeNexusSnapshots = "Sonatype Nexus Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
  val sonatypeNexusReleases = "Sonatype Nexus Releases" at "https://oss.sonatype.org/content/repositories/releases"
  val scalatra = "org.scalatra" %% "scalatra" % "2.0.0.M3"
  
  // jetty
  val jetty6 = "org.mortbay.jetty" % "jetty" % "6.1.22" % "test"
  val servletApi = "org.mortbay.jetty" % "servlet-api" % "2.5-20081211" % "provided"
  val dispatch = "net.databinder" %% "dispatch-http" % "0.7.8"
  val slf4j = "org.slf4j" % "slf4j-api" % "1.6.1"
  val slf4jSimple = "org.slf4j" % "slf4j-simple" % "1.6.1"

  val liftjson = "net.liftweb" %% "lift-json" % "2.2"

  val casbah = "com.mongodb.casbah" %% "casbah" % "2.0.1"

  override val jettyPort = 9999
}
