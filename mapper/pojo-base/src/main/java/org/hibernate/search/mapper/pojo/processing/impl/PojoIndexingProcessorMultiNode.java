/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.processing.impl;

import java.util.Collection;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.mapper.pojo.processing.spi.PojoIndexingProcessorRootContext;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.spi.ToStringTreeAppender;

/**
 * A node inside a {@link PojoIndexingProcessor} responsible for applying multiple processors.
 *
 * @param <T> The processed type
 */
public class PojoIndexingProcessorMultiNode<T> extends PojoIndexingProcessor<T> {

	private final Collection<? extends PojoIndexingProcessor<? super T>> elements;

	public PojoIndexingProcessorMultiNode(Collection<? extends PojoIndexingProcessor<? super T>> elements) {
		this.elements = elements;
	}

	@Override
	public void close() {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.pushAll( PojoIndexingProcessor::close, elements );
		}
	}

	@Override
	public void appendTo(ToStringTreeAppender appender) {
		appender.attribute( null, elements );
	}

	@Override
	public final void process(DocumentElement target, T source, PojoIndexingProcessorRootContext context) {
		for ( PojoIndexingProcessor<? super T> element : elements ) {
			// Recursion here
			element.process( target, source, context );
		}
	}

}
