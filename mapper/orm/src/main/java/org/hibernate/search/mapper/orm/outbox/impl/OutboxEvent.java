/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.outbox.impl;

import java.util.Arrays;
import java.util.Objects;
import javax.persistence.Transient;

public final class OutboxEvent {

	public enum Type {
		ADD, ADD_OR_UPDATE, DELETE
	}

	private Long id;
	private Type type;

	private String entityName;
	private String entityId;
	private byte[] payload;
	private int retries = 0;

	@Transient
	private Object originalEntityId;

	public OutboxEvent() {
	}

	public OutboxEvent(Type type, String entityName, String entityId, byte[] payload,
			Object originalEntityId) {
		this.type = type;
		this.entityName = entityName;
		this.entityId = entityId;
		this.payload = payload;
		this.originalEntityId = originalEntityId;
	}

	public OutboxEvent(Type type, String entityName, String entityId, byte[] payload, int retries) {
		this.type = type;
		this.entityName = entityName;
		this.entityId = entityId;
		this.payload = payload;
		this.retries = retries;
		this.originalEntityId = null;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}

	public String getEntityName() {
		return entityName;
	}

	public void setEntityName(String entityName) {
		this.entityName = entityName;
	}

	public String getEntityId() {
		return entityId;
	}

	public void setEntityId(String entityId) {
		this.entityId = entityId;
	}

	public byte[] getPayload() {
		return payload;
	}

	public void setPayload(byte[] payload) {
		this.payload = payload;
	}

	public int getRetries() {
		return retries;
	}

	public void setRetries(int retries) {
		this.retries = retries;
	}

	public Object getOriginalEntityId() {
		return originalEntityId;
	}

	public void setOriginalEntityId(Object originalEntityId) {
		this.originalEntityId = originalEntityId;
	}

	OutboxEventReference getReference() {
		return new OutboxEventReference( getEntityName(), getEntityId() );
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		OutboxEvent event = (OutboxEvent) o;
		return type == event.type && Objects.equals( entityName, event.entityName ) && Objects.equals(
				entityId, event.entityId ) && Arrays.equals( payload, event.payload );
	}

	@Override
	public int hashCode() {
		int result = Objects.hash( type, entityName, entityId );
		result = 31 * result + Arrays.hashCode( payload );
		return result;
	}

	@Override
	public String toString() {
		return "OutboxEvent{" +
				"id=" + id +
				", type=" + type +
				", entityName='" + entityName + '\'' +
				", entityId='" + entityId + '\'' +
				", retries=" + retries +
				", originalEntityId=" + originalEntityId +
				'}';
	}
}
