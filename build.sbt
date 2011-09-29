name := "GenomeMuseum"

version := "0.8a1"

scalaVersion := "2.9.1"

organization := "ScienceDesign"

seq(ProguardPlugin.proguardSettings :_*)

libraryDependencies ++= Seq(
  "org.scala-lang" % "scala-swing" % "2.9.1",
  "net.java.dev.glazedlists" % "glazedlists_java15" % "1.8.0",
  "com.h2database" % "h2" % "1.3.160",
  "org.scala-tools.testing" %% "specs" % "1.6.9" % "test"
)

proguardOptions += keepMain("jp.scid.genomemuseum.GenomeMuseum")

maxErrors := 20

parallelExecution := true
