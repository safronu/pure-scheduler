package dev.safronu.scheduler4s

import cats.effect.Sync
import scala.collection.mutable
import java.time.LocalDateTime
import dev.safronu.scheduler4s.TriggerInterpreter
import dev.safronu.scheduler4s.model._
import dev.safronu.scheduler4s._
import tofu.concurrent.Daemon0
import tofu.concurrent.Daemon
import tofu.concurrent.Daemonic
import cats.Monad
import tofu.common.Console
import cats.syntax._
import cats.implicits._
import cats._
import cats.Applicative
import cats.effect.Timer
import java.util.concurrent.TimeUnit
import java.time.Instant
import java.{util => ju}
import java.time.Period
import java.time.Period
import java.time.ZoneOffset
import io.estatico.newtype.ops._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.Uuid
import tofu._
import tofu.syntax.handle._
import tofu.syntax.raise._
import tofu.syntax.foption._
import tofu.syntax.fire._
import cats.effect.IOApp
import cats.effect.IO
import cats.effect.ExitCode
import io.estatico.newtype.Coercible
import syntax._

trait FUUID[F[_]] {
  def random: F[String Refined Uuid]
}

final class FUUIDImpl[F[_]: Sync: Raise[*[_], Exception]] extends FUUID[F] {
  def random: F[String Refined Uuid] = {
    F.delay(Refined.unsafeApply(ju.UUID.randomUUID().toString))
  }
}

final class InmemoryKV[F[_]: Sync, A, B] extends KeyValue[F, A, B] with KeyValueS[Lambda[C => F[List[C]]], A, B] {

  val map: mutable.Map[A, B]                  = mutable.Map()
  override def save(id: A, value: B): F[Unit] = Sync[F].delay(map.put(id, value))

  override def read(id: A): F[Option[B]] = Sync[F].delay(map.get(id))

  override def delete(id: A): F[Option[B]] = Sync[F].delay(map.remove(id))

  override def update(id: A, value: B): F[Unit] = Sync[F].delay(map.put(id, value))

  override def all: F[List[(A, B)]] = Sync[F].delay(map.toList)
}

case class SimpleTrigger(date: LocalDateTime)

final class SimpleJobsExtractor[F[_]: Monad: Raise[*[_], Exception]: Console: Daemonic[*[_], Throwable]](implicit KV: JobsInputKV[F, String], FK: JobTriggerLinkKV[F]) {
  def jobs(id: TriggerId): F[F[Unit]] =
    for {
      jobId    <- FK.read(id).orThrow(new Exception(s"There is no such trigger: $id"))
      jobInput <- KV.read(jobId).orThrow(new Exception(s"There is no such job: $id"))
    } yield Console[F].putStrLn(s"hello, $jobInput")
}

final class SimpleTriggerInterpreter[F[_]: Applicative] extends TriggerInterpreter[F, F, SimpleTrigger] {
  override def next(triggerRepr: SimpleTrigger): F[LocalDateTime] = triggerRepr.date.pure[F]
}

object syntax {
  implicit class TimerOps[F[_]](timer: Timer[F]) {
    def convert(ts: Long)           = LocalDateTime.ofInstant(Instant.ofEpochMilli(ts), ju.TimeZone.getDefault().toZoneId())
    def now(implicit G: Functor[F]) = timer.clock.realTime(TimeUnit.MILLISECONDS).map(convert)
  }

  implicit class FUUIDSyntax[F[_], A](f: F[A]) {
    def coerce[B: Coercible[A, *]](implicit F: Functor[F]): F[B] = f.map(_.coerce[B])
  }
  implicit class KVSyntax[A](f: A) {
    def saveF[F[_], A, B](key: A, value: B)(implicit KV: KeyValue[F, A, B]) = KV.save(key, value)
    def deleteF[F[_], A, B](key: A)(implicit KV: KeyValue[F, A, B]) = KV.delete(key)
    def get[F[_], A, B](key: A)(implicit KV: KeyValue[F, A, B]) = KV.read(key)
  }
}

final class SimpleJobWatchmen[F[_]: Raise[*[_], Exception]
                                  : Fire
                                  : Monad
                                  : Parallel
                                  : Timer
                                  : Console
                                  : Daemonic[*[_], Throwable]
                                  : KeyValue[*[_], TriggerId, JobId]
                                  : KeyValue[*[_], TriggerId, SimpleTrigger]
                                  : KeyValue[*[_], JobId, String], 
                              G[_]: Traverse
                              ](implicit TriggerKV: TriggerAll[F, G, SimpleTrigger],
                                    jobsExtractor: SimpleJobsExtractor[F])
  extends TriggersWatchmen[F] {
  import scala.concurrent.duration._

  def deleteAll[TriggerRepr, JobInput, 
                F[_]: Monad
                    : Raise[*[_], Exception]
                    : KeyValue[*[_], TriggerId, JobId]
                    : KeyValue[*[_], TriggerId, TriggerRepr]
                    : KeyValue[*[_], JobId, JobInput]
                ](triggerId: TriggerId) = {
    for{
      jobId <- F.get[F, TriggerId, JobId](triggerId).orThrow(new Exception("TriggerId is not presented in trigger-job kv"))
      _     <- F.deleteF[F, TriggerId, TriggerRepr](triggerId)
      _     <- F.deleteF[F, JobId, JobInput](jobId)
      _     <- F.deleteF[F, TriggerId, JobId](triggerId)
    } yield ()
  }

  def diff(start: LocalDateTime, end: LocalDateTime): Long = {
    end.toInstant(ZoneOffset.UTC).toEpochMilli - start.toInstant(ZoneOffset.UTC).toEpochMilli()
  }

  def start: F[tofu.concurrent.Daemon0[F]] = {
    val readAndRunJobs =
      for {
        triggers <- TriggerKV.all
        _ <- triggers.parTraverse {
          case (triggerId, SimpleTrigger(date)) =>
            for {
              job <- jobsExtractor.jobs(triggerId)
              now <- Timer[F].now
              tts = diff(start = now, end = date)
              _   <- Console[F].putStrLn(s"Now: $now, Target date: $date, Diff: ${tts.millis}")
              _   <- (F.sleep(tts millis) >> job).fireAndForget
              _   <- deleteAll[SimpleTrigger, String, F](triggerId)
            } yield ()
        }
      } yield ()

    F.daemonize(
      F.iterateWhile(readAndRunJobs >> F.sleep(1 second))(_ => true)
    )
  }

}

object Test extends IOApp {
  implicit val jobsInputKv               = new InmemoryKV[IO, JobId, String]
  implicit val jobsTriggerLinkKv         = new InmemoryKV[IO, TriggerId, JobId]
  implicit val triggerDataKv             = new InmemoryKV[IO, TriggerId, SimpleTrigger]
  implicit val jobsExtractor             = new SimpleJobsExtractor[IO]
  implicit val FUUIDImpl: FUUID[IO]      = new FUUIDImpl()
  val jobsWatchmen: TriggersWatchmen[IO] = new SimpleJobWatchmen[IO, List]

  def scheduleHello[F[_]: Monad: FUUID: KeyValue[*[_], JobId, String]: KeyValue[*[_], TriggerId, JobId]: KeyValue[*[_], TriggerId, SimpleTrigger]](
    name: String,
    date: LocalDateTime
  ): F[Unit] =
    for {
      jobId     <- F.random.coerce[JobId]
      _         <- F.saveF[F, JobId, String](jobId, name)
      triggerId <- F.random.coerce[TriggerId]
      _         <- F.saveF[F, TriggerId, JobId](triggerId, jobId)
      _         <- F.saveF[F, TriggerId, SimpleTrigger](triggerId, SimpleTrigger(date))
    } yield ()

  def run(args: List[String]): IO[ExitCode] =
    for {
      daemon <- jobsWatchmen.start
      _      <- scheduleHello[IO]("Ulad", LocalDateTime.now().plusMinutes(2))
      _      <- daemon.join
    } yield ExitCode.Success
}
