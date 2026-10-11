package com.minishiftsync.store;

import java.util.List;

import com.minishiftsync.model.Employee;
import com.minishiftsync.model.Shift;

/** The exact shape of the JSON data file: {"employees": [...], "shifts": [...]}. */
public record StoreData(List<Employee> employees, List<Shift> shifts) {
}
