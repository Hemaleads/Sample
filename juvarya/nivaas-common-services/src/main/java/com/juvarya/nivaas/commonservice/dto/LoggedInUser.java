package com.juvarya.nivaas.commonservice.dto;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
@Data
public class LoggedInUser {
	private Long id;

	private String email;

	private String password;

	private String fullName;

	private Set<String> roles = new HashSet<>();

	private String primaryContact;

	private String gender;

	private Long postalCode;

	private MediaDTO media;

	private boolean newUser;

	private String profilePicture;
	
	private String fcmToken;

	private int version;

	private List<OnboardingRequestDTO> apartmentDTOs;

	private List<OnboardingRequestDTO> flatDTO;

	

}
