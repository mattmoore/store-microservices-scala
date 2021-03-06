package io.mattmoore.store.algebras

import java.util.UUID

trait ServiceAlgebra[F[_], A] {
  def get(id: UUID): F[A]
  def add(a: A): F[UUID]
  def update(a: A): F[UUID]
}
