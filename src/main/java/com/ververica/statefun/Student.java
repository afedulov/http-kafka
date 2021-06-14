package com.ververica.statefun;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data(staticConstructor = "of")
public class Student {
	private final String registrationNumber;
	private final String name;
	private final String grade;
}
