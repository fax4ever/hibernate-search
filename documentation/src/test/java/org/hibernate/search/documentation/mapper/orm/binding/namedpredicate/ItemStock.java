/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.mapper.orm.binding.namedpredicate;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.PropertyBinderRef;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.PropertyBinding;

// tag::include[]
@Entity
@Indexed
public class ItemStock {

	@Id
	@PropertyBinding(binder = @PropertyBinderRef(type = SkuIdentifierBinder.class)) // <1>
	private String skuId;

	private int amountInStock;

	// Getters and setters
	// ...

	// tag::getters-setters[]
	public String getSkuId() {
		return skuId;
	}

	public void setSkuId(String skuId) {
		this.skuId = skuId;
	}

	public int getAmountInStock() {
		return amountInStock;
	}

	public void setAmountInStock(int amountInStock) {
		this.amountInStock = amountInStock;
	}
	// end::getters-setters[]

}
// end::include[]
