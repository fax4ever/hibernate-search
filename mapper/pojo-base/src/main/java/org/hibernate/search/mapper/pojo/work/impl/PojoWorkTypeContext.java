/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.work.impl;

import java.util.Optional;
import java.util.function.Supplier;

import org.hibernate.search.mapper.pojo.automaticindexing.impl.PojoImplicitReindexingResolver;
import org.hibernate.search.mapper.pojo.automaticindexing.impl.PojoImplicitReindexingResolverRootContext;
import org.hibernate.search.mapper.pojo.automaticindexing.impl.PojoReindexingCollector;
import org.hibernate.search.mapper.pojo.identity.impl.IdentifierMappingImplementor;
import org.hibernate.search.mapper.pojo.loading.spi.PojoLoadingTypeContext;
import org.hibernate.search.mapper.pojo.model.path.impl.PojoPathOrdinals;
import org.hibernate.search.mapper.pojo.work.spi.PojoWorkSessionContext;

/**
 * @param <I> The identifier type for the entity type.
 * @param <E> The entity type.
 */
public interface PojoWorkTypeContext<I, E> extends PojoLoadingTypeContext<E> {

	Optional<? extends PojoWorkIndexedTypeContext<I, E>> asIndexed();

	Optional<? extends PojoWorkContainedTypeContext<I, E>> asContained();

	IdentifierMappingImplementor<I, E> identifierMapping();

	String toDocumentIdentifier(PojoWorkSessionContext sessionContext, I identifier);

	PojoImplicitReindexingResolver<E> reindexingResolver();

	E toEntity(Object unproxiedEntity);

	Supplier<E> toEntitySupplier(PojoWorkSessionContext sessionContext, Object entity);

	void resolveEntitiesToReindex(PojoReindexingCollector collector,
			PojoWorkSessionContext sessionContext, Object identifier,
			Supplier<E> entitySupplier,
			PojoImplicitReindexingResolverRootContext context);

	PojoPathOrdinals pathOrdinals();

}
