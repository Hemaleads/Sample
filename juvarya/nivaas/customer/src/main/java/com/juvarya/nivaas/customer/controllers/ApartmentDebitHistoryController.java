package com.juvarya.nivaas.customer.controllers;

import java.util.List;
import java.util.Objects;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

import com.juvarya.nivaas.utils.SecurityUtils;
import com.juvarya.nivaas.commonservice.user.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.juvarya.nivaas.customer.dto.ApartmentDebitDto;
import com.juvarya.nivaas.customer.dto.MessageDTO;
import com.juvarya.nivaas.customer.model.ApartmentDebitHistoryModel;
import com.juvarya.nivaas.customer.model.NivaasApartmentModel;
import com.juvarya.nivaas.customer.service.ApartmentDebitHistoryService;
import com.juvarya.nivaas.customer.service.NivaasApartmentService;
import com.juvarya.nivaas.customer.util.UserRoleHelper;
import com.juvarya.nivaas.utils.NivaasConstants;
import com.juvarya.nivaas.utils.converter.JTBaseEndpoint;

import lombok.extern.slf4j.Slf4j;

@SuppressWarnings("rawtypes")
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/apartment/debit-history")
@Slf4j
public class ApartmentDebitHistoryController extends JTBaseEndpoint {

	@Autowired
	private ApartmentDebitHistoryService service;

	@Autowired
	private UserRoleHelper userRoleHelper;

	@Autowired
	private NivaasApartmentService apartmentService;

	@PostMapping
	@PreAuthorize(NivaasConstants.ROLE_APARTMENT_ADMIN)
	public ResponseEntity addDebitHistory(@RequestBody @Valid ApartmentDebitDto debitHistory) {
		UserDetailsImpl user = SecurityUtils.getCurrentUserDetails();
		log.info("Received request to add debit history: {}", debitHistory);
		NivaasApartmentModel nivaasApartmentModel = apartmentService.findById(debitHistory.getApartmentId());
		if (Objects.isNull(nivaasApartmentModel)
				|| !userRoleHelper.isValidApartmentAdmin(user.getId(), nivaasApartmentModel)) {
			log.warn("Unauthorized access or invalid apartment for user: {}", user.getPrimaryContact());

			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}
		ApartmentDebitHistoryModel savedDebitHistory = service.addDebitHistory(debitHistory, nivaasApartmentModel,
				user.getId());
		log.info("Debit history added successfully for apartmentId: {} by user: {}", debitHistory.getApartmentId(),
				user.getId());

		return ResponseEntity.status(201).body(savedDebitHistory);
	}

	@PutMapping("/{id}")
	@PreAuthorize(NivaasConstants.ROLE_APARTMENT_ADMIN)
	public ResponseEntity updateDebitHistory(@PathVariable Long id,
			@RequestBody @Valid ApartmentDebitDto updatedDebitHistory) {
		UserDetailsImpl user = SecurityUtils.getCurrentUserDetails();
		log.info("Received request to update debit history with id: {} by user: {}", id, user);

		NivaasApartmentModel nivaasApartmentModel = apartmentService.findById(updatedDebitHistory.getApartmentId());
		if (Objects.isNull(nivaasApartmentModel)
				|| !userRoleHelper.isValidApartmentAdmin(user.getId(), nivaasApartmentModel)) {
			log.warn("Unauthorized access or invalid apartment for user: {}", user);

			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}
		service.updateDebitHistory(id, updatedDebitHistory);
		log.info("Debit history with id: {} updated successfully by user: {}", id, user);

		return ResponseEntity.ok().body(new MessageDTO("Updated the record"));
	}

	@DeleteMapping("/apartment/{apartmentId}/debit/{id}")
	@PreAuthorize(NivaasConstants.ROLE_APARTMENT_ADMIN)
	public ResponseEntity<Void> deleteDebitHistory(@PathVariable Long apartmentId, @PathVariable Long id) {
		UserDetailsImpl user = SecurityUtils.getCurrentUserDetails();
		log.info("Received request to delete debit history with id: {} in apartment: {} by user: {}", id, apartmentId,
				user);
		NivaasApartmentModel nivaasApartmentModel = apartmentService.findById(apartmentId);
		if (Objects.isNull(nivaasApartmentModel)
				|| !userRoleHelper.isValidApartmentAdmin(user.getId(), nivaasApartmentModel)) {
			log.warn("Unauthorized access or invalid apartment for user: {}", user);

			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}
		if (service.deleteDebitHistory(id)) {
			log.info("Debit history with id: {} deleted successfully by user: {}", id, user);

			return ResponseEntity.ok().build();
		} else {
			log.info("Debit history with id: {} not found for deletion", id);

			return ResponseEntity.notFound().build();
		}
	}

	@GetMapping("/apartment/{apartmentId}/year/{year}/month/{month}")
	@PreAuthorize(NivaasConstants.ROLE_APARTMENT_ADMIN + " " + NivaasConstants.OR + " " + NivaasConstants.ROLE_FLAT_OWNER)
	public List<ApartmentDebitHistoryModel> getAllDebitHistoriesBy(@PathVariable Long apartmentId,
			@PathVariable @Valid int year, @PathVariable @Valid @Min(1) @Max(12) int month) {
		log.info("Fetching all debit histories for apartmentId: {} in year: {}, month: {}", apartmentId, year, month);
		return service.getAllDebitHistories(apartmentId, year, month);
	}

	@GetMapping("/{id}")
	@PreAuthorize(NivaasConstants.ROLE_APARTMENT_ADMIN + " " + NivaasConstants.OR + " " + NivaasConstants.ROLE_FLAT_OWNER)
	public ResponseEntity<ApartmentDebitHistoryModel> getDebitHistoryById(@PathVariable Long id) {
		log.info("Fetching debit history with id: {} ", id);

		return service.getDebitHistoryById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
	}
}
