package com.buildingos.building.buildingapplication.application.port.out;

public enum CreationFeeStatus {
    SETTLED, UNPAID, NOT_REQUIRED;

    public boolean allowsApproval() { return this != UNPAID; }
}
