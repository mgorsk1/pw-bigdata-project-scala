name          := "MeetupRSVPStreaming"
organization  := "com.gorskimariusz"
description   := "Final project on Postgraduate Studies (Big Data / Hadoop) - Warsaw University of Technology 02/2019"

version := "0.1"

scalaVersion := "2.11.8"

val sparkVersion = "2.2.2"

PB.targets in Compile := Seq(
  scalapb.gen() -> (sourceManaged in Compile).value
)

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