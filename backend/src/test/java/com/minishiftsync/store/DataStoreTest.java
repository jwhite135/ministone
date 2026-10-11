package com.minishiftsync.store;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.minishiftsync.model.Employee;
import com.minishiftsync.model.Shift;

import tools.jackson.databind.json.JsonMapper;

class DataStoreTest {

	private static final LocalDate TODAY = LocalDate.of(2026, 10, 14); // a Wednesday

	private final JsonMapper jsonMapper = JsonMapper.builder().build();

	@TempDir
	Path tempDir;

	@Test
	void seedsSampleDataWhenFileIsMissing() {
		Path file = tempDir.resolve("data.json");

		DataStore store = new DataStore(jsonMapper, file, TODAY);

		assertThat(file).exists();
		assertThat(store.findAllEmployees()).hasSize(4);
		// Sample shifts fall in the current Monday-to-Sunday week.
		assertThat(store.findAllShifts()).allSatisfy(s -> assertThat(s.date())
				.isBetween(LocalDate.of(2026, 10, 12), LocalDate.of(2026, 10, 18)));
	}

	@Test
	void dataSurvivesRestart() {
		Path file = tempDir.resolve("data.json");
		DataStore first = new DataStore(jsonMapper, file, TODAY);
		Employee added = first.addEmployee("Test Person", "Host", new BigDecimal("16.00"));
		Shift shift = first.addShift(added.id(), TODAY, LocalTime.of(22, 0), LocalTime.of(2, 0));

		// A new DataStore reading the same file simulates restarting the server.
		DataStore second = new DataStore(jsonMapper, file, TODAY);

		assertThat(second.findEmployee(added.id())).contains(added);
		assertThat(second.findAllShifts()).contains(shift);
	}

	@Test
	void editsAndDeletesSurviveRestart() {
		Path file = tempDir.resolve("data.json");
		DataStore first = new DataStore(jsonMapper, file, TODAY);
		Shift toEdit = first.findAllShifts().get(0);
		Shift toDelete = first.findAllShifts().get(1);
		Shift edited = first.updateShift(toEdit.id(), toEdit.employeeId(), toEdit.date(), LocalTime.of(7, 0),
				LocalTime.of(8, 0)).orElseThrow();
		first.deleteShift(toDelete.id());

		DataStore second = new DataStore(jsonMapper, file, TODAY);

		assertThat(second.findAllShifts()).contains(edited).doesNotContain(toEdit, toDelete);
	}

	@Test
	void idsContinueAfterRestart() {
		Path file = tempDir.resolve("data.json");
		new DataStore(jsonMapper, file, TODAY);

		DataStore reloaded = new DataStore(jsonMapper, file, TODAY);
		Employee added = reloaded.addEmployee("New Hire", "Server", new BigDecimal("15.00"));

		assertThat(added.id()).isEqualTo(5); // sample data uses ids 1-4
	}

	@Test
	void refusesToStartWithCorruptFile() throws IOException {
		Path file = tempDir.resolve("data.json");
		Files.writeString(file, "{ this is not json");

		assertThatThrownBy(() -> new DataStore(jsonMapper, file, TODAY))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("Could not read data file");
		assertThat(file).hasContent("{ this is not json"); // left untouched
	}

}
