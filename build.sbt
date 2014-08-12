val buildSettings = Seq(
  organization := "com.blinkbox.books.hermes",
  name := "credit-offer-service",
  version := scala.util.Try(scala.io.Source.fromFile("VERSION").mkString.trim).getOrElse("0.0.0"),
  scalaVersion  := "2.10.4",
  scalacOptions := Seq("-unchecked", "-deprecation", "-feature", "-encoding", "utf8", "-target:jvm-1.7")
)

val dependencySettings = Seq(
  libraryDependencies ++= {
    val akkaV = "2.3.3"
    val sprayV = "1.3.1"
    Seq(
    "com.blinkbox.books"        %%         "common-config"          % "0.9.0" withSources(),
    "com.blinkbox.books"        %%         "common-messaging"       % "1.1.0" withSources(),
    "com.blinkbox.books.hermes" %%         "rabbitmq-ha"            % "4.1.2" withSources(),
    "com.blinkbox.books"        %%         "common-scala-test"      % "0.2.0" withSources(),
    "com.blinkbox.books"        %%         "common-spray"           % "0.14.1" withSources(),
    "commons-dbcp"               %         "commons-dbcp"           % "1.4",
    "io.spray"                   %         "spray-client"           % sprayV,
    "io.spray"                   %         "spray-http"             % sprayV,
    "io.spray"                   %         "spray-httpx"            % sprayV,
    "io.spray"                  %%         "spray-json"             % "1.2.6",
    "org.json4s"                %%         "json4s-jackson"         % "3.2.10",
    "com.h2database"             %         "h2"                     % "1.3.173" % "test",
    "xmlunit"                    %         "xmlunit"                % "1.5" % "test",
    "com.typesafe.akka"         %%         "akka-actor"             % akkaV,
    "com.typesafe.akka"         %%         "akka-slf4j"             % akkaV,
    "com.typesafe.akka"         %%         "akka-testkit"           % akkaV % "test",
    "com.typesafe.slick"        %%         "slick"                  % "2.1.0",
    "joda-time"                  %         "joda-time"              % "2.3",
    "org.joda"                   %         "joda-convert"           % "1.6"
    )
  }
)

rpmPrepSettings

val root = (project in file(".")).
  settings(rpmPrepSettings: _*).
  settings(buildSettings: _*).
  settings(dependencySettings: _*)
