package com.juvarya.nivaas.customer.service;

import com.juvarya.nivaas.commonservice.dto.LoggedInUser;
import com.juvarya.nivaas.customer.model.CurrentApartment;

public interface CurrentApartmentService {
    CurrentApartment setCurrentApartment(Long userId, Long apartmentId);

    void setCurrentApartmentIfNotExists(Long userId, Long apartmentId);

    LoggedInUser getCurrentApartment();
}
