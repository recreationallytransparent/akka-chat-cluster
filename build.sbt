name := "pickard-akka-chat-cluster"

version := "0.1"

scalaVersion := "2.12.6"

lazy val akkaVersion = "2.5.8"
lazy val akkaHttpVersion = "10.1.0-RC1"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-cluster" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster-tools" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test",
  "org.json4s" %% "json4s-jackson" % "3.6.0-M4"
)

