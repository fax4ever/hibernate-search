/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.identifiertovalue.impl;

import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.engine.search.common.NamedValues;
import org.hibernate.search.mapper.pojo.bridge.IdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.IdentifierBindingContext;
import org.hibernate.search.mapper.pojo.bridge.binding.ValueBindingContext;
import org.hibernate.search.mapper.pojo.model.PojoModelValue;

final class ValueBindingContextToIdentifierBindingContextAdapter<I> implements IdentifierBindingContext<I> {
	private final ValueBindingContext<I> delegate;

	public ValueBindingContextToIdentifierBindingContextAdapter(ValueBindingContext<I> delegate) {
		this.delegate = delegate;
	}

	@Override
	public <I2> void bridge(Class<I2> expectedIdentifierType, IdentifierBridge<I2> bridge) {
		delegate.bridge( expectedIdentifierType, new IdentifierBridgeToValueBridgeAdapter<>( bridge ),
				delegate.typeFactory().asString() );
	}

	@Override
	public <I2> void bridge(Class<I2> expectedIdentifierType, BeanHolder<? extends IdentifierBridge<I2>> bridgeHolder) {
		delegate.bridge( expectedIdentifierType,
				BeanHolder.of( new IdentifierBridgeToValueBridgeAdapter<>( bridgeHolder.get() ) )
						.withDependencyAutoClosing( bridgeHolder ),
				delegate.typeFactory().asString() );
	}

	@Override
	public PojoModelValue<I> bridgedElement() {
		return delegate.bridgedElement();
	}

	@Override
	public NamedValues params() {
		return delegate.params();
	}

	@Override
	public BeanResolver beanResolver() {
		return delegate.beanResolver();
	}
}
