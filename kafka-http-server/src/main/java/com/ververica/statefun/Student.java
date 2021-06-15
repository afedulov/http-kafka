package com.ververica.statefun;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class Student {
	private String registrationNumber;
	private String name;
	private String grade;
}
