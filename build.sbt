import com.typesafe.sbt.packager.docker.DockerChmodType
import com.typesafe.sbt.packager.docker.DockerPermissionStrategy


name := """play-http-api"""
organization := "io.hiis"
maintainer := "@hiis-io"
version := "1.0-SNAPSHOT"
scalaVersion := "2.12.15"

lazy val root = (project in file(".")).enablePlugins(PlayScala, AshScriptPlugin)
scalacOptions ++= Seq("-deprecation", "-language:_", "-Ypartial-unification")


val reactiveMongoVersion = "0.20.13-play28"
val silhouetteVersion = "7.0.7"
val playMailerVersion = "8.0.1"
val playJsonVersion = "2.8.2"
val swaggerUIVersion = "3.25.2"
val swaggerPlay2Version = "3.1.0"
val ficusVersion = "1.5.2"
val catsVersion = "2.7.0"

libraryDependencies ++= Seq(
  //Reactive MongoDB driver
  "org.reactivemongo" %% "play2-reactivemongo" % reactiveMongoVersion,

  //Silhouette authentication library dependencies
  "io.github.honeycomb-cheesecake" %% "play-silhouette" % silhouetteVersion,
  "io.github.honeycomb-cheesecake" %% "play-silhouette-persistence" % silhouetteVersion,
  "io.github.honeycomb-cheesecake" %% "play-silhouette-password-bcrypt" % silhouetteVersion,
  "io.github.honeycomb-cheesecake" %% "play-silhouette-crypto-jca" % silhouetteVersion,
  "io.github.honeycomb-cheesecake" %% "play-silhouette-testkit" % silhouetteVersion % "test",

  //Configuration and dependency injection
  "com.iheart" %% "ficus" % ficusVersion,
  "net.codingwell" %% "scala-guice" % "5.0.2",
  guice,

  //SMTP and Email
  "com.typesafe.play" %% "play-mailer" % playMailerVersion,
  "com.typesafe.play" %% "play-mailer-guice" % playMailerVersion,

  //Json Serialization and Deserialization library
  "com.typesafe.play" %% "play-json" % playJsonVersion,
  "com.typesafe.play" %% "play-json-joda" % playJsonVersion,

  //Swagger related dependencies
  "com.github.dwickern" %% "swagger-play2.8" % swaggerPlay2Version,
  "org.webjars" % "swagger-ui" % swaggerUIVersion,
  "io.swagger" % "swagger-core" % "1.6.2",
  "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.11.1",

  //Misc
  "org.typelevel" %% "cats-core" % catsVersion,

  //Testing
  "org.scalatestplus.play" %% "scalatestplus-play" % "5.0.0" % Test,
  specs2 % Test,
  ehcache
)

// Test
Test / unmanagedResourceDirectories += (baseDirectory.value / "target/web/public/test")

// Docker
dockerChmodType := DockerChmodType.UserGroupWriteExecute
dockerPermissionStrategy := DockerPermissionStrategy.CopyChown
Docker / maintainer := "dev@hiis.io"
Docker / packageName := "play-http-api"
Docker / version := sys.env.getOrElse("BUILD_NUMBER", "0")
Docker / daemonUserUid  := None
Docker / daemonUser := "daemon"
dockerExposedPorts := Seq(9000)
dockerBaseImage := "openjdk:8-jre-alpine"
dockerRepository := sys.env.get("ecr_repo")
dockerUpdateLatest := true


// Resolvers
resolvers += "atlassian-maven" at "https://maven.atlassian.com/content/repositories/atlassian-public"
