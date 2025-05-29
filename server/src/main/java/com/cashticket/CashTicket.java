package com.cashticket;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CashTicket {
	public static void main(String[] args) {
		SpringApplication.run(CashTicket.class, args);
	}
}