/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.backend.reporting.spi;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.util.common.logging.impl.MessageConstants;

import org.jboss.logging.Messages;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageBundle;

@MessageBundle(projectCode = MessageConstants.PROJECT_CODE)
public interface BackendSearchHints {

	BackendSearchHints NONE = Messages.getBundle( MethodHandles.lookup(), BackendSearchHints.class );

	@Message("An ID projection represents the document ID and adding it as a part of the nested object projection might produce misleading results "
			+ "since it is always a root document ID and not a nested object ID.")
	String idProjectionNestingNotSupportedHint();

	@Message("A document reference projection represents a root document and adding it as a part of the nested object projection might produce misleading results.")
	String documentReferenceProjectionNestingNotSupportedHint();

	@Message("An entity projection represents a root entity and adding it as a part of the nested object projection might produce misleading results.")
	String entityProjectionNestingNotSupportedHint();

	@Message("An entity reference projection represents a root entity and adding it as a part of the nested object projection might produce misleading results.")
	String entityReferenceProjectionNestingNotSupportedHint();

	@Message("An explanation projection describes the score computation for the hit and adding it as a part of the nested object projection might produce misleading results.")
	String explanationProjectionNestingNotSupportedHint();

	@Message("A score projection provides the score for the entire hit and adding it as a part of the nested object projection might produce misleading results.")
	String scoreProjectionNestingNotSupportedHint();
}
