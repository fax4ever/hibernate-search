/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.impl;

import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.common.tree.spi.TreeNodeInclusion;

public class StubIndexFieldReference<F> implements IndexFieldReference<F> {

	private final String absolutePath;
	private final String relativeFieldName;
	private final TreeNodeInclusion inclusion;

	public StubIndexFieldReference(String absolutePath, String relativeFieldName, TreeNodeInclusion inclusion) {
		this.absolutePath = absolutePath;
		this.relativeFieldName = relativeFieldName;
		this.inclusion = inclusion;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + absolutePath + "]";
	}

	public TreeNodeInclusion getInclusion() {
		return inclusion;
	}

	String getAbsolutePath() {
		return absolutePath;
	}

	String getRelativeFieldName() {
		return relativeFieldName;
	}
}
