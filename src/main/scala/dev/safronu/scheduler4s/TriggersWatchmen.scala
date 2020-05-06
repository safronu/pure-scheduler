package dev.safronu.scheduler4s

import tofu.concurrent.Daemon0
import aliases.JobRegister

trait TriggersWatchmen[F[_], G[_]]{
  def start(jobRegister: JobRegister[F, G]): F[Daemon0[F]]
}
