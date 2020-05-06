package dev.safronu.scheduler4s

import tofu.concurrent.Daemon0
import java.time.LocalDateTime

trait TriggerInterpreter[F[_], A] {
  def next(triggerRepr: A)  : F[LocalDateTime]
  def isLast(triggerRepr: A): F[Boolean]
}