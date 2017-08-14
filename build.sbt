//import AssemblyKeys._
import org.scalatra.sbt._
import org.scalatra.sbt.PluginKeys._
import ScalateKeys._

ScalatraPlugin.scalatraSettings

scalateSettings

name := "search"

version := "1.8"

scalaVersion := "2.11.4"

mainClass := Some("JettyLauncher")

//seq(webSettings :_*)
//seq(assemblySettings: _*)

resolvers += Classpaths.typesafeReleases
resolvers ++= Seq("typesafe" at "http://repo.typesafe.com/typesafe/maven-releases/")
libraryDependencies ++= Seq(
  "org.scalatra" % "scalatra" % "2.2.+",
  "org.scalatra" % "scalatra-specs2" % "2.2.+" % "test",
  "ch.qos.logback" % "logback-classic" % "1.2.3" % "runtime",
  "org.eclipse.jetty" % "jetty-webapp" % "9.2.19.v20160908" % "container",
  "org.slf4j" % "slf4j-api" % "1.6.1",
  "org.slf4j" % "slf4j-simple" % "1.6.1",
  "net.liftweb" % "lift-json_2.9.1" % "2.4"
)
libraryDependencies += "net.databinder.dispatch" %% "dispatch-core" % "0.13.1"
// https://mvnrepository.com/artifact/org.eclipse.jetty/jetty-server
libraryDependencies += "org.eclipse.jetty" % "jetty-server" % "9.4.6.v20170531"
libraryDependencies += "javax.servlet" % "javax.servlet-api" % "3.1.0"

scalateTemplateConfig in Compile := {
  val base = (sourceDirectory in Compile).value
  Seq(
    TemplateConfig(
      base / "webapp" / "WEB-INF" / "templates",
      Seq.empty,  /* default imports should be added here */
      Seq(
        Binding("context", "_root_.org.scalatra.scalate.ScalatraRenderContext", importMembers = true, isImplicit = true)
      ),  /* add extra bindings here */
      Some("templates")
    )
  )
}
//port in container.Configuration := 9999


enablePlugins(JettyPlugin)
scalacOptions in Test ++= Seq("-Yrangepos", "-Ylog-classpath")
