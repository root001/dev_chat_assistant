package com.digital.creo.AIntegrationDemo.dto;

import lombok.Builder;
import org.springframework.http.HttpStatusCode;

@Builder
public record ChatResponse(HttpStatusCode code, String response, String msg) {
}
