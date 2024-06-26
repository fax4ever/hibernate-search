/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.query.spi;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;

import org.hibernate.search.engine.logging.impl.Log;
import org.hibernate.search.engine.search.common.spi.MapNamedValues;
import org.hibernate.search.util.common.annotation.Incubating;
import org.hibernate.search.util.common.impl.Contracts;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

@Incubating
public class QueryParameters extends MapNamedValues {
	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	public QueryParameters() {
		super( new HashMap<>(), log::cannotFindQueryParameter, log::unexpectedQueryParameterType );
	}

	public void add(String parameter, Object value) {
		Contracts.assertNotNullNorEmpty( parameter, "parameter" );
		values.put( parameter, value );
	}
}
