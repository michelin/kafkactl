package com.michelin.kafkactl.models;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.Date;
import java.util.Map;

@Getter
@Setter
@Builder
@ToString
@ReflectiveAccess
@NoArgsConstructor
@AllArgsConstructor
public class ObjectMeta {
	private String name;
	private String namespace;
	private String cluster;
	private Map<String,String> labels;
	@JsonFormat(shape = JsonFormat.Shape.STRING)
	private Date creationTimestamp;
}
