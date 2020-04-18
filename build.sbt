val V = new {
  val newtype = "0.4.3"
  val cron4s = "0.6.0"
  val cats = "2.0.0"
}

val Deps = new{
  val scalaTest = List("org.scalatest" %% "scalatest" % "3.1.1")
  val newtype = List("io.estatico" %% "newtype" % V.newtype)
  val cats = List("org.typelevel" %% "cats-core" % V.cats)
  val cron4s = List("com.github.alonsodomin.cron4s" %% "cron4s-core" % V.cron4s)
}

lazy val scheduler4s = (project in file("."))
  .settings(
    scalaVersion := "2.13.1",
    version := "0.0.1-SNAPSHOT",
    organization := "dev.safronu",
    organizationName := "safronu",
    name := "scheduler4s",
    libraryDependencies ++= Deps.scalaTest 
      ++ Deps.newtype 
      ++ Deps.cron4s 
      ++ Deps.cats,
  )