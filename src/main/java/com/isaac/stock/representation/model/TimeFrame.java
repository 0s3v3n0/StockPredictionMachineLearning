package com.isaac.stock.representation.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;


import java.time.Instant;
import java.time.LocalDate;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties
public class TimeFrame{
	@JsonFormat(pattern = "MM-dd-yyyy")
	private LocalDate date;
	private int volume;
	private Double high;
	private Instant date_utc;
	private Double low;
	private Double close;
	private Double open;


}
