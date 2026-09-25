package com.buildingos.backoffice.supportsession.domain.model;

/**
 * Derived from {@code ended_at}: none = {@code ACTIVE}; ended by a user before {@code expires_at} = {@code ENDED};
 * ended automatically at {@code expires_at} = {@code EXPIRED}.
 */
public enum SupportSessionStatus { ACTIVE, ENDED, EXPIRED }
