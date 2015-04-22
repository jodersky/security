import sbt._
import Keys._

object ApplicationBuild extends Build {
  
  lazy val commonSettings = Seq(
    scalaVersion := "2.11.6",
    scalacOptions ++= Seq("-deprecation", "-feature")
  )

  lazy val skeybase = (
    Project("skeybase", file("."))
    settings(commonSettings: _*)
    settings(
      resolvers += "spray repo" at "http://repo.spray.io",
      libraryDependencies ++= Seq(
      	"io.spray" %% "spray-can" % "1.3.3",
      	"io.spray" %% "spray-httpx" % "1.3.3",
      	"io.spray" %% "spray-client" % "1.3.3",
      	"io.spray" %% "spray-json" % "1.3.1",
      	"com.typesafe.akka"  %% "akka-actor" % "2.3.9",
      	"net.sourceforge.htmlcleaner" % "htmlcleaner" % "2.10",
      	"org.apache.commons" % "commons-lang3" % "3.4"
      )
    )
  )

}
