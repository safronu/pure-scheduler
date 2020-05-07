package dev.safronu.scheduler4s

import io.estatico.newtype.Coercible
import cats.Functor
import java.time.LocalDateTime
import java.time.Instant
import java.{util => ju}
import cats.effect.Timer
import cats.syntax.functor._
import io.estatico.newtype.ops._
import java.util.concurrent.TimeUnit

object syntax {
  implicit class TimerOps[F[_]](timer: Timer[F]) {
    def convert(ts: Long)           = LocalDateTime.ofInstant(Instant.ofEpochMilli(ts), ju.TimeZone.getDefault().toZoneId())
    def now(implicit G: Functor[F]) = timer.clock.realTime(TimeUnit.MILLISECONDS).map(convert)
  }

  implicit class FUUIDSyntax[F[_], A](f: F[A]) {
    def coerce[B: Coercible[A, *]](implicit F: Functor[F]): F[B] = f.map(_.coerce[B])
  }

  def const[A, B](b: => B): A => B = _ => b
}

