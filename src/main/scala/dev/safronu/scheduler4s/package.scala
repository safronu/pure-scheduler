package dev.safronu

import tofu.concurrent.Daemon0
import java.time.LocalDateTime
import dev.safronu.model.TriggerId
import dev.safronu.model.JobId

package object scheduler4s {
  type JobDetailsKV[F[_], G[_], B] = KeyValue[F, JobId, B => Daemon0[G]]
  type TriggerKV[F[_], A]          = KeyValue[F, TriggerId, A]
  type JobTriggerLinkKV[F[_], A]   = KeyValue[F, (JobId, TriggerId), A]
}
