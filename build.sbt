name          := "MeetupRSVPStreaming"
organization  := "com.gorskimariusz"
description   := "Final project on Postgraduate Studies (Big Data / Hadoop) - Warsaw University of Technology 02/2019"

version := "0.1"

scalaVersion := "2.11.8"

val sparkVersion = "2.2.2"

libraryDependencies ++= Seq(
  "org.apache.spark"       %% "spark-core"                  % sparkVersion,
  "org.apache.spark"       %% "spark-sql"                   % sparkVersion,
  "org.apache.spark"       %% "spark-streaming"             % sparkVersion,
  "org.apache.bahir"       %% "spark-streaming-pubsub"      % sparkVersion,
  "com.thesamet.scalapb"   %% "scalapb-runtime"             % scalapb.compiler.Version.scalapbVersion % "protobuf",
  "org.elasticsearch"      %% "elasticsearch-spark-20"      % "6.5.0",
  "com.thesamet.scalapb"   %% "sparksql-scalapb"            % "0.7.0",
  "com.google.code.gson"    % "gson"                        % "1.7.1",
  "com.thesamet.scalapb"   %% "sparksql-scalapb-gen"        % "0.7.0",
  "com.google.cloud"        % "google-cloud-pubsub"         % "1.53.0"
)

logLevel in assembly := Level.Debug

// sort out issues with deduplicate files
assemblyMergeStrategy in assembly := {
  case PathList("org","aopalliance", xs @ _*) => MergeStrategy.last
  case PathList("javax", "inject", xs @ _*) => MergeStrategy.last
  case PathList("javax", "servlet", xs @ _*) => MergeStrategy.last
  case PathList("javax", "activation", xs @ _*) => MergeStrategy.last
  case PathList("org", "apache", xs @ _*) => MergeStrategy.last
  case PathList("com", "google", xs @ _*) => MergeStrategy.last
  case PathList("com", "esotericsoftware", xs @ _*) => MergeStrategy.last
  case PathList("com", "codahale", xs @ _*) => MergeStrategy.last
  case PathList("com", "yammer", xs @ _*) => MergeStrategy.last
  case PathList("META-INF", xs @ _*) =>
    (xs map {_.toLowerCase}) match {
      case ("manifest.mf" :: Nil) | ("index.list" :: Nil) | ("dependencies" :: Nil) =>
        MergeStrategy.discard
      case ps@(x :: xs) if ps.last.endsWith(".sf") || ps.last.endsWith(".dsa") =>
        MergeStrategy.discard
      case "plexus" :: xs =>
        MergeStrategy.discard
      case "services" :: xs =>
        MergeStrategy.filterDistinctLines
      case ("spring.schemas" :: Nil) | ("spring.handlers" :: Nil) =>
        MergeStrategy.filterDistinctLines
      case _ => MergeStrategy.first
  }
  case "about.html" => MergeStrategy.rename
  case "META-INF/ECLIPSEF.RSA" => MergeStrategy.last
  case "META-INF/mailcap" => MergeStrategy.last
  case "META-INF/mimetypes.default" => MergeStrategy.last
  case "plugin.properties" => MergeStrategy.last
  case "log4j.properties" => MergeStrategy.last
  case "overview.html" => MergeStrategy.last
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}

PB.targets in Compile := Seq(
  scalapb.gen() -> (sourceManaged in Compile).value
)

assemblyOption in assembly := (assemblyOption in assembly).value.copy(includeScala = false)
assemblyJarName in assembly := "pw-bd-project-meetup-streaming.jar"