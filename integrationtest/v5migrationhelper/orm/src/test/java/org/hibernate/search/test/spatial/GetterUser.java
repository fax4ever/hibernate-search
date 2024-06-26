/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.test.spatial;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Latitude;
import org.hibernate.search.annotations.Longitude;
import org.hibernate.search.annotations.Spatial;

@Spatial(name = "home")
@Entity
@Indexed
public class GetterUser {

	@Id
	Integer id;

	Double homeLatitude;

	Double homeLongitude;

	public GetterUser(Integer id, Double homeLatitude, Double homeLongitude) {
		this.id = id;
		this.homeLatitude = homeLatitude;
		this.homeLongitude = homeLongitude;
	}

	public GetterUser() {
	}

	@Latitude(of = "home")
	public Double getHomeLatitude() {
		return homeLatitude;
	}

	public void setHomeLatitude(Double homeLatitude) {
		this.homeLatitude = homeLatitude;
	}

	@Longitude(of = "home")
	public Double getHomeLongitude() {
		return homeLongitude;
	}

	public void setHomeLongitude(Double homeLongitude) {
		this.homeLongitude = homeLongitude;
	}

	public Integer getId() {
		return id;
	}
}
