package com.xiaohua;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Random;

@SpringBootTest
class MemoryBackApplicationTests {

	@Test
	void contextLoads() {
		Random r = new Random();
		System.out.println(r.nextInt(10));
	}

}
