package dev.safronu.scheduler4s

import cats._
import cats.syntax._
import cats.implicits._
import cats.data.Kleisli
import io.circe.Json

object aliases{
  import model.JobId
  type JobRegister[F[_], G[_]] = 
    KeyValueModify[F, JobId, Kleisli[G, Json, Unit]] 
    with KeyValueRead[F, List, JobId, Kleisli[G, Json, Unit]]
  type KeyValue[F[_], G[_], A, B] = KeyValueRead[F, G, A, B] with KeyValueModify[F, A, B]
}