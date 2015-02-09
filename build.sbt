val buildSettings = Seq(
  organization := "com.blinkbox.books.hermes",
  name := "credit-offer-service",
  version := scala.util.Try(scala.io.Source.fromFile("VERSION").mkString.trim).getOrElse("0.0.0"),
  scalaVersion  := "2.10.4",
  scalacOptions := Seq("-unchecked", "-deprecation", "-feature", "-encoding", "utf8", "-target:jvm-1.7",
    "-Xfatal-warnings", "-Xlint", "-Yno-adapted-args", "-Xfuture")
)

val dependencySettings = Seq(
  libraryDependencies ++= {
    val akkaV = "2.3.6"
    val sprayV = "1.3.1"
    Seq(
    "com.blinkbox.books"        %%         "common-config"          % "1.4.1",
    "com.blinkbox.books"        %%         "common-messaging"       % "1.1.1",
    "com.blinkbox.books"        %%         "common-scala-test"      % "0.3.0"                   % Test,
    "com.blinkbox.books"        %%         "common-spray"           % "0.17.0",
    "com.blinkbox.books.hermes" %%         "rabbitmq-ha"            % "6.0.7",
    "com.blinkbox.books.hermes" %%         "message-schemas"        % "0.6.0",
    "org.apache.commons"         %         "commons-dbcp2"          % "2.0.1",
    "mysql"                      %         "mysql-connector-java"   % "5.1.32",
    "io.spray"                  %%         "spray-client"           % sprayV,
    "org.json4s"                %%         "json4s-jackson"         % "3.2.10",
    "com.h2database"             %         "h2"                     % "1.3.173"                 % Test,
    "xmlunit"                    %         "xmlunit"                % "1.5"                     % Test,
    "com.typesafe.akka"         %%         "akka-testkit"           % akkaV                     % Test,
    "com.typesafe.slick"        %%         "slick"                  % "2.1.0",
    "joda-time"                  %         "joda-time"              % "2.3",
    "org.joda"                   %         "joda-convert"           % "1.6",
    "org.joda"                   %         "joda-money"             % "0.9.1"
    )
  }
)

// Needed to keep database tests happy, it seems. Would be good to use unique names for test DBs to avoid this problem.
parallelExecution in Test := false

val root = (project in file(".")).
  settings(buildSettings: _*).
  settings(dependencySettings: _*)
