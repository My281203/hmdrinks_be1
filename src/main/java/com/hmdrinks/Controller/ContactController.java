package com.hmdrinks.Controller;

import com.hmdrinks.Request.*;
import com.hmdrinks.Response.CRUDContactResponse;
import com.hmdrinks.Response.CRUDVoucherResponse;
import com.hmdrinks.Response.ListAllContactResponse;
import com.hmdrinks.Response.ListAllVoucherResponse;
import com.hmdrinks.Service.ContactService;
import com.hmdrinks.Service.VoucherService;
import com.hmdrinks.SupportFunction.SupportFunction;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin
@RestController
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/contact")
public class ContactController {
    @Autowired
    private ContactService contactService;
    @Autowired
    private SupportFunction supportFunction;

    @PostMapping(value = "/create")
    public ResponseEntity<?> createVoucher(@RequestBody @Valid  CreateContactReq req){
        return contactService.createContact(req);
    }

    @GetMapping(value ="/view/{id}")
    public ResponseEntity<?> getOneContact(
            @PathVariable Integer id,HttpServletRequest httpRequest
    ){
        ResponseEntity<?> validation = supportFunction.validatePositiveId("id", id);
        if (validation != null) return validation;
        return contactService.getContactById(id);
    }

    @PutMapping(value = "/update")
    public ResponseEntity<?> updateContact(
            @RequestBody @Valid  CrudContactReq req, HttpServletRequest httpRequest
    )
    {
        return  contactService.updateContact(req);
    }

    @GetMapping(value = "/view/all")
    public ResponseEntity<?> getAllContact(
            @RequestParam(name = "page") String page, @RequestParam(name = "limit") String limit
    ){

        ResponseEntity<?> validationResult = supportFunction.validatePaginationParams(page, limit);

        if (validationResult != null) {
            return validationResult;
        }

        return  ResponseEntity.ok(contactService.listAllContact(page,limit));
    }

    @GetMapping(value = "/view/all/complete")
    public ResponseEntity<?> getAllContactComplete(
            @RequestParam(name = "page") String page, @RequestParam(name = "limit") String limit
    ){

        ResponseEntity<?> validationResult = supportFunction.validatePaginationParams(page, limit);

        if (validationResult != null) {
            return validationResult;
        }
        return  ResponseEntity.ok(contactService.listAllContactComplete(page,limit));
    }

    @GetMapping(value = "/view/all/waiting")
    public ResponseEntity<?> getAllContactWaiting(
            @RequestParam(name = "page") String page, @RequestParam(name = "limit") String limit
    ){

        ResponseEntity<?> validationResult = supportFunction.validatePaginationParams(page, limit);

        if (validationResult != null) {
            return validationResult;
        }
        return  ResponseEntity.ok(contactService.listAllContactWaiting(page,limit));
    }

    @PutMapping("/contact/response")
    public  ResponseEntity<?> responseContact(@RequestBody @Valid  AcceptContactReq req)
    {
        return ResponseEntity.ok(contactService.responseContact(req));
    }

}