package com.juvarya.nivaas.customer.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import com.juvarya.nivaas.customer.NivaasBaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.juvarya.nivaas.customer.model.NivaasApartmentModel;

class ApartmentServiceTest extends NivaasBaseTest {

    @BeforeEach
    void setUp() {
        super.init();
    }

    @Test
    public void testFindByCreatedBy_shouldReturnApartments() {
        NivaasApartmentModel apartment1 = new NivaasApartmentModel();
        apartment1.setName("Apartment A");
        apartment1.setCreatedBy(user.getId());

        NivaasApartmentModel apartment2 = new NivaasApartmentModel();
        apartment2.setName("Apartment B");
        apartment2.setCreatedBy(user.getId());

        apartmentService.saveApartment(apartment1);
        apartmentService.saveApartment(apartment2);

        // Call service method
        List<NivaasApartmentModel> retrievedApartments = apartmentService.findByCreatedBy(user.getId());

        // Assertions
        assertEquals(2, retrievedApartments.size(), "Number of apartments should be 2");
        assertEquals(apartment1.getId(), retrievedApartments.get(0).getId(), "Apartment IDs should match");
        assertEquals(apartment2.getId(), retrievedApartments.get(1).getId(), "Apartment IDs should match");

        // Call service method
        NivaasApartmentModel retrievedApartment = apartmentService.findById(apartment1.getId());

        // Assertions
        assertEquals(apartment1.getId(), retrievedApartment.getId(), "Apartment IDs should match");
        assertEquals(apartment1.getName(), retrievedApartment.getName(), "Apartment names should match");
    }

}