package dev.safronu.scheduler4s.simple

import cats.Applicative
import java.time.LocalDateTime
import dev.safronu.scheduler4s.TriggerInterpreter
import cats.implicits._
import cats.syntax._

final class SimpleTriggerInterpreter[F[_]: Applicative] extends TriggerInterpreter[F, SimpleTrigger] {
  override def next(triggerRepr: SimpleTrigger): F[LocalDateTime] = triggerRepr.date.pure[F]
}
