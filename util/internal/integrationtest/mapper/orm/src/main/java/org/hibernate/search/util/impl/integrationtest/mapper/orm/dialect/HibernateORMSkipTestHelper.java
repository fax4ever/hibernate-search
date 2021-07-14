/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.mapper.orm.dialect;

import static org.junit.Assume.assumeFalse;

import org.hibernate.SessionFactory;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;

public final class HibernateORMSkipTestHelper {

	private HibernateORMSkipTestHelper() {
	}

	public static void skipForDialect(SessionFactory sessionFactory, Class<? extends Dialect> dialect, String message) {
		Dialect currentDialect = sessionFactory.unwrap( SessionFactoryImplementor.class )
				.getJdbcServices().getDialect();

		assumeFalse( message, dialect.isAssignableFrom( currentDialect.getClass() ) );
	}
}
