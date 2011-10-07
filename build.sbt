name := "GenomeMuseum"

version := "0.8a1"

scalaVersion := "2.9.1"

organization := "ScienceDesign"

seq(ProguardPlugin.proguardSettings :_*)

libraryDependencies ++= Seq(
  "org.scala-lang" % "scala-swing" % "2.9.1",
  "com.explodingpixels" % "mac_widgets" % "0.9.6-SNAPSHOT",
  "org.jdesktop.bsaf" % "bsaf" % "1.9.2",
  "net.java.dev.glazedlists" % "glazedlists_java15" % "1.8.0",
  "com.h2database" % "h2" % "1.3.160",
  "org.squeryl" %% "squeryl" % "0.9.4",
  "org.specs2" %% "specs2" % "1.6.1" % "test",
  "org.mockito" % "mockito-all" % "1.8.5" % "test",
  "junit" % "junit" % "4.9" % "test",
  "org.pegdown" % "pegdown" % "1.0.2" % "test"
)

resolvers += "releases" at "http://scala-tools.org/repo-releases"

resolvers += "java.net" at "http://download.java.net/maven/2/"

proguardOptions ++= Seq(
  keepMain("jp.scid.genomemuseum.GenomeMuseum"),
  "-dontnote",
  "-keepclassmembers class ** {@org.jdesktop.application.*Action *;}",
  "-keepclassmembers class * extends org.jdesktop.application.AbstractBean { public *;}",
  "-keep class * implements java.sql.Driver",
  "-keep class net.sf.cglib.** {*;}"
)

maxErrors := 20

parallelExecution := true

testOptions in Test += Tests.Argument("console", "junitxml")

scalacOptions += "-unchecked"

mainClass in (Compile, run) := Some("jp.scid.genomemuseum.GenomeMuseum")
