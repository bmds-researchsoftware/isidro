name := """ISIDRO"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.7"

scalacOptions ++= Seq("-feature", "-deprecation", "-unchecked", "-language:reflectiveCalls", "-language:postfixOps", "-language:implicitConversions")

resolvers ++= Seq(
	"Scalaz Bintray Repo" at "https://dl.bintray.com/scalaz/releases",
	"Atlassian Releases" at "https://maven.atlassian.com/public/",
    "sonatype-releases" at "https://oss.sonatype.org/content/repositories/releases/",
	Resolver.sonatypeRepo("snapshots")
)

routesGenerator := InjectedRoutesGenerator

includeFilter in (Assets, LessKeys.less) := "*.less"

excludeFilter in (Assets, LessKeys.less) := "_*.less"

pipelineStages := Seq(rjs, digest, gzip)

RjsKeys.mainModule := "main"

doc in Compile <<= target.map(_ / "none")


libraryDependencies ++= Seq(
  cache,
  ws,
  evolutions,
  specs2 % Test,
  "org.webjars" %% "webjars-play" % "2.4.0",
  "com.typesafe.play" %% "play-slick" % "1.1.1",
  "com.typesafe.play" %% "play-slick-evolutions" % "1.1.1",
  // "mysql" % "mysql-connector-java" % "5.1.34",
  // "com.h2database" % "h2" % "1.4.190",
  "org.postgresql" % "postgresql" % "9.4-1206-jdbc42",
  "org.webjars" % "requirejs" % "2.1.19",
  "com.mohiva" %% "play-silhouette" % "3.0.0",
  "com.adrianhurt" %% "play-bootstrap3" % "0.4.4-P24",	// Add bootstrap3 helpers and field constructors (http://play-bootstrap3.herokuapp.com/)
  "com.typesafe.play" %% "play-mailer" % "3.0.1",
  "net.codingwell" %% "scala-guice" % "4.0.0",  // ??
  "net.ceedubs" %% "ficus" % "1.1.2",  // ??
  "com.mohiva" %% "play-silhouette-testkit" % "3.0.0" % "test", // ??
  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % "test", // scala test + play

  // file conversion libraries
  "org.csveed" % "csveed" % "0.4.0",
  "org.apache.commons" % "commons-io" % "1.3.2", // https://mvnrepository.com/artifact/org.apache.commons/commons-io
  "org.apache.poi" % "poi" % "3.14",  // https://mvnrepository.com/artifact/org.apache.poi/poi-ooxml
  "org.apache.poi" % "poi-ooxml" % "3.14", // https://mvnrepository.com/artifact/org.apache.poi/poi-ooxml-schemas
  "org.apache.poi" % "poi-ooxml-schemas" % "3.14", // https://mvnrepository.com/artifact/org.apache.poi/poi-ooxml-schemas
  "org.bouncycastle" % "bcprov-jdk15on" % "1.54",
  "org.bouncycastle" % "bcpkix-jdk15on" % "1.54",
  filters
)

fork in run := true
