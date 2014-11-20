import sbt._
import Keys._

object BuildSettings {
  val buildSettings = Defaults.defaultSettings ++ Seq(
    organization := "org.scalamacros",
    version := "1.0.0",
    scalaVersion := "2.11.4",
    crossScalaVersions := Seq("2.10.2", "2.10.3", "2.10.4", "2.11.0", "2.11.1", "2.11.2", "2.11.3", "2.11.4"),
    resolvers += Resolver.sonatypeRepo("snapshots"),
    resolvers += Resolver.sonatypeRepo("releases"),
    scalacOptions ++= Seq(
      "-deprecation",
      "-encoding", "UTF-8",
      "-feature",
      "-language:implicitConversions", "-language:higherKinds", "-language:postfixOps",
      "-optimise",
      "-unchecked",
      "-Yno-generic-signatures",
      "-Yno-adapted-args",
      "-Yinline", "-Yinline-warnings",
      "-Ywarn-value-discard"
    )
  )
}

object MyBuild extends Build {
  import BuildSettings._

  lazy val root: Project = Project(
    "root",
    file("."),
    settings = buildSettings ++ Seq(
      run <<= run in Compile in macros)
  ) aggregate(macros, core, example)

  lazy val core: Project = Project(
    "core",
    file("core"),
    settings = buildSettings
  )

  val macroVersion = "2.0.1"
  val paradisePlugin = compilerPlugin("org.scalamacros" % "paradise"        % macroVersion cross CrossVersion.full)

  lazy val macros: Project = Project(
    "macros",
    file("macros"),
    settings = buildSettings ++ Seq(
      libraryDependencies ++= Seq(
        "org.scala-lang"  %  "scala-reflect"  % scalaVersion.value,
        "org.scala-lang"  %  "scala-compiler" % scalaVersion.value % "provided"
      ),
      addCompilerPlugin(paradisePlugin),
      libraryDependencies ++= CrossVersion partialVersion scalaVersion.value collect {
        case (2, scalaMajor) if scalaMajor < 11 =>
          // if scala 2.11+ is used, quasiquotes are merged into scala-reflect
          Seq("org.scalamacros" %% "quasiquotes" % macroVersion)
      } getOrElse Nil
    )
  ) dependsOn(core)

  lazy val example: Project = Project(
    "example",
    file("example"),
    settings = buildSettings ++ Seq(
      publishArtifact := false,
      addCompilerPlugin(paradisePlugin) // Unfortunately necessary :( see: http://stackoverflow.com/q/23485426/463761
    )
  ) dependsOn(core, macros)

}
