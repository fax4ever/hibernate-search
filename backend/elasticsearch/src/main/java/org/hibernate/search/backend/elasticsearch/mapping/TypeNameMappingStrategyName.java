/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.mapping;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.engine.cfg.spi.ParseUtils;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public enum TypeNameMappingStrategyName {

	/**
	 * Rely on the "_index" meta-field.
	 * <p>
	 * Does not work with index aliases.
	 */
	INDEX_NAME( "index-name" ),
	/**
	 * Rely on a discriminator field added to each document to resolve the type name.
	 * <p>
	 * Works correctly with index aliases.
	 */
	DISCRIMINATOR( "discriminator" );

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	// This method conforms to the MicroProfile Config specification. Do not change its signature.
	public static TypeNameMappingStrategyName of(String value) {
		return ParseUtils.parseDiscreteValues(
				TypeNameMappingStrategyName.values(),
				TypeNameMappingStrategyName::externalRepresentation,
				log::invalidTypeNameMappingStrategyName,
				value
		);
	}

	private final String externalRepresentation;

	TypeNameMappingStrategyName(String externalRepresentation) {
		this.externalRepresentation = externalRepresentation;
	}

	/**
	 * @return The expected string representation in configuration properties.
	 */
	private String externalRepresentation() {
		return externalRepresentation;
	}
}
