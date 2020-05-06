package dev.safronu.scheduler4s


trait KeyValueRead[F[_], G[_], A, B]{
  def read(id: A): F[Option[B]]
  def all: G[(A, B)]
}

trait KeyValueModify[F[_], A, B] {
  def save(id: A, value: B): F[Unit]
  def delete(id: A): F[Option[B]]
  def update(id: A, value: B): F[Unit]
}