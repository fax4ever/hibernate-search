/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.jakarta.batch.core.massindexing.util.impl;

import org.hibernate.search.mapper.orm.loading.spi.ConditionalExpression;

/**
 * Provides ID-based, order-sensitive restrictions
 * and ascending ID order for the indexed type,
 * allowing to easily build partitions based on ID order.
 *
 * @author Mincong Huang
 * @author Yoann Rodiere
 */
public interface IdOrder {

	/**
	 * @param paramNamePrefix A unique prefix for the name of parameters added by the resulting expression.
	 * @param idObj The ID all results should be lesser than.
	 * @return A "strictly greater than" restriction on the ID.
	 */
	ConditionalExpression idGreater(String paramNamePrefix, Object idObj);

	/**
	 * @param paramNamePrefix A unique prefix for the name of parameters added by the resulting expression.
	 * @param idObj The ID all results should be lesser than.
	 * @return A "greater or equal" restriction on the ID.
	 */
	ConditionalExpression idGreaterOrEqual(String paramNamePrefix, Object idObj);

	/**
	 * @param paramNamePrefix A unique prefix for the name of parameters added by the resulting expression.
	 * @param idObj The ID all results should be lesser than.
	 * @return A "lesser than" restriction on the ID.
	 */
	ConditionalExpression idLesser(String paramNamePrefix, Object idObj);

	String ascOrder();

}
