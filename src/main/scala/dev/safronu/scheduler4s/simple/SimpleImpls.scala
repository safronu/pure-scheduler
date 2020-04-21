package dev.safronu.scheduler4s.simple

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
import dev.safronu.scheduler4s.syntax._
import java.util.concurrent.ConcurrentHashMap
import scala.jdk.CollectionConverters


case class SimpleTrigger(date: LocalDateTime)

final class SimpleJobsExtractor[F[_]: Monad: KeyValue[*[_], JobId, String]: KeyValue[*[_], TriggerId, JobId]: Raise[*[_], Exception]: Console: Daemonic[*[_], Throwable]] {
  def jobs(id: TriggerId): F[F[Unit]] =
    for {
      jobId    <- F.get[F, TriggerId, JobId](id).orThrow(new Exception(s"There is no such trigger: $id"))
      jobInput <- F.get[F, JobId, String](jobId).orThrow(new Exception(s"There is no such job: $id"))
    } yield Console[F].putStrLn(s"hello, $jobInput")
}



