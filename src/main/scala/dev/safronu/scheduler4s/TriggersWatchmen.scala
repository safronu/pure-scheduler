package dev.safronu.scheduler4s

import tofu.concurrent.Daemon0

trait TriggersWatchmen[F[_]]{
  def start: F[Daemon0[F]]
}
