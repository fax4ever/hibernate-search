/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.search.projection.impl;

import java.util.Objects;

import org.hibernate.search.engine.spatial.GeoPoint;

public class DistanceSortKey {

	private final String absoluteFieldPath;

	private final GeoPoint location;

	public DistanceSortKey(String absoluteFieldPath, GeoPoint location) {
		this.absoluteFieldPath = absoluteFieldPath;
		this.location = location;
	}

	@Override
	public boolean equals(Object obj) {
		if ( obj == this ) {
			return true;
		}
		if ( !( obj instanceof DistanceSortKey ) ) {
			return false;
		}

		DistanceSortKey other = (DistanceSortKey) obj;

		return Objects.equals( this.absoluteFieldPath, other.absoluteFieldPath )
				&& Objects.equals( this.location, other.location );
	}

	@Override
	public int hashCode() {
		return Objects.hash( absoluteFieldPath, location );
	}
}
