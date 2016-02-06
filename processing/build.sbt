name := "1USAgov-club"

version := "1.0"

scalaVersion := "2.10.5"

resolvers += "Scalaz Bintray Repo" at "https://dl.bintray.com/scalaz/releases"

libraryDependencies ++= Seq(
  "com.datastax.spark"  %%  "spark-cassandra-connector"   % "1.5.0-RC1",
  "com.twitter"         %   "algebird-core_2.10"          % "0.11.0",
  "io.argonaut"         %%  "argonaut"                    % "6.1",
  "org.apache.spark"    %%  "spark-core"                  % "1.6.0",
  "org.apache.spark"    %   "spark-streaming_2.10"        % "1.6.0",
  "org.apache.spark"    %   "spark-streaming-kafka_2.10"  % "1.6.0",
  "org.specs2"          %   "specs2-core_2.10"            % "3.7-scalaz-7.1.6"

)

jarName in assembly := "1USAgov.club-streaming.jar"

mergeStrategy in assembly := {
  case m if m.toLowerCase.endsWith("manifest.mf")          => MergeStrategy.discard
  case m if m.toLowerCase.matches("meta-inf.*\\.sf$")      => MergeStrategy.discard
  case "log4j.properties"                                  => MergeStrategy.discard
  case m if m.toLowerCase.startsWith("meta-inf/services/") => MergeStrategy.filterDistinctLines
  case "reference.conf"                                    => MergeStrategy.concat
  case _                                                   => MergeStrategy.first
}
