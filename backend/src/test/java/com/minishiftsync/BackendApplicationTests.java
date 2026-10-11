package com.minishiftsync;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// Point the app at a throwaway file under target/ so tests never touch real data.
@SpringBootTest(properties = "minishiftsync.data-file=target/test-data/context-test.json")
class BackendApplicationTests {

	@Test
	void contextLoads() {
	}

}
