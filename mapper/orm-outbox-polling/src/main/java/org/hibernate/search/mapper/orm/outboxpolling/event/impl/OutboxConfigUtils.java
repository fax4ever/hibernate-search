/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.outboxpolling.event.impl;

import java.lang.invoke.MethodHandles;
import java.time.Duration;

import org.hibernate.search.mapper.orm.outboxpolling.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

final class OutboxConfigUtils {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private OutboxConfigUtils() {
	}

	static Duration checkPulseInterval(Duration pulseInterval, Duration pollingInterval) {
		if ( pulseInterval.compareTo( pollingInterval ) < 0 ) {
			throw log.invalidPollingIntervalAndPulseInterval( pollingInterval.toMillis() );
		}
		return pulseInterval;
	}

	static Duration checkPulseExpiration(Duration pulseExpiration, Duration pulseInterval) {
		Duration pulseIntervalTimes3 = pulseInterval.multipliedBy( 3 );
		if ( pulseExpiration.compareTo( pulseIntervalTimes3 ) < 0 ) {
			throw log.invalidPulseIntervalAndPulseExpiration( pulseIntervalTimes3.toMillis() );
		}
		return pulseExpiration;
	}
}
