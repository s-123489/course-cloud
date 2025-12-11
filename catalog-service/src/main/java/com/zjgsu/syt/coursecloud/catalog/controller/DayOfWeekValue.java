package com.zjgsu.syt.coursecloud.catalog.controller;

import java.time.DayOfWeek;

public enum DayOfWeekValue {
    MONDAY,
    TUESDAY,
    WEDNESDAY,
    THURSDAY,
    FRIDAY,
    SATURDAY,
    SUNDAY;

    public DayOfWeek toDayOfWeek() {
        return DayOfWeek.valueOf(name());
    }
}
