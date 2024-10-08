package com.juvarya.nivaas.customer.request;

import javax.validation.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Getter;
import lombok.Setter;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
public class LoginRequest {
	@NotBlank
	private String primaryContact;

	@NotBlank
	private String otp;

	

}
