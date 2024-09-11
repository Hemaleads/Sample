package com.juvarya.nivaas.customer.controllers;

import com.juvarya.nivaas.commonservice.dto.LoggedInUser;
import com.juvarya.nivaas.customer.model.CurrentApartment;
import com.juvarya.nivaas.customer.service.CurrentApartmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/current-apartment")
public class CurrentApartmentController {

    @Autowired
    private CurrentApartmentService currentApartmentService;

    @PutMapping("/set")
    public ResponseEntity<Void> setCurrentApartment(@RequestParam Long userId, @RequestParam Long apartmentId) {
        CurrentApartment currentApartment = currentApartmentService.setCurrentApartment(userId, apartmentId);
        if (currentApartment == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok().build();
    }

    @GetMapping("/get")
    public ResponseEntity<LoggedInUser> getCurrentApartment() {
        LoggedInUser loggedInUser = currentApartmentService.getCurrentApartment();
        if (loggedInUser != null) {
            return ResponseEntity.ok().body(loggedInUser);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}

