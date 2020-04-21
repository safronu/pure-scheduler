package dev.safronu.scheduler4s.simple

import dev.safronu.scheduler4s.model.JobId
import dev.safronu.scheduler4s.model.TriggerId
import cats.effect.IO
import dev.safronu.scheduler4s.FUUID
import dev.safronu.scheduler4s.TriggersWatchmen
import cats.effect.IOApp
import dev.safronu.scheduler4s.FUUIDImpl
import dev.safronu.scheduler4s.Transaction
import cats.Monad
import dev.safronu.scheduler4s.KeyValue
import java.time.LocalDateTime
import cats.effect.ExitCode
import dev.safronu.scheduler4s.syntax._
import cats.implicits._
import cats.syntax._
import cats._
import tofu.common.Console
import cats.effect.Timer
import dev.safronu.scheduler4s.TriggerInterpreter

object Test extends IOApp {
  import dev.safronu.scheduler4s.simple.ConcurrentHashMapKV
  implicit val jobsInputKv          = new ConcurrentHashMapKV[IO, JobId, String]
  implicit val jobsTriggerLinkKv    = new ConcurrentHashMapKV[IO, TriggerId, JobId]
  implicit val triggerDataKv        = new ConcurrentHashMapKV[IO, TriggerId, SimpleTrigger]
  implicit val jobStorage           = new ConcurrentHashMapKV[IO, JobId, String => IO[Unit]]
  implicit val jobsExtractor        = new SimpleJobsExtractor[IO]
  implicit val FUUIDImpl: FUUID[IO] = new FUUIDImpl()
  implicit val fakeTransaction = new Transaction[IO] {
    def transaction(f: IO[Unit]): IO[Unit] = f
  }
  implicit val simpleTriggerInterpreter: TriggerInterpreter[IO, SimpleTrigger] = new SimpleTriggerInterpreter[IO]()

  val jobsWatchmen: TriggersWatchmen[IO] = new dev.safronu.scheduler4s.simple.SimpleJobWatchmen[IO, SimpleTrigger, String, List]

  def schedule[F[_]: Monad: FUUID: KeyValue[*[_], JobId, String => F[Unit]]: KeyValue[*[_], JobId, String]: KeyValue[*[_], TriggerId, JobId]: KeyValue[*[_],
                                                                                                                                                       TriggerId,
                                                                                                                                                       SimpleTrigger]](
    job: String => F[Unit],
    input: String,
    date: LocalDateTime
  ): F[Unit] =
    for {
      jobId     <- F.random.coerce[JobId]
      _         <- F.saveF[F, JobId, String](jobId, input)
      _         <- F.saveF[F, JobId, String => F[Unit]](jobId, job)
      triggerId <- F.random.coerce[TriggerId]
      _         <- F.saveF[F, TriggerId, JobId](triggerId, jobId)
      _         <- F.saveF[F, TriggerId, SimpleTrigger](triggerId, SimpleTrigger(date))
    } yield ()

  def job(x: String): IO[Unit]        = Console[IO].putStrLn(s"Hello, $x")
  def anotherJob(x: String): IO[Unit] = Console[IO].putStrLn(s"Goodbye, $x")
  def run(args: List[String]): IO[ExitCode] =
    for {
      daemon <- jobsWatchmen.start
      now    <- Timer[IO].now
      _ <- schedule[IO](
        job   = job,
        input = "Ulad",
        date  = now.plusSeconds(30)
      )
      _ <- schedule[IO](
        job   = anotherJob,
        input = "Ulad",
        date  = now.plusMinutes(1)
      )
      _ <- daemon.join
    } yield ExitCode.Success
}
