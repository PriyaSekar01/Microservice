package com.miroservicescompany.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.miroservicescompany.dto.CompanyDto;
import com.miroservicescompany.dto.EncryptedData;
import com.miroservicescompany.entity.Company;
import com.miroservicescompany.exception.CompanyAccessException;
import com.miroservicescompany.exception.CompanyNotFoundException;
import com.miroservicescompany.exception.EncryptionException;
import com.miroservicescompany.response.Response;
import com.miroservicescompany.response.ResponseGenerator;
import com.miroservicescompany.response.TransactionContext;
import com.miroservicescompany.service.CompanyService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/company")
@RequiredArgsConstructor
public class CompanyController {
	
	private final CompanyService companyService;
	
	private final ResponseGenerator  responseGenerator;
	
	@PostMapping("/create")
    public ResponseEntity<Response> createCompany(@RequestBody CompanyDto companyDto) {
        TransactionContext context = responseGenerator.generateTransactionContext(null);
        try {
            Company createdCompany = companyService.createCompany(companyDto);
            return responseGenerator.successResponse(context, createdCompany, HttpStatus.CREATED);
        } catch (Exception e) {
            return responseGenerator.errorResponse(context, "Failed to create company", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
	
	@PostMapping("/encryptEmailLicense")
    public ResponseEntity<Response> encryptEmailLicense(@RequestParam String companyName) {
        TransactionContext context = responseGenerator.generateTransactionContext(null);
        try {
          EncryptedData encryptedData = companyService.encryptEmailLicense(companyName);
            return responseGenerator.successResponse(context, encryptedData, HttpStatus.OK);
        } catch (EncryptionException e) {
            return responseGenerator.errorResponse(context, "Encryption failed", HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (CompanyNotFoundException e) {
            return responseGenerator.errorResponse(context, "Company not found", HttpStatus.NOT_FOUND);
        }
    }
	
	 @GetMapping("/license/{id}")
	    public ResponseEntity<Response> getLicenseById(@PathVariable Long id) {
	        TransactionContext context = responseGenerator.generateTransactionContext(null);
	        try {
	            CompanyDto company = companyService.getLicense(id);
	            if (company != null) {
	                return responseGenerator.successResponse(context, company, HttpStatus.OK);
	            } else {
	                return responseGenerator.errorResponse(context, "License not found", HttpStatus.NOT_FOUND);
	            }
	        } catch (Exception e) {
	            return responseGenerator.errorResponse(context, "Error retrieving license", HttpStatus.INTERNAL_SERVER_ERROR);
	        }
	    }
	 
	 
	 @GetMapping("/getAll")
	    public ResponseEntity<Response> getAllLicense() {
	        TransactionContext context = responseGenerator.generateTransactionContext(null);
	        try {
	            List<CompanyDto> companies = companyService.getAllLicense();
	            return responseGenerator.successResponse(context, companies, HttpStatus.OK);
	        } catch (CompanyAccessException e) {
	            return responseGenerator.errorResponse(context, "Error accessing company data", HttpStatus.INTERNAL_SERVER_ERROR);
	        } catch (Exception e) {
	            return responseGenerator.errorResponse(context, "Error retrieving all licenses", HttpStatus.INTERNAL_SERVER_ERROR);
	        }
	    }
}
