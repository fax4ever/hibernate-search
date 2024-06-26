/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.common.extension;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.Set;

public class CloseScrollWorkCall extends Call<CloseScrollWorkCall> {

	private final Set<String> indexNames;

	public CloseScrollWorkCall(Set<String> indexNames) {
		this.indexNames = indexNames;
	}

	@Override
	protected String summary() {
		return "scroll.close() work execution on indexes '" + indexNames + "'";
	}

	public CallBehavior<Void> verify(CloseScrollWorkCall actualCall) {
		assertThat( actualCall.indexNames )
				.as( "CloseScroll work did not target the expected indexes: " )
				.isEqualTo( indexNames );

		return () -> null;
	}

	@Override
	protected boolean isSimilarTo(CloseScrollWorkCall other) {
		return Objects.equals( indexNames, other.indexNames );
	}
}
