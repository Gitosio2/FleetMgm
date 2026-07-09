package com.fleetmgm.workshop.domain;

import com.fleetmgm.shared.exception.BadRequestException;

public enum ScheduleRange {
    TODAY,
    WEEK,
    MONTH;

    public static ScheduleRange fromValue(String value) {
        if (value == null) {
            throw new BadRequestException("INVALID_RANGE", "range is required — must be one of: today, week, month");
        }
        try {
            return ScheduleRange.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("INVALID_RANGE",
                    "range '" + value + "' is invalid — must be one of: today, week, month");
        }
    }
}
