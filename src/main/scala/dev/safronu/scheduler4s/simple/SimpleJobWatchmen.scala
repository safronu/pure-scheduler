package dev.safronu.scheduler4s.simple

import tofu.Raise
import tofu.Fire
import cats.Monad
import cats.Parallel
import cats.effect.Timer
import tofu.common.Console
import tofu.concurrent.Daemonic
import dev.safronu.scheduler4s.Transaction
import dev.safronu.scheduler4s.KeyValue
import cats.Traverse
import dev.safronu.scheduler4s.KeyValueS
import dev.safronu.scheduler4s.model.TriggerId
import dev.safronu.scheduler4s.model.JobId
import dev.safronu.scheduler4s.simple.SimpleTrigger
import dev.safronu.scheduler4s.simple.SimpleJobsExtractor
import dev.safronu.scheduler4s.TriggersWatchmen
import dev.safronu.scheduler4s.syntax._
import tofu.syntax.foption._
import java.time.LocalDateTime
import cats.implicits._
import cats.syntax._
import cats._
import java.time.ZoneOffset
import tofu.syntax.fire._
import dev.safronu.scheduler4s.TriggerInterpreter

final class SimpleJobWatchmen[F[_]: Raise[*[_], Exception]: Fire: Monad: Parallel: Timer: Console: Daemonic[*[_], Throwable]: Transaction: KeyValue[*[_],
                                                                                                                                                    TriggerId,
                                                                                                                                                    JobId]: KeyValue[
  *[_],
  TriggerId,
  TriggerType
]: KeyValue[*[_], JobId, JobInput]: KeyValue[*[_], JobId, JobInput => F[Unit]]: TriggerInterpreter[*[_], TriggerType], TriggerType, JobInput, G[_]: Traverse](
  implicit TriggerKV: KeyValueS[Lambda[A => F[List[A]]], TriggerId, TriggerType]
) extends TriggersWatchmen[F] {
  import scala.concurrent.duration._

  def deleteAll[TriggerRepr, JobInput, F[_]: Monad: Transaction: Raise[*[_], Exception]: KeyValue[*[_], TriggerId, JobId]: KeyValue[*[_], TriggerId, TriggerRepr]: KeyValue[
    *[_],
    JobId,
    JobInput
  ]](triggerId: TriggerId) = {
    F.transaction {
      for {
        jobId <- F.get[F, TriggerId, JobId](triggerId).orThrow(new Exception("TriggerId is not presented in trigger-job kv"))
        _     <- F.deleteF[F, TriggerId, TriggerRepr](triggerId)
        _     <- F.deleteF[F, JobId, JobInput](jobId)
        _     <- F.deleteF[F, TriggerId, JobId](triggerId)
      } yield ()
    }
  }

  def diff(start: LocalDateTime, end: LocalDateTime): Long = {
    end.toInstant(ZoneOffset.UTC).toEpochMilli - start.toInstant(ZoneOffset.UTC).toEpochMilli()
  }

  def start: F[tofu.concurrent.Daemon0[F]] = {
    val readAndRunJobs =
      for {
        triggers <- TriggerKV.all
        _ <- triggers.parTraverse {
          case (triggerId, trigger) =>
            for {
              jobId       <- F.get[F, TriggerId, JobId](triggerId).orThrow(new Exception(s"Not found job for triggerId=$triggerId"))
              jobInput    <- F.get[F, JobId, JobInput](jobId).orThrow(new Exception(s"Not found input for job"))
              jobCreator  <- F.get[F, JobId, JobInput => F[Unit]](jobId).orThrow(new Exception("blbla"))
              job         = jobCreator(jobInput)
              now         <- Timer[F].now
              triggerRepr <- F.get[F, TriggerId, TriggerType](triggerId).orThrow(new Exception("b"))
              date        <- F.next(triggerRepr)
              tts         = diff(start = now, end = date)
              _           <- Console[F].putStrLn(s"Now: $now, Target date: $date, StringDiff: ${tts.millis}")
              _           <- (F.sleep(tts millis) >> job).fireAndForget
              _           <- deleteAll[TriggerType, JobInput, F](triggerId)
            } yield ()
        }
      } yield ()

    F.daemonize(
      F.iterateWhile(readAndRunJobs >> F.sleep(1 second))(const(true))
    )
  }

}
