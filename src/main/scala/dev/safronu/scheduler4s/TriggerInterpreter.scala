package dev.safronu.scheduler4s

import tofu.concurrent.Daemon0
import java.time.LocalDateTime

trait TriggerInterpreter[F[_], G[_], A] {
  def jobs: F[List[Daemon0[G]]]
  def next(triggerRepr: A): F[LocalDateTime]
}