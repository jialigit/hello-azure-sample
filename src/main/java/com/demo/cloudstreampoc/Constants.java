package com.demo.cloudstreampoc;

import org.springframework.beans.factory.annotation.Value;

public class Constants {
	
	@Value("${spring.cloud.stream.bindings.consume-in-0.destination}")
	public static String consumer;
	
	@Value("${spring.cloud.stream.bindings.consume-in-0.group}")
	public static String group;
	

}
