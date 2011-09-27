name := "GenomeMuseum"

version := "0.8a1"

scalaVersion := "2.9.1"

organization := "ScienceDesign"

libraryDependencies ++= Seq(
  "com.h2database" % "h2" % "1.3.160",
  "org.scala-tools.testing" %% "specs" % "1.6.8" % "test"
)
