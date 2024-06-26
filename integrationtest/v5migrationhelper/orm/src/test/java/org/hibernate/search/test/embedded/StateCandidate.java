/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.test.embedded;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;

/**
 *
 * @author Hardy Ferentschik
 */
@Entity
@Indexed
public class StateCandidate implements Person {

	@Id
	@GeneratedValue
	@DocumentId
	private int id;

	@Field
	private String name;

	@OneToOne(cascade = CascadeType.ALL)
	private Address address;

	@IndexedEmbedded
	@OneToOne(cascade = CascadeType.ALL)
	private State state;

	public State getState() {
		return state;
	}

	public void setState(State state) {
		this.state = state;
	}

	@Override
	public Address getAddress() {
		return address;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setAddress(Address address) {
		this.address = address;

	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}
}
