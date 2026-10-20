package com.buildingos.backoffice.platformuser.application.listplatformusers;

/** {@code query} matches phone; {@code role} filters to holders of one platform role; both optional. */
public record ListPlatformUsersQuery(String query, String role, int page, int size) {}
