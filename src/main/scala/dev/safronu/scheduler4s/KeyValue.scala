package dev.safronu.scheduler4s

trait KeyValue[F[_], A, B] {
  def save(id: A, value: B): F[Unit]
  def read(id: A): F[Option[B]]
  def delete(id: A): F[Option[B]]
  def update(id: A, value: B): F[Unit]
}

trait KeyValueS[G[_], A, B]{
  def all: G[(A, B)] 
}