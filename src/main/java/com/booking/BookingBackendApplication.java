package com.booking;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.booking.mapper")
public class BookingBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(BookingBackendApplication.class, args);
	}

}
