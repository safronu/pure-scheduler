package dev.safronu

import io.estatico.newtype.macros._
import eu.timepit.refined._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.Uuid

object model{
    @newtype case class JobId(value: String Refined Uuid)
    @newtype case class TriggerId(value: String Refined Uuid)
}