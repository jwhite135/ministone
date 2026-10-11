package com.minishiftsync.model;

import java.math.BigDecimal;

public record Employee(long id, String name, String position, BigDecimal hourlyRate) {
}
