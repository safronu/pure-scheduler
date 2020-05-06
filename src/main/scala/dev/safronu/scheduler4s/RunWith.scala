package dev.safronu.scheduler4s

trait RunWith[F[_], A, B] {
  def run[C](f: F[C], input: A): F[B]
}
