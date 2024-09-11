package com.juvarya.nivaas.access.mgmt.controllers;

import com.juvarya.nivaas.access.mgmt.model.Role;
import com.juvarya.nivaas.access.mgmt.services.RoleService;
import com.juvarya.nivaas.commonservice.enums.ERole;
import com.juvarya.nivaas.utils.converter.JTBaseEndpoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/role")
public class RoleController extends JTBaseEndpoint {

    @Autowired
    private RoleService roleService;


    @GetMapping("/find/{erole}")
    public Role getByERole(@PathVariable("erole") ERole eRole) {
        return roleService.findByErole(eRole);
    }

}
