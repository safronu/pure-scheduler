val V = new {
  val newtype        = "0.4.3"
  val cron4s         = "0.6.0"
  val cats           = "2.0.0"
  val refined        = "0.9.13"
  val tofu           = "0.7.4"
  val kindProjector  = "0.11.0"
  val catsEffects    = "2.1.3"
  val contextApplied = "0.1.3"
}

val Deps = new {
  val scalaTest   = List("org.scalatest" %% "scalatest" % "3.1.1")
  val newtype     = List("io.estatico" %% "newtype" % V.newtype)
  val cats        = List("org.typelevel" %% "cats-core" % V.cats)
  val catsEffects = List("org.typelevel" %% "cats-effect" % V.catsEffects)
  val cron4s      = List("com.github.alonsodomin.cron4s" %% "cron4s-core" % V.cron4s)
  val refined = List(
    "eu.timepit" %% "refined" % V.refined,
    "eu.timepit" %% "refined-cats" % V.refined, // optional
  )
  val tofu           = List("ru.tinkoff" %% "tofu" % V.tofu)
  val kindProjector  = "org.typelevel" %% "kind-projector" % V.kindProjector cross CrossVersion.full
  val contextApplied = "org.augustjune" %% "context-applied" % V.contextApplied
}

lazy val scheduler4s = (project in file("."))
  .settings(
    scalacOptions ++= List("-Ymacro-annotations", "-language:postfixOps"),
    scalaVersion     := "2.13.1",
    version          := "0.0.1-SNAPSHOT",
    organization     := "dev.safronu",
    organizationName := "safronu",
    name             := "scheduler4s",
    libraryDependencies ++= Deps.scalaTest
    ++ Deps.newtype
    ++ Deps.cron4s
    ++ Deps.cats
    ++ Deps.tofu
    ++ Deps.refined
    ++ Deps.catsEffects,
    addCompilerPlugin(Deps.kindProjector),
    addCompilerPlugin(Deps.contextApplied)
  )
