/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.backend.lucene;

import java.util.LinkedHashMap;
import java.util.Map;

import org.hibernate.search.util.impl.integrationtest.common.extension.BackendConfiguration;

public class LuceneBackendConfiguration extends BackendConfiguration {
	@Override
	public String toString() {
		return "lucene";
	}

	@Override
	public Map<String, String> rawBackendProperties() {
		Map<String, String> properties = new LinkedHashMap<>();
		properties.put(
				"directory.root",
				LuceneTestIndexesPathConfiguration.get().getPath()
						+ "/test-indexes/#{test.startup.timestamp}/#{test.id}/"
		);
		return properties;
	}

	@Override
	public boolean supportsExplicitPurge() {
		return true;
	}

	@Override
	public boolean supportsExplicitRefresh() {
		return true;
	}

}
