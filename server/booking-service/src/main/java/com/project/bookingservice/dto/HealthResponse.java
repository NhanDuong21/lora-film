package com.project.bookingservice.dto;

public class HealthResponse {

	private String service;
	private String status;

	public HealthResponse() {
	}

	public HealthResponse(String service, String status) {
		this.service = service;
		this.status = status;
	}

	public String getService() {
		return service;
	}

	public void setService(String service) {
		this.service = service;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}
}
