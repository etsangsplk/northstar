lazy val akkaHttpVersion = "10.1.1"
lazy val akkaVersion    = "2.5.12"

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization    := "com.jask",
      scalaVersion    := "2.12.5"
    )),
    name := "Northstar",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-http"            % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-stream"          % akkaVersion,
      "com.typesafe.akka" %% "akka-slf4j"           % akkaVersion,
      "com.typesafe.akka" %% "akka-stream-kafka"    % "0.20",

      "com.typesafe.akka" %% "akka-http-testkit"    % akkaHttpVersion % Test,
      "com.typesafe.akka" %% "akka-testkit"         % akkaVersion     % Test,
      "com.typesafe.akka" %% "akka-stream-testkit"  % akkaVersion     % Test,
      "org.scalatest"     %% "scalatest"            % "3.0.1"         % Test,

      "ch.qos.logback"    % "logback-classic"       % "1.2.3",
      "org.slf4j"         % "slf4j-api"             % "1.7.25",
      "org.slf4j"         % "log4j-over-slf4j"      % "1.7.25",
      "org.slf4j"         % "jul-to-slf4j"          % "1.7.25"
    )
  )
