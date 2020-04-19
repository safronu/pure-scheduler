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

final class InmemoryKV[F[_]: Sync, A, B] extends KeyValue[F, A, B] {

  val map: mutable.Map[A, B]                  = mutable.Map()
  override def save(id: A, value: B): F[Unit] = Sync[F].delay(map.put(id, value))

  override def read(id: A): F[Option[B]] = Sync[F].delay(map.get(id))

  override def delete(id: A): F[Option[B]] = Sync[F].delay(map.remove(id))

  override def update(id: A, value: B): F[Unit] = Sync[F].delay(map.put(id, value))

}

case class SimpleTrigger(date: LocalDateTime)

import cats.instances
final class SimpleJobsExtractor[F[_]: Monad: Console: Daemonic[*[_], Throwable]](implicit KV: JobsInputKV[F, String]) {
  def jobs(id: JobId): F[Daemon0[F]] =
    for {
      maybeInput <- KV.read(id)
      input <- maybeInput match {
        case Some(value) => value.pure[F]
        case None        => F.unit
      }
      daemon <- F.daemonize(Console[F].putStr(s"hello, $input"))
    } yield daemon
}

final class SimpleTriggerInterpreter[F[_]: Applicative] extends TriggerInterpreter[F, F, SimpleTrigger] {
  override def next(triggerRepr: SimpleTrigger): F[LocalDateTime] = triggerRepr.date.pure[F]
}

object timer {
  implicit class TimerOps[F[_]](timer: Timer[F]) {
    def convert(ts: Long)           = LocalDateTime.ofInstant(Instant.ofEpochMilli(ts), ju.TimeZone.getDefault().toZoneId())
    def now(implicit G: Functor[F]) = timer.clock.realTime(TimeUnit.MILLISECONDS).map(convert)
  }
}

final class SimpleJobWatchmen[F[_]: Monad: Parallel: Timer: Daemonic[*[_], Throwable], G[_]: Traverse]
(implicit TriggerKV: TriggerAll[G, SimpleTrigger], jobsExtractor: SimpleJobsExtractor[F], jobf: G ~> F)
  extends TriggersWatchmen[F] {
  import timer._
  import scala.concurrent.duration._

  def diff(start: LocalDateTime, end: LocalDateTime): Long = {
    end.toInstant(ZoneOffset.UTC).toEpochMilli - start.toInstant(ZoneOffset.UTC).toEpochMilli()
  }

  def start: tofu.concurrent.Daemon0[F] = F.daemonize{
    for{
      _ <- jobsExtractor.jobs()
    } yield ()


    f(KV.all.parTraverse[F, Unit]{
    case (triggerId, SimpleTrigger(date)) => 
      jobsExtractor.jobs()
      for{
        now <- Timer[F].now
        duration = diff(now, date)
        _   <- Timer[F].sleep(duration millis)
      } yield ()
    })
  }
}
