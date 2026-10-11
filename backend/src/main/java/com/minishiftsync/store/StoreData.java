package com.minishiftsync.store;

import java.util.List;

import com.minishiftsync.model.Employee;
import com.minishiftsync.model.Shift;

public record StoreData(List<Employee> employees, List<Shift> shifts) {
}
