package dev.safronu

import tofu.concurrent.Daemon0
import java.time.LocalDateTime
import dev.safronu.scheduler4s.model.TriggerId
import dev.safronu.scheduler4s.model.JobId

package object scheduler4s {
  type JobsKV[F[_], G[_], B]     = KeyValue[F, JobId, B => Daemon0[G]]
  type JobsInputKV[F[_], B]      = KeyValue[F, JobId, B]
  type TriggerAll[F[_], A]       = KeyValueS[F, TriggerId, A]
  type TriggerModifyKV[F[_], A]  = KeyValue[F, TriggerId, A] with KeyValueS[F, TriggerId, A]
  type JobTriggerLinkKV[F[_], A] = KeyValue[F, (JobId, TriggerId), A]
}
