name := "sdk-dslink-scala"
organization := "com.uralian"

version := "0.6.0-SNAPSHOT"

scalaVersion := "2.12.7"
crossScalaVersions := Seq("2.10.5", "2.11.7", "2.12.7")

scalacOptions ++= Seq(
  "-feature",
  "-unchecked",
  "-deprecation",
  "-Yno-adapted-args",
  "-Ywarn-dead-code",
  "-language:_",
  "-target:jvm-1.8",
  "-encoding", "UTF-8"
)

//scalacOptions in (Compile, doc) ++= Seq("-no-link-warnings")
//run in Compile <<= Defaults.runTask(fullClasspath in Compile, mainClass in (Compile, run), runner in (Compile, run))

// scoverage options
//coverageEnabled := true
//coverageExcludedPackages := "org\\.dsa\\.iot\\.scala\\.netty\\.*;org\\.dsa\\.iot\\.scala\\.examples\\.*;.*DSAConnector;.*DSAEventListener"
coverageExcludedPackages := "com\\.uralian\\.dsa\\.examples\\.*"
coverageMinimum := 80
coverageFailOnMinimum := true

// test options
configs(IntegrationTest)
Defaults.itSettings
IntegrationTest / fork := true

// publishing options
publishMavenStyle := true
publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases" at nexus + "service/local/staging/deploy/maven2")
}
pomIncludeRepository := { _ => false }
pomExtra := <url>https://github.com/uralian/sdk-dslink-scala</url>
  <licenses>
    <license>
      <name>The Apache License, Version 2.0</name>
      <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>
      <distribution>repo</distribution>
    </license>
  </licenses>
  <scm>
    <url>scm:git:https://github.com/uralian/sdk-dslink-scala.git</url>
    <connection>scm:git:git@github.com:uralian/sdk-dslink-scala.git</connection>
  </scm>
  <developers>
    <developer>
      <id>snark</id>
      <name>Vlad Orzhekhovskiy</name>
      <email>vlad@uralian.com</email>
      <url>http://uralian.com</url>
    </developer>
  </developers>

pgpSecretRing := file("local.secring.gpg")

pgpPublicRing := file("local.pubring.gpg")

// dependencies
libraryDependencies ++= Seq(
  "com.typesafe" % "config" % "1.3.2",
  //  "org.slf4j"           % "slf4j-log4j12"           % "1.6.1",
  "org.iot-dsa" % "dslink" % "0.18.3",
  //  		exclude("org.slf4j", "*")
  //  		exclude("org.iot-dsa", "logging")
  //  		exclude("io.netty", "*"),
  //  "io.netty"            % "netty-all"               % "4.0.33.Final",
  "io.reactivex" %% "rxscala" % "0.26.5",
  "org.scalatest" %% "scalatest" % "3.0.5" % "it,test",
  "org.scalacheck" %% "scalacheck" % "1.14.0" % "it,test",
  "org.mockito" % "mockito-core" % "2.23.4" % "it,test"
)
