lazy val akkaHttpVersion = "10.1.1"
lazy val akkaVersion = "2.5.12"
lazy val circeVersion = "0.9.3"
lazy val deps    = Seq(
      "com.typesafe.akka"  %% "akka-http"              % akkaHttpVersion,
      "com.typesafe.akka"  %% "akka-stream"            % akkaVersion,
      "com.typesafe.akka"  %% "akka-slf4j"             % akkaVersion,
      "com.typesafe.akka"  %% "akka-stream-kafka"      % "0.20",
      "com.lightbend.akka" %% "akka-stream-alpakka-s3" % "0.19",

      "com.lightbend"      %% "kafka-streams-scala"    % "0.2.1",

      "io.circe"           %% "circe-core"             % circeVersion,
      "io.circe"           %% "circe-jawn"             % circeVersion,
      "io.circe"           %% "circe-generic"          % circeVersion,
      "de.heikoseeberger"  %% "akka-http-circe"        % "1.21.0",

      // Needed by circe
      "org.scalamacros"     % "paradise"               % "2.1.1" cross CrossVersion.full,

      "com.typesafe.akka"  %% "akka-http-testkit"      % akkaHttpVersion % Test,
      "com.typesafe.akka"  %% "akka-testkit"           % akkaVersion     % Test,
      "com.typesafe.akka"  %% "akka-stream-testkit"    % akkaVersion     % Test,

      "org.scalatest"      %% "scalatest"              % "3.0.1"         % Test,

      "ch.qos.logback"     % "logback-classic"         % "1.2.3",
      "org.slf4j"          % "slf4j-api"               % "1.7.25",
      "org.slf4j"          % "log4j-over-slf4j"        % "1.7.25",
      "org.slf4j"          % "jul-to-slf4j"            % "1.7.25"
    )
lazy val commonSettings = Seq(
    organization    := "com.jask",
    scalaVersion    := "2.12.6"
  )

lazy val common = (project in file("common"))
  .settings(commonSettings)
  .settings(
    name := "northstar-common",
    libraryDependencies ++= deps
  )

lazy val http = (project in file("http"))
  .settings(commonSettings)
  .settings(
    name := "northstar-http",
    libraryDependencies ++= deps
  )
  .dependsOn(common)

lazy val parse = (project in file("parse"))
  .settings(commonSettings)
  .settings(
    name := "northstar-parse",
    libraryDependencies ++= deps
  )
  .dependsOn(common)
