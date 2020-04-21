package dev.safronu.scheduler4s

import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.Uuid
import simulacrum._
import cats.effect.Sync

@typeclass trait FUUID[F[_]] {
  def random: F[String Refined Uuid]
}

object FUUID{
    implicit def fuuid[F[_]: Sync] = new FUUIDImpl[F]
}