package com.minishiftsync.store;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.minishiftsync.model.Employee;
import com.minishiftsync.model.Shift;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/** In-memory data mirrored to a JSON file. Synchronized because requests run on many threads. */
@Component
public class DataStore {

	private static final Logger log = LoggerFactory.getLogger(DataStore.class);

	private final JsonMapper jsonMapper;
	private final Path dataFile;
	private final List<Employee> employees = new ArrayList<>();
	private final List<Shift> shifts = new ArrayList<>();
	private long nextEmployeeId;
	private long nextShiftId;

	@Autowired
	public DataStore(JsonMapper jsonMapper, @Value("${minishiftsync.data-file}") String dataFile) {
		this(jsonMapper, Path.of(dataFile), LocalDate.now());
	}

	public DataStore(JsonMapper jsonMapper, Path dataFile, LocalDate today) {
		this.jsonMapper = jsonMapper;
		this.dataFile = dataFile.toAbsolutePath();
		if (Files.exists(this.dataFile)) {
			load();
			log.info("Loaded {} employees and {} shifts from {}", employees.size(), shifts.size(), this.dataFile);
		}
		else {
			replaceContents(SampleData.create(today));
			save();
			log.info("No data file found; wrote sample data to {}", this.dataFile);
		}
	}

	public synchronized List<Employee> findAllEmployees() {
		return List.copyOf(employees);
	}

	public synchronized Optional<Employee> findEmployee(long id) {
		return employees.stream().filter(e -> e.id() == id).findFirst();
	}

	public synchronized Employee addEmployee(String name, String position, BigDecimal hourlyRate) {
		Employee employee = new Employee(nextEmployeeId, name, position, hourlyRate);
		employees.add(employee);
		saveOrUndo(() -> employees.remove(employee));
		nextEmployeeId++;
		return employee;
	}

	public synchronized List<Shift> findAllShifts() {
		return List.copyOf(shifts);
	}

	public synchronized Optional<Shift> findShift(long id) {
		return shifts.stream().filter(s -> s.id() == id).findFirst();
	}

	public synchronized Shift addShift(long employeeId, LocalDate date, LocalTime startTime, LocalTime endTime) {
		Shift shift = new Shift(nextShiftId, employeeId, date, startTime, endTime);
		shifts.add(shift);
		saveOrUndo(() -> shifts.remove(shift));
		nextShiftId++;
		return shift;
	}

	public synchronized Optional<Shift> updateShift(long id, long employeeId, LocalDate date, LocalTime startTime,
			LocalTime endTime) {
		for (int i = 0; i < shifts.size(); i++) {
			Shift old = shifts.get(i);
			if (old.id() == id) {
				Shift updated = new Shift(id, employeeId, date, startTime, endTime);
				int index = i;
				shifts.set(index, updated);
				saveOrUndo(() -> shifts.set(index, old));
				return Optional.of(updated);
			}
		}
		return Optional.empty();
	}

	public synchronized boolean deleteShift(long id) {
		for (int i = 0; i < shifts.size(); i++) {
			if (shifts.get(i).id() == id) {
				Shift removed = shifts.remove(i);
				int index = i;
				saveOrUndo(() -> shifts.add(index, removed));
				return true;
			}
		}
		return false;
	}

	private void load() {
		try {
			StoreData data = jsonMapper.readValue(Files.readAllBytes(dataFile), StoreData.class);
			replaceContents(data);
		}
		catch (IOException | JacksonException ex) {
			throw new IllegalStateException("Could not read data file " + dataFile
					+ ". Fix the JSON or delete the file to start over with sample data.", ex);
		}
	}

	private void replaceContents(StoreData data) {
		employees.clear();
		shifts.clear();
		if (data.employees() != null) {
			employees.addAll(data.employees());
		}
		if (data.shifts() != null) {
			shifts.addAll(data.shifts());
		}
		nextEmployeeId = employees.stream().mapToLong(Employee::id).max().orElse(0) + 1;
		nextShiftId = shifts.stream().mapToLong(Shift::id).max().orElse(0) + 1;
	}

	private void saveOrUndo(Runnable undo) {
		try {
			save();
		}
		catch (RuntimeException ex) {
			undo.run();
			throw ex;
		}
	}

	/** Write to a temp file and rename it, so a crash can't leave a half-written data file. */
	private void save() {
		Path tempFile = dataFile.resolveSibling(dataFile.getFileName() + ".tmp");
		try {
			Files.createDirectories(dataFile.getParent());
			try (OutputStream out = Files.newOutputStream(tempFile)) {
				jsonMapper.writerWithDefaultPrettyPrinter().writeValue(out, new StoreData(employees, shifts));
			}
			Files.move(tempFile, dataFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		}
		catch (IOException ex) {
			throw new UncheckedIOException("Could not save data file " + dataFile, ex);
		}
	}

}
