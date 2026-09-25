package com.buildingos.backoffice.systemhealth.domain.model;

/** One service's readiness: UP (2xx), DOWN (answered non-2xx), UNKNOWN (unreachable or timed out). */
public enum ServiceHealthStatus { UP, DOWN, UNKNOWN }
