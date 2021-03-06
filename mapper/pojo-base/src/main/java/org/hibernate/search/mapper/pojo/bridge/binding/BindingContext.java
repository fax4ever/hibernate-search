/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.binding;

import java.util.Optional;

import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.util.common.SearchException;

public interface BindingContext {

	/**
	 * @return A bean provider, allowing the retrieval of beans,
	 * including CDI/Spring DI beans when in the appropriate environment.
	 */
	BeanResolver beanResolver();

	/**
	 * @param name The name of the param
	 * @return Get a param defined for the binder by the given name
	 * @throws SearchException if it does not exist a param having such name
	 */
	Object param(String name);

	/**
	 * @param name The name of the param
	 * @return Get an optional param defined for the binder by the given name,
	 * a param having such name may either exist or not.
	 */
	Optional<Object> paramOptional(String name);

}
