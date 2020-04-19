package dev.safronu

import tofu.concurrent.Daemon0
import java.time.LocalDateTime
import dev.safronu.scheduler4s.model.TriggerId
import dev.safronu.scheduler4s.model.JobId

package object scheduler4s {
  type JobsKV[F[_], G[_], B]          = KeyValue[F, JobId, B => Daemon0[G]]
  type JobsInputKV[F[_], B]           = KeyValue[F, JobId, B]
  type TriggerAll[F[_], G[_], A]      = KeyValueS[Lambda[A => F[G[A]]], TriggerId, A]
  type TriggerModifyKV[F[_], G[_], A] = KeyValue[F, TriggerId, A] with KeyValueS[Lambda[A => F[G[A]]], TriggerId, A]
  type JobTriggerLinkKV[F[_]]         = KeyValue[F, TriggerId, JobId]
}
