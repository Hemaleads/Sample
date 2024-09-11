package com.juvarya.nivaas.customer.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.juvarya.nivaas.auth.exception.handling.ErrorCode;
import com.juvarya.nivaas.auth.exception.handling.NivaasCustomerException;
import com.juvarya.nivaas.commonservice.dto.BasicOnboardUserDTO;
import com.juvarya.nivaas.commonservice.dto.JTUserDTO;
import com.juvarya.nivaas.commonservice.dto.LoggedInUser;
import com.juvarya.nivaas.commonservice.dto.OnboardingRequestDTO;
import com.juvarya.nivaas.commonservice.dto.UserDTO;
import com.juvarya.nivaas.commonservice.enums.ERole;
import com.juvarya.nivaas.commonservice.user.UserDetailsImpl;
import com.juvarya.nivaas.customer.client.AccessMgmtClient;
import com.juvarya.nivaas.customer.dto.BulkFlatOnboardDto;
import com.juvarya.nivaas.customer.firebase.listeners.NotificationPublisher;
import com.juvarya.nivaas.customer.model.ApartmentAndFlatRelatedUsersModel;
import com.juvarya.nivaas.customer.model.ApartmentUserRoleModel;
import com.juvarya.nivaas.customer.model.NivaasApartmentModel;
import com.juvarya.nivaas.customer.model.NivaasFlatModel;
import com.juvarya.nivaas.customer.model.NotificationModel;
import com.juvarya.nivaas.customer.model.OnboardingRequest;
import com.juvarya.nivaas.customer.model.constants.NotificationType;
import com.juvarya.nivaas.customer.model.constants.OnboardType;
import com.juvarya.nivaas.customer.model.constants.RelatedType;
import com.juvarya.nivaas.customer.populator.OnboardingRequestPopulator;
import com.juvarya.nivaas.customer.proxy.AccessMgmtClientProxy;
import com.juvarya.nivaas.customer.repository.ApartmentAndFlatRelatedUsersModelRepository;
import com.juvarya.nivaas.customer.repository.ApartmentUserRoleRepository;
import com.juvarya.nivaas.customer.repository.OnboardingRequestRepository;
import com.juvarya.nivaas.customer.service.CurrentApartmentService;
import com.juvarya.nivaas.customer.service.NivaasApartmentService;
import com.juvarya.nivaas.customer.service.NivaasFlatService;
import com.juvarya.nivaas.customer.service.NotificationService;
import com.juvarya.nivaas.customer.service.OnboardingRequestService;
import com.juvarya.nivaas.utils.NivaasConstants;
import com.juvarya.nivaas.utils.SecurityUtils;
import com.juvarya.nivaas.utils.converter.AbstractConverter;
import com.juvarya.nivaas.utils.converter.JTBaseEndpoint;

import lombok.extern.slf4j.Slf4j;

@SuppressWarnings("rawtypes")
@Service
@Slf4j
public class OnboardingRequestServiceImpl extends JTBaseEndpoint implements OnboardingRequestService {

	@Autowired
	private OnboardingRequestRepository onboardingRequestRepository;

	@Autowired
	private OnboardingRequestPopulator onboardingRequestPopulator;

	@Autowired
	private AccessMgmtClientProxy accessMgmtClientProxy;

	@Autowired
	private AccessMgmtClient accessMgmtClient;

	@Autowired
	private ApartmentAndFlatRelatedUsersModelRepository relatedUsersModelRepository;

	@Autowired
	private NotificationService notificationService;

	@Autowired
	private NotificationPublisher notificationPublisher;

	@Autowired
	private NivaasFlatService flatService;

	@Autowired
	private NivaasApartmentService apartmentService;

	@Autowired
	private ApartmentUserRoleRepository apartmentUserRoleRepository;

	@Autowired
	private CurrentApartmentService currentApartmentService;

	@Transactional
	@Override
	public void bulkAdd(final BulkFlatOnboardDto flatOnboardDto) {
		UserDetailsImpl loggedInUser = SecurityUtils.getCurrentUserDetails();
		log.info("Starting bulk add for flats: {} LoggedInUser: {}", flatOnboardDto, loggedInUser.getId());

		NivaasApartmentModel jtApartmentModel = apartmentService.findById(flatOnboardDto.getApartmentId());
		if (null == jtApartmentModel) {
			log.warn("Apartment not found: {}", flatOnboardDto.getApartmentId());
			throw new NivaasCustomerException(ErrorCode.APARTMENT_NOT_FOUND);
		}
		log.debug("NivaasApartmentModel: {}", jtApartmentModel);

		int totalFlats = jtApartmentModel.getTotalFlats();
		List<NivaasFlatModel> flatModels = flatService.getAllFlatsByApartment(jtApartmentModel.getId());
		int flats = flatModels.size();

		ApartmentUserRoleModel apartmentUserRoleModel = apartmentUserRoleRepository
				.findByApartmentModelAndCustomerId(jtApartmentModel, loggedInUser.getId());
		if (null == apartmentUserRoleModel) {
			log.warn("User is not allowed to onboard flats: {}", loggedInUser.getId());
			throw new NivaasCustomerException(ErrorCode.FLAT_ONBOARD_NOT_ALLOWED);
		}
		int onboardFlats = flatOnboardDto.getFlats().size();
		if (flats + onboardFlats > totalFlats) {
			throw new NivaasCustomerException(ErrorCode.FLAT_LIMIT, ErrorCode.FLAT_LIMIT.formatMessage(totalFlats));
		}
		log.info("User has apartment role: {}", apartmentUserRoleModel);
		List<NivaasFlatModel> flatModelList = new ArrayList<>();
		List<OnboardingRequest> onboardingRequests = new ArrayList<>();
		List<NotificationModel> notificationModels = new ArrayList<>();
		flatOnboardDto.getFlats().stream().filter(
				flatBasicDTO -> !flatService.checkFlatExists(flatOnboardDto.getApartmentId(), flatBasicDTO.getFlatNo()))
				.forEach(flatBasicDTO -> {
					NivaasFlatModel flatModel = new NivaasFlatModel();
					LoggedInUser owner = accessMgmtClient.getByPrimaryContact(flatBasicDTO.getOwnerPhoneNo());
					Long flatOwnerId;
					if (null != owner) {
						accessMgmtClient.addRole(owner.getId(), ERole.ROLE_FLAT_OWNER);
						flatModel.setOwnerId(owner.getId());
						flatOwnerId = owner.getId();
						log.debug("Owner found and role added: {}", owner.getId());
					} else {
						BasicOnboardUserDTO basicOnboardUserDTO = BasicOnboardUserDTO.builder()
								.primaryContact(flatBasicDTO.getOwnerPhoneNo()).fullName(flatBasicDTO.getOwnerName())
								.userRoles(Set.of(ERole.ROLE_USER, ERole.ROLE_FLAT_OWNER)).build();
						flatOwnerId = accessMgmtClient.onBoardUser(basicOnboardUserDTO);
						flatModel.setOwnerId(flatOwnerId);
						log.debug("New owner onboarded: {}", flatOwnerId);
					}
					// By-default setting flat is available for rent
					flatModel.setAvailableForRent(true);
					flatModel.setFlatNo(flatBasicDTO.getFlatNo());
					flatModel.setApartment(jtApartmentModel);
					// LoggedIn user sets as requested user because he raised bulk add
					onboardingRequests.add(buildOnboardRequest(jtApartmentModel, flatModel, flatOwnerId));

					flatModelList.add(flatModel);
					NotificationModel notificationModel = getNotificationModel(jtApartmentModel, flatModel,
							flatOwnerId);
					notificationModels.add(notificationModel);
					currentApartmentService.setCurrentApartmentIfNotExists(flatOwnerId, jtApartmentModel.getId());
				});
		flatService.saveAll(flatModelList);
		log.info("Saved flat models: {}", flatModelList);
		bulkOnBoardFlat(onboardingRequests);
		log.info("Bulk onboarded flats: {}", onboardingRequests);
		notificationService.saveAll(notificationModels);
		log.info("Saved notifications: {}", notificationModels);

		notificationModels.forEach(notification -> notificationPublisher.sendNotification(
				notification.getNivaasApartmentModel().getId(), notification.getUserId(), false, false,
				notification.getFlatModel().getId(), true, true, null, null, 0, false, null));
		log.info("Notifications sent");
	}

	@Override
	public Map<String, Object> getFlatOwners(Long apartmentId, int pageNo, int pageSize) {
		log.info("Fetching flat owners for apartment ID: {}", apartmentId);
		Pageable pageable = PageRequest.of(pageNo, pageSize);
		Page<NivaasFlatModel> flats = flatService.getFlatsByApartment(apartmentId, pageable);

		if (flats == null || CollectionUtils.isEmpty(flats.getContent())) {
			log.warn("No flats found for apartment ID: {}", apartmentId);
			return null;
		}

		List<JTUserDTO> users = new ArrayList<>();

		Map<String, Object> response = new HashMap<>();
		for (NivaasFlatModel flatModel : flats) {
			OnboardingRequest onboardingRequest = findByFlatAndAdminApproved(flatModel);

			if (null != flatModel.getOwnerId() && null != onboardingRequest
					&& onboardingRequest.getOnboardType().equals(OnboardType.FLAT)
					&& flatModel.getOwnerId().equals(onboardingRequest.getRequestedCustomer())) {
				UserDTO user = accessMgmtClient.getUserById(flatModel.getOwnerId());
				JTUserDTO jtUserDTO = new JTUserDTO();
				jtUserDTO.setId(flatModel.getOwnerId());
				jtUserDTO.setFullName(user.getFullName());
				jtUserDTO.setPrimaryContact(user.getPrimaryContact());

				users.add(jtUserDTO);
			}
		}

		response.put(NivaasConstants.CURRENT_PAGE, flats.getNumber());
		response.put(NivaasConstants.TOTAL_ITEMS, users.size());
		response.put(NivaasConstants.TOTAL_PAGES, flats.getTotalPages());
		response.put(NivaasConstants.PAGE_NUM, pageNo);
		response.put(NivaasConstants.PAGE_SIZE, pageSize);
		response.put(NivaasConstants.PROFILES, users);

		log.info("Returning {} flat owners for apartment ID: {}", users.size(), apartmentId);
		return response;
	}

	@Transactional
	public OnboardingRequest save(OnboardingRequest jtonboardingRequest) {
		return onboardingRequestRepository.save(jtonboardingRequest);
	}

	public void bulkOnBoardFlat(final List<OnboardingRequest> onboardingRequests) {
		onboardingRequestRepository.saveAll(onboardingRequests);
	}

	public OnboardingRequest findById(Long id) {
		Optional<OnboardingRequest> onbOptional = onboardingRequestRepository.findById(id);
		return onbOptional.orElse(null);
	}

	@Override
	public Page<OnboardingRequest> findByStatus(boolean status, Pageable pageble) {
		return onboardingRequestRepository.findByStatus(status, pageble);
	}

	@Override
	public Page<OnboardingRequest> findByFlat(NivaasFlatModel nivaasFlatModel, Pageable pageable) {
		return onboardingRequestRepository.findByFlat(nivaasFlatModel, pageable);
	}

	@Override
	public OnboardingRequest findByFlatAndAdminApproved(NivaasFlatModel nivaasFlatModel) {
		return onboardingRequestRepository.findByFlatAndAdminApproved(nivaasFlatModel, Boolean.TRUE);
	}

	@Override
	public List<OnboardingRequest> findByRequestCustomer(Long userId) {
		return onboardingRequestRepository.findByRequestedCustomer(userId);
	}

	@Override
	public LoggedInUser findByUserAndApartmentId(final LoggedInUser loggedInUser, final Long apartmentId) {
		List<OnboardingRequest> requests = onboardingRequestRepository.findByUserAndApartmentId(loggedInUser.getId(),
				apartmentId);
		return buildLoggedInUserWithOnboardRequest(loggedInUser, requests);
	}

	@Override
	public boolean isValidApartmentUserMap(final Long userId, final Long apartmentId) {
		return onboardingRequestRepository.existsByUserAndApartmentId(userId, apartmentId);
	}

	@Override
	public void onBoardApartmentAdmin(NivaasApartmentModel nivaasApartmentModel, Long customerId) {
		OnboardingRequest onboardingRequest = new OnboardingRequest();

		onboardingRequest.setApartment(nivaasApartmentModel);
		onboardingRequest.setAdminApproved(false);
		onboardingRequest.setStatus(false);
		onboardingRequest.setCreationTime(new Date());
		onboardingRequest.setModificationTime(new Date());
		onboardingRequest.setOnboardType(OnboardType.APARTMENT);
		onboardingRequest.setRequestedCustomer(customerId);
		save(onboardingRequest);
	}

	@Override
	public void onBoardCoAdmin(final Long apartmentId, final Long userId) {
		List<OnboardingRequest> onboardingRequests = onboardingRequestRepository
				.findByApartmentAndAdminApprovedAndOnboardType(apartmentId, OnboardType.APARTMENT);
		if (!CollectionUtils.isEmpty(onboardingRequests) && onboardingRequests.size() == 1) {
			buildAndSaveOnBoardRelatedUsers(onboardingRequests.get(0), userId, RelatedType.CO_ADMIN, true);
		} else {
			log.warn("Invalid onboard coAdmin request for apartment {} coAdmin user {}", apartmentId, userId);
		}

	}

	@Override
	@Transactional
	public void flatRelatedOnboarding(final OnboardingRequestDTO onboardingRequestDTO, final NivaasFlatModel flatModel,
			final RelatedType relatedType) {
		UserDetailsImpl loggedInUser = SecurityUtils.getCurrentUserDetails();
		OnboardingRequest onboardingRequest = findByFlatAndAdminApproved(flatModel);
		if (onboardingRequest == null) {
			log.warn("Did not find valid onboarding request for {}", flatModel.getId());
			throw new NivaasCustomerException(ErrorCode.NOT_VALID);
		}

		boolean isRequestAlreadyExists = onboardingRequest.getRelatedUsers() != null
				&& onboardingRequest.getRelatedUsers().stream().anyMatch(
						o -> relatedType.equals(o.getRelatedType()) && o.getUserId().equals(loggedInUser.getId()));
		if (isRequestAlreadyExists) {
			log.warn("Request already exists for user {} flat {}", loggedInUser.getId(), flatModel.getId());
			throw new NivaasCustomerException(ErrorCode.DUPLICATE);
		}
		switch (relatedType) {
		case TENANT:
			if (Boolean.FALSE.equals(flatModel.isAvailableForRent())) {
				throw new NivaasCustomerException(ErrorCode.FLAT_NOT_AVAILABLE_FOR_RENT);
			}
			log.info("Sending tenant onboarding request for flat ID: {}", flatModel.getId());
			buildAndOnboardFlatRelatedUser(flatModel, loggedInUser.getId(), RelatedType.TENANT,
					NotificationType.TENANT_ONBOARD);
			break;
		case FLAT_OWNER_FAMILY_MEMBER:
			log.info("Sending family member onboarding request for flat ID: {}", flatModel.getId());
			buildAndOnboardFlatRelatedUser(flatModel, loggedInUser.getId(), RelatedType.FLAT_OWNER_FAMILY_MEMBER,
					NotificationType.FLAT_OWNER_FAMILY_MEMBER);
			break;
		default:
			throw new NivaasCustomerException(ErrorCode.NOT_SUPPORTED);
		}
	}

	private void buildAndOnboardFlatRelatedUser(final NivaasFlatModel flatModel, final Long userId,
			final RelatedType relatedType, final NotificationType notificationType) {
		List<OnboardingRequest> onboardingRequests = onboardingRequestRepository
				.findByFlatAndAdminApprovedAndOnboardType(flatModel.getId(), OnboardType.FLAT);
		if (!CollectionUtils.isEmpty(onboardingRequests) && onboardingRequests.size() == 1) {
			buildAndSaveOnBoardRelatedUsers(onboardingRequests.get(0), userId, relatedType, false);
			buildAndSendTenantNotification(flatModel, userId, notificationType);
			currentApartmentService.setCurrentApartmentIfNotExists(userId, flatModel.getApartment().getId());
		} else {
			log.warn("Invalid onboard tenant request for flat {} tenant user {}", flatModel.getId(), userId);
		}
	}

	@Override
	public void approveFlatRelatedUsers(final OnboardingRequest onboardingRequest, final Long userId,
			final RelatedType relatedType) {
		ApartmentAndFlatRelatedUsersModel apartmentAndFlatRelatedUsersModel = onboardingRequest.getRelatedUsers()
				.stream().filter(o -> RelatedType.TENANT.equals(o.getRelatedType()) && o.getUserId().equals(userId))
				.findFirst().orElseThrow(() -> new NivaasCustomerException(ErrorCode.NOT_FOUND));
		apartmentAndFlatRelatedUsersModel.setRelatedUserApproved(true);
		relatedUsersModelRepository.save(apartmentAndFlatRelatedUsersModel);
		// TODO: need to understand the requirement to have tenantId in flat model
		// Also, not updating available for rent to false because to support multiple
		// tenants
		/*
		 * final NivaasFlatModel flatModel = onboardingRequest.getFlat();
		 * flatModel.setTenantId(onboardingRequest.getRequestedCustomer());
		 * flatModel.setAvailableForRent(Boolean.FALSE); flatService.save(flatModel);
		 */

		buildAndSendTenantNotification(onboardingRequest.getFlat(), userId, NotificationType.TENANT_APPROVAL);
	}

	@Override
	public LoggedInUser getOnboardRequests() {
		LoggedInUser loggedInUser = accessMgmtClientProxy.getCurrentCustomer();
		if (loggedInUser != null) {
			List<OnboardingRequest> requests = findByRequestCustomer(loggedInUser.getId());
			return buildLoggedInUserWithOnboardRequest(loggedInUser, requests);
		}
		return null;
	}

	private void buildAndSaveOnBoardRelatedUsers(final OnboardingRequest existingOnboardRequest,
			final Long relatedUserId, final RelatedType relatedType, final boolean relatedUserApproved) {
		ApartmentAndFlatRelatedUsersModel apartmentAndFlatRelatedUsersModel = new ApartmentAndFlatRelatedUsersModel();
		apartmentAndFlatRelatedUsersModel.setUserId(relatedUserId);
		apartmentAndFlatRelatedUsersModel.setOnboardingRequestId(existingOnboardRequest.getId());
		apartmentAndFlatRelatedUsersModel.setRelatedType(relatedType);
		apartmentAndFlatRelatedUsersModel.setRelatedUserApproved(relatedUserApproved);
		apartmentAndFlatRelatedUsersModel = relatedUsersModelRepository.save(apartmentAndFlatRelatedUsersModel);
		List<ApartmentAndFlatRelatedUsersModel> relatedUsersModels = existingOnboardRequest.getRelatedUsers();
		relatedUsersModels.add(apartmentAndFlatRelatedUsersModel);
		existingOnboardRequest.setRelatedUsers(relatedUsersModels);
		onboardingRequestRepository.save(existingOnboardRequest);
	}

	private static NotificationModel getNotificationModel(NivaasApartmentModel jtApartmentModel,
			NivaasFlatModel flatModel, Long flatOwnerId) {
		NotificationModel notificationModel = new NotificationModel();
		notificationModel.setCreationTime(new Date());
		notificationModel.setNivaasApartmentModel(jtApartmentModel);
		notificationModel.setFlatModel(flatModel);
		notificationModel.setUserId(flatOwnerId);
		notificationModel.setType(NotificationType.FLAT_APPROVED);
		notificationModel.setMessage(jtApartmentModel.getName() + " " + flatModel.getFlatNo() + " " + flatOwnerId + " "
				+ ",NIVAAS Admin Approved Your Flat");
		return notificationModel;
	}

	@SuppressWarnings("unchecked")
	private LoggedInUser buildLoggedInUserWithOnboardRequest(final LoggedInUser loggedInUser,
			final List<OnboardingRequest> requests) {
		List<OnboardingRequestDTO> apartmentOnboards = new ArrayList<>();
		List<OnboardingRequestDTO> flatOnboards = new ArrayList<>();
		try {
			if (!CollectionUtils.isEmpty(requests)) {
				for (OnboardingRequest onboardingRequest : requests) {
					if (onboardingRequest.getApartment() != null
							&& onboardingRequest.getOnboardType().equals(OnboardType.APARTMENT)) {
						OnboardingRequestDTO onboardingRequestDTO = (OnboardingRequestDTO) getConverterInstance()
								.convert(onboardingRequest);
						apartmentOnboards.add(onboardingRequestDTO);
					}

					if (onboardingRequest.getFlat() != null
							&& onboardingRequest.getOnboardType().equals(OnboardType.FLAT)) {
						OnboardingRequestDTO onboardingRequestDTO = (OnboardingRequestDTO) getConverterInstance()
								.convert(onboardingRequest);
						onboardingRequestDTO.setApartmentDTO(null);
						flatOnboards.add(onboardingRequestDTO);
					}
				}
				loggedInUser.setApartmentDTOs(apartmentOnboards);
				loggedInUser.setFlatDTO(flatOnboards);
				return loggedInUser;
			}
			return loggedInUser;
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException exception) {
			log.error("Failed to convert ", exception);
			return null;
		}
	}

	private void buildAndSendTenantNotification(final NivaasFlatModel flatModel, final Long userId,
			final NotificationType notificationType) {
		NotificationModel notificationModel = new NotificationModel();
		notificationModel.setCreationTime(new Date());
		notificationModel.setFlatModel(flatModel);
		notificationModel.setNivaasApartmentModel(flatModel.getApartment());
		notificationModel.setType(notificationType);
		notificationModel.setUserId(userId);
		notificationModel.setMessage("Tenant OnBoarding Request Sent");

		notificationService.save(notificationModel);
		notificationPublisher.sendNotification(notificationModel.getNivaasApartmentModel().getId(), null, false, false,
				notificationModel.getFlatModel().getId(), true, false, null, null, 0, true,
				notificationModel.getUserId());

	}

	@SuppressWarnings("unchecked")
	public AbstractConverter getConverterInstance() {
		return getConverter(onboardingRequestPopulator, OnboardingRequestDTO.class.getName());
	}

	private static OnboardingRequest buildOnboardRequest(final NivaasApartmentModel nivaasApartmentModel,
			final NivaasFlatModel flatModel, final Long userId) {
		OnboardingRequest onboardingRequest = new OnboardingRequest();
		onboardingRequest.setApartment(nivaasApartmentModel);
		onboardingRequest.setFlat(flatModel);
		onboardingRequest.setAdminApproved(Boolean.TRUE);
		onboardingRequest.setStatus(Boolean.TRUE);
		onboardingRequest.setOnboardType(OnboardType.FLAT);
		onboardingRequest.setCreationTime(new Date());
		onboardingRequest.setApprovedOn(new Date());
		onboardingRequest.setModificationTime(new Date());
		onboardingRequest.setRequestedCustomer(userId);
		return onboardingRequest;
	}
}
