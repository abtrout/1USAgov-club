name := "1USAgov-club"

version := "1.0"

scalaVersion := "2.10.5"

libraryDependencies ++= Seq(
  "com.twitter"       %   "algebird-core_2.10"          % "0.11.0",
  "io.argonaut"       %%  "argonaut"                    % "6.1",
  "org.apache.spark"  %%  "spark-core"                  % "1.6.0",
  "org.apache.spark"  %   "spark-streaming_2.10"        % "1.6.0",
  "org.apache.spark"  %   "spark-streaming-kafka_2.10"  % "1.6.0",
  "org.specs2"        %   "specs2-core_2.10"            % "3.7-scalaz-7.1.6"
)

jarName in assembly := "1USAgovclub-streaming.jar"
