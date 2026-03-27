package com.shipment.shippinggo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class ShippinggoApplication {

	public static void main(String[] args) {
		SpringApplication.run(ShippinggoApplication.class, args);
	}

}
