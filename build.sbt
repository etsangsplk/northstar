lazy val akkaHttpVersion = "10.1.1"
lazy val akkaVersion    = "2.5.12"
lazy val deps    = Seq(
      "com.typesafe.akka" %% "akka-http"            % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-stream"          % akkaVersion,
      "com.typesafe.akka" %% "akka-slf4j"           % akkaVersion,
      "com.typesafe.akka" %% "akka-stream-kafka"    % "0.20",

      "com.lightbend" % "kafka-streams-scala_2.12" % "0.2.1",

      "com.typesafe.akka" %% "akka-http-testkit"    % akkaHttpVersion % Test,
      "com.typesafe.akka" %% "akka-testkit"         % akkaVersion     % Test,
      "com.typesafe.akka" %% "akka-stream-testkit"  % akkaVersion     % Test,

      "org.scalatest"     %% "scalatest"            % "3.0.1"         % Test,

      "ch.qos.logback"    % "logback-classic"       % "1.2.3",
      "org.slf4j"         % "slf4j-api"             % "1.7.25",
      "org.slf4j"         % "log4j-over-slf4j"      % "1.7.25",
      "org.slf4j"         % "jul-to-slf4j"          % "1.7.25"
    )

lazy val common = (project in file("common"))
  .settings(
    inThisBuild(List(
      organization    := "com.jask",
      scalaVersion    := "2.12.6"
    )),
    name := "northstar-common",
    libraryDependencies ++= deps
  )

lazy val http = (project in file("http"))
  .settings(
    inThisBuild(List(
      organization    := "com.jask",
      scalaVersion    := "2.12.6"
    )),
    name := "northstar-http",
    libraryDependencies ++= deps
  )
  .dependsOn(common)

lazy val parse = (project in file("parse"))
  .settings(
    inThisBuild(List(
      organization    := "com.jask",
      scalaVersion    := "2.12.6"
    )),
    name := "northstar-parse",
    libraryDependencies ++= deps
  ) 
  .dependsOn(common)
