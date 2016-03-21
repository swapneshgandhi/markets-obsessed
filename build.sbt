name := "Markets-Obsessed"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.7"

resolvers ++= Seq(
  "Kundera" at "https://oss.sonatype.org/content/repositories/releases",
  "Kundera missing" at "http://kundera.googlecode.com/svn/maven2/maven-missing-resources",
  "Scale 7" at "https://github.com/s7/mvnrepo/raw/master",
  "OpenGamma" at "http://maven.opengamma.com/nexus/content/groups/public",
  "Job Server Bintray" at "https://dl.bintray.com/spark-jobserver/maven"
)

libraryDependencies ++= Seq(
  ws excludeAll ExclusionRule(organization = "org.slf4j") intransitive(), // Play's web services module
  "org.apache.httpcomponents" % "httpclient" % "4.3.1" exclude("commons-logging", "commons-logging"),
  "org.apache.httpcomponents" % "httpclient" % "4.5.1" exclude("commons-logging", "commons-logging"),
  "org.xerial.snappy" % "snappy-java" % "1.1.1.7",
  "com.impetus.client" % "kundera-cassandra" % "2.5" exclude("org.jboss.netty", "netty") exclude("org.hibernate.javax.persistence", "hibernate-jpa-2.0-api")
    exclude("javassist", "javassist") exclude("org.apache.cassandra.deps", "avro") exclude("org.xerial", "snappy")
    exclude("org.jboss.spec.javax.transaction", "jboss-transaction-api_1.1_spec"),
  "com.jimmoores" % "quandl" % "1.3.0",
  "javax.persistence" % "persistence-api" % "2.0",
  "com.typesafe.akka" %% "akka-actor" % "2.3.11" exclude("commons-logging", "commons-logging"),
  "com.typesafe.akka" %% "akka-slf4j" % "2.3.11" exclude("commons-logging", "commons-logging"),
  "org.webjars" % "bootstrap" % "3.0.0",
  "org.webjars" % "flot" % "0.8.0",
  "com.typesafe.akka" %% "akka-testkit" % "2.3.11" % "test",
  "edu.stanford.nlp" % "stanford-corenlp" % "3.4.1",
  "edu.stanford.nlp" % "stanford-corenlp" % "3.4.1" classifier "models",
  "org.twitter4j" % "twitter4j-core" % "4.0.4",
  "org.apache.spark" %% "spark-core" % "1.6.0" % "provided",
  "org.apache.spark" %% "spark-sql" % "1.6.0" % "provided",
  "com.datastax.spark" %% "spark-cassandra-connector" % "1.6.0-M1"
    exclude("io.netty", "netty*") exclude("org.apache.avro", "avro-ipc")
    exclude("org.apache.spark", "spark-core")
    exclude("org.apache.spark", "spark-sql")
    exclude("org.apache.cassandra.deps", "avro")
    exclude("commons-logging", "commons-logging")
    exclude("org.apache.cassandra", "cassandra-clientutil"),
  "spark.jobserver" %% "job-server-api" % "0.6.1" % "provided",
  "org.scalatest" %% "scalatest" % "2.2.6" % "test"
)

fork in run := true

scalaSource in Compile := baseDirectory.value / "src/main/scala"

scalacOptions ++= Seq("-unchecked", "-deprecation")

assemblyJarName in assembly := "Markets-Obsessed.jar"

test in assembly := {}

parallelExecution in Test := false

(testOptions in Test) += Tests.Argument(TestFrameworks.ScalaTest, "-o", "-h", "target/report")

assemblyMergeStrategy in assembly := {
  case PathList("javax", "servlet", xs@_*) => MergeStrategy.first
  case PathList("org", xs@_*) => MergeStrategy.first
  case PathList("play", xs@_*) => MergeStrategy.first
  case PathList(ps@_*) if ps.last endsWith ".html" => MergeStrategy.first
  case PathList(ps@_*) if ps.last endsWith ".properties" => MergeStrategy.first
  case "application.conf" => MergeStrategy.concat
  case "unwanted.txt" => MergeStrategy.discard
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}

fullClasspath in assembly += Attributed.blank(PlayKeys.playPackageAssets.value)