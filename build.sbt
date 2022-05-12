name := """play-http-api-seed"""
organization := "io.hiis"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)
scalacOptions ++= Seq("-deprecation", "-language:_", "-Ypartial-unification")

scalaVersion := "2.12.15"
val reactiveMongoVersion = "0.20.13-play28"
val silhouetteVersion = "7.0.7"
val playMailerVersion = "8.0.1"
val playJsonVersion = "2.8.2"
val swaggerUIVersion = "3.6.1"
val swaggerPlay2Version = "1.7.1"
val ficusVersion = "1.5.2"
val catsVersion = "2.7.0"

libraryDependencies ++= Seq(
  "org.reactivemongo" %% "play2-reactivemongo" % reactiveMongoVersion,
  "io.github.honeycomb-cheesecake" %% "play-silhouette" % silhouetteVersion,
  "io.github.honeycomb-cheesecake" %% "play-silhouette-persistence" % silhouetteVersion,
  "io.github.honeycomb-cheesecake" %% "play-silhouette-password-bcrypt" % silhouetteVersion,
  "io.github.honeycomb-cheesecake" %% "play-silhouette-crypto-jca" % silhouetteVersion,
  "io.github.honeycomb-cheesecake" %% "play-silhouette-testkit" % silhouetteVersion % "test",
  "com.iheart" %% "ficus" % ficusVersion,
  "com.typesafe.play" %% "play-mailer" % playMailerVersion,
  "com.typesafe.play" %% "play-mailer-guice" % playMailerVersion,
  "com.typesafe.play" %% "play-json" % playJsonVersion,
  "com.typesafe.play" %% "play-json-joda" % playJsonVersion,
  "io.swagger" %% "swagger-play2" % swaggerPlay2Version,
  "org.webjars" % "swagger-ui" % swaggerUIVersion,
  "org.typelevel" %% "cats-core" % catsVersion,
  "net.codingwell" %% "scala-guice" % "5.0.2",
  "org.scalatestplus.play" %% "scalatestplus-play" % "5.0.0" % Test,
  specs2 % Test,
  ehcache,
  guice
)

Test / unmanagedResourceDirectories += (baseDirectory.value / "target/web/public/test")

resolvers += "atlassian-maven" at "https://maven.atlassian.com/content/repositories/atlassian-public"

// Adds additional packages into Twirl
//TwirlKeys.templateImports += "io.hiis.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "io.hiis.binders._"
