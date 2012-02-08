name := "GenomeMuseum"

version := "0.9a1"

scalaVersion := "2.9.1"

organization := "ScienceDesign"

seq(ProguardPlugin.proguardSettings :_*)

libraryDependencies ++= Seq(
  "jp.scid" % "scid-gui" % "0.1.3",
  "jp.scid" % "motifviewer" % "0.1.2",
  "org.scala-lang" % "scala-swing" % "2.9.1",
  "com.explodingpixels" % "mac_widgets" % "0.9.6-SNAPSHOT",
  "org.jdesktop.bsaf" % "bsaf" % "1.9.2",
  "net.java.dev.glazedlists" % "glazedlists_java15" % "1.8.0",
  "com.h2database" % "h2" % "1.3.160",
  "org.squeryl" %% "squeryl" % "0.9.4",
  "org.apache.httpcomponents" % "httpclient" % "4.1.2",
  "com.jgoodies" % "binding" % "2.5.0" from "http://www.sci-d.co.jp/jar-files/jgoodies-common-1.2.1.jar",
  "com.jgoodies" % "common" % "1.2.1" from "http://www.sci-d.co.jp/jar-files/jgoodies-binding-2.5.0.jar",
  "org.slf4j" % "slf4j-api" % "1.6.4",
  "org.slf4j" % "slf4j-jdk14" % "1.6.4",
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
  "-keep class net.sf.cglib.** {*;}",
  "-keep public class org.apache.commons.logging.impl.LogFactoryImpl",
  "-keep public class org.apache.commons.logging.impl.Jdk14Logger { *** <init>(...); }",
  "-keep class jp.scid.** { *;}"
)

maxErrors := 20

pollInterval := 500

parallelExecution := true

testOptions in Test += Tests.Argument("console", "junitxml")

scalacOptions ++= Seq("-unchecked", "-deprecation")

mainClass in (Compile, packageBin) := Some("jp.scid.genomemuseum.GenomeMuseum")

mainClass in (Compile, run) := Some("jp.scid.genomemuseum.GenomeMuseum")

fork := true

javaOptions += "-Xmx1024m -Djava.util.logging.config.file=logging.properties -Dfile.encoding=UTF-8"


