package io.mattmoore.store.product.domain

import java.util.UUID

case class Product(
  id: Option[UUID] = None,
  name: String,
  description: String,
  price: BigDecimal
)
