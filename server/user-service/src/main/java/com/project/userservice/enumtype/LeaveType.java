package com.project.userservice.enumtype;

public enum LeaveType {
    ANNUAL(true),
    SICK(true),
    UNPAID(false),
    OTHER(false);

    private final boolean paid;

    LeaveType(boolean paid) {
        this.paid = paid;
    }

    public boolean isPaid() {
        return paid;
    }
}
