package com.miroservicescompany.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.miroservicescompany.dto.CompanyDto;
import com.miroservicescompany.dto.EncryptedData;
import com.miroservicescompany.entity.Company;
import com.miroservicescompany.enumeration.Status;
import com.miroservicescompany.exception.CompanyAccessException;
import com.miroservicescompany.exception.CompanyNotFoundException;
import com.miroservicescompany.exception.CompanyServiceException;
import com.miroservicescompany.exception.EncryptionException;
import com.miroservicescompany.generator.SecretKeyGenerator;
import com.miroservicescompany.repository.CompanyRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CompanyService {
	
	private final CompanyRepository companyRepository;
	
	private final LicenseGenerator licenseGenerator;
	
	private final SecretKeyGenerator encryptionService;

	public Company createCompany(CompanyDto companyDto) {
        try {
        	Company company = Company.builder()
                    .companyName(companyDto.getCompanyName())
                    .email(companyDto.getEmail())
                    .address(companyDto.getAddress())
                    .build();

            // Generate license
            String license = licenseGenerator.generateLicense(company);
            company.setLicense(license);
            company.setStatus(Status.CREATE);

            return companyRepository.save(company);
        } catch (Exception e) {
            throw new CompanyServiceException("Failed to create company", e);
        }
    }

	public EncryptedData encryptEmailLicense(String companyName) {
        Company company = companyRepository.findByCompanyName(companyName)
                .orElseThrow(() -> new CompanyNotFoundException("Company not found: " + companyName));
        try {
            EncryptedData encryptedData = encryptionService.encrypt(company.getEmail() + ";" + company.getLicense());
            company.setStatus(Status.REQUEST);
            companyRepository.save(company);
            return encryptedData;
        } catch (Exception e) {
            throw new EncryptionException("Encryption failed for company: " + companyName, e);
        }
    }

	public CompanyDto getLicense(Long id) {
	    try {
	        Optional<Company> company = companyRepository.findById(id);
	        if (company.isPresent()) {
	            Company companyObj = company.get();
	            return CompanyDto.builder()
	                    .id(companyObj.getId())
	                    .companyName(companyObj.getCompanyName())
	                    .email(companyObj.getEmail())
	                    .address(companyObj.getAddress())
	                    .license(companyObj.getLicense())
	                    .activationDate(companyObj.getActivationDate())
	                    .expireDate(companyObj.getExpireDate())
	                    .status(companyObj.getStatus())
	                    .gracePeriod(companyObj.getGracePeriod())
	                    .graceStatus(companyObj.getGraceStatus())
	                    .build();
	        } else {
	            throw new CompanyNotFoundException("Company not found with ID: " + id);
	        }
	    } catch (Exception e) {
	        throw new CompanyAccessException("Error accessing company data", e);
	    }
	}
	
	public List<CompanyDto> getAllLicense() {
        try {
            List<Company> companies = companyRepository.findAll();
            return companies.stream()
                    .map(company -> CompanyDto.builder()
                            .id(company.getId())
                            .companyName(company.getCompanyName())
                            .email(company.getEmail())
                            .address(company.getAddress())
                            .license(company.getLicense())
                            .activationDate(company.getActivationDate())
                            .expireDate(company.getExpireDate())
                            .status(company.getStatus())
                            .gracePeriod(company.getGracePeriod())
                            .graceStatus(company.getGraceStatus())
                            .build())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new CompanyAccessException("Error accessing company data", e);
        }
    }
}
