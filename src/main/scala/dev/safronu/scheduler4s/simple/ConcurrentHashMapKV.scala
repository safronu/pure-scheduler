package dev.safronu.scheduler4s.simple

import dev.safronu.scheduler4s.KeyValueRead
import dev.safronu.scheduler4s.KeyValueModify
import java.util.concurrent.ConcurrentHashMap
import scala.collection.mutable
import cats.effect.Sync
import scala.jdk.CollectionConverters._

final class ConcurrentHashMapKV[F[_]: Sync, A, B] 
  extends KeyValueRead[F, Lambda[C => F[List[C]]], A, B] 
  with    KeyValueModify[F, A, B] {

  val map: mutable.Map[A, B] = (new ConcurrentHashMap()).asScala
  
  override def save(id: A, value: B): F[Unit] = Sync[F].delay(map.put(id, value))

  override def read(id: A): F[Option[B]] = Sync[F].delay(map.get(id))

  override def delete(id: A): F[Option[B]] = Sync[F].delay(map.remove(id))

  override def update(id: A, value: B): F[Unit] = Sync[F].delay(map.put(id, value))

  override def all: F[List[(A, B)]] = Sync[F].delay(map.toList)
}

