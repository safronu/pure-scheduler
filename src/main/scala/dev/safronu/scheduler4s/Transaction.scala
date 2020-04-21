package dev.safronu.scheduler4s

trait Transaction[F[_]]{
  def transaction(f: F[Unit]): F[Unit]
}