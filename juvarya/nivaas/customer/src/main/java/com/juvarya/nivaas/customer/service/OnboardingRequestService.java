package com.juvarya.nivaas.customer.service;

import java.util.List;
import java.util.Map;

import com.juvarya.nivaas.commonservice.dto.OnboardingRequestDTO;
import com.juvarya.nivaas.customer.dto.BulkFlatOnboardDto;
import com.juvarya.nivaas.customer.model.constants.RelatedType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.juvarya.nivaas.commonservice.dto.LoggedInUser;
import com.juvarya.nivaas.customer.model.NivaasApartmentModel;
import com.juvarya.nivaas.customer.model.NivaasFlatModel;
import com.juvarya.nivaas.customer.model.OnboardingRequest;

public interface OnboardingRequestService {

	void bulkAdd(final BulkFlatOnboardDto flatOnboardDto);

	Map<String, Object> getFlatOwners(Long apartmentId, int pageNo, int pageSize);
	OnboardingRequest save(OnboardingRequest jtonboardingRequest);

	void bulkOnBoardFlat(final List<OnboardingRequest> onboardingRequests);

	OnboardingRequest findById(Long id);

	Page<OnboardingRequest> findByStatus(boolean status, Pageable pageble);

	Page<OnboardingRequest> findByFlat(NivaasFlatModel nivaasFlatModel, Pageable pageable);

	OnboardingRequest findByFlatAndAdminApproved(NivaasFlatModel nivaasFlatModel);

	List<OnboardingRequest> findByRequestCustomer(Long userId);

	LoggedInUser findByUserAndApartmentId(LoggedInUser loggedInUser, Long apartmentId);

	boolean isValidApartmentUserMap(final Long userId, final Long apartmentId);

	void onBoardApartmentAdmin(NivaasApartmentModel nivaasApartmentModel, Long customerId);

	void onBoardCoAdmin(final Long apartmentId, final Long userId);

	void flatRelatedOnboarding(final OnboardingRequestDTO onboardingRequestDTO,
							   final NivaasFlatModel flatModel, final RelatedType relatedType);

	void approveFlatRelatedUsers(final OnboardingRequest onboardingRequest, final Long userId, final RelatedType relatedType);

	LoggedInUser getOnboardRequests()
			throws ClassNotFoundException, InstantiationException, IllegalAccessException;
}
