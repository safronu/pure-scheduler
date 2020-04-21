package dev.safronu.scheduler4s

import tofu.Raise
import cats.effect.Sync
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.Uuid
import java.{util => ju}

final class FUUIDImpl[F[_]: Sync] extends FUUID[F] {
  def random: F[String Refined Uuid] = {
    F.delay(Refined.unsafeApply(ju.UUID.randomUUID().toString))
  }
}
