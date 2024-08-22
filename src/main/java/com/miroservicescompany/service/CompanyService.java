package com.miroservicescompany.service;

import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Service;

import com.miroservicescompany.dto.CompanyDto;
import com.miroservicescompany.dto.EncryptedData;
import com.miroservicescompany.dto.Encryption;
import com.miroservicescompany.email.EmailService;
import com.miroservicescompany.entity.Company;
import com.miroservicescompany.enumeration.GraceStatus;
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
	
	private final EmailService emailService;

	public Company createCompany(CompanyDto companyDto) {
		try {
			Company company = Company.builder().companyName(companyDto.getCompanyName()).email(companyDto.getEmail())
					.address(companyDto.getAddress()).build();

			// Generate license
			String license = licenseGenerator.generateLicense(company);
			company.setLicense(license);
			company.setStatus(Status.CREATE);

			return companyRepository.save(company);
		} catch (Exception e) {
			throw new CompanyServiceException("Failed to create company", e);
		}
	}

	public EncryptedData encryptEmailLicense(String companyName, String adminEmail, String subject) {
	    Company company = companyRepository.findByCompanyName(companyName)
	            .orElseThrow(() -> new CompanyNotFoundException("Company not found: " + companyName));
	    try {
	        // Generate secret key
	        SecretKey secretKey = encryptionService.generateSecretKey();

	        // Encrypt company email and license using the secret key
	        EncryptedData encryptedData = encryptionService.encrypt(company.getEmail() + ";" + company.getLicense(), secretKey);



	        // Convert secret key to a byte array
	        byte[] secretKeyBytes = secretKey.getEncoded();

	        // Convert byte arrays to strings using Base64 encoding
	        String encryptedDataString = encryptedData.toString();
	        String secretKeyString = Base64.getEncoder().encodeToString(secretKeyBytes);
	        
	        
	        company.setStatus(Status.REQUEST);
	        companyRepository.save(company);

	        // Prepare the email body with the necessary details
			/*
			 * String emailBody = String.format( //company.getCompanyName(),
			 * encryptedData.getEncryptedData(), encryptedData.getSecretKey());
			 */

	        // Send email notification to admin with encrypted data and secret key
	        emailService.sendSimpleMessage(adminEmail, subject, emailBody);

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
				return CompanyDto.builder().id(companyObj.getId()).companyName(companyObj.getCompanyName())
						.email(companyObj.getEmail()).address(companyObj.getAddress()).license(companyObj.getLicense())
						.activationDate(companyObj.getActivationDate()).expireDate(companyObj.getExpireDate())
						.status(companyObj.getStatus()).gracePeriod(companyObj.getGracePeriod())
						.graceStatus(companyObj.getGraceStatus()).build();
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
					.map(company -> CompanyDto.builder().id(company.getId()).companyName(company.getCompanyName())
							.email(company.getEmail()).address(company.getAddress()).license(company.getLicense())
							.activationDate(company.getActivationDate()).expireDate(company.getExpireDate())
							.status(company.getStatus()).gracePeriod(company.getGracePeriod())
							.graceStatus(company.getGraceStatus()).build())
					.collect(Collectors.toList());
		} catch (Exception e) {
			throw new CompanyAccessException("Error accessing company data", e);
		}
	}

	public String decryptForActivate(Encryption encryption) {
	    if (encryption != null) {
	        LocalDate activationDate = LocalDate.now();
	        Optional<Company> companyOptional = companyRepository.findByEmailAndLicense(encryption.getEmail(), encryption.getLicense());

	        if (companyOptional.isPresent()) {
	            Company company = companyOptional.get();
	            company.setActivationDate(activationDate);
	            LocalDate expireDate = activationDate.plusDays(30); // Set expiration date 30 days from activation
	            company.setExpireDate(expireDate);
	            company.setGraceStatus(GraceStatus.ACTIVE);

	            // Calculate grace period
	            LocalDate gracePeriodStart = expireDate.plusDays(1);
	            LocalDate gracePeriodEnd = gracePeriodStart.plusDays(1);
	            company.setGracePeriod(gracePeriodEnd); // Set the end date of the grace period

	            company.setStatus(Status.APPROVED);

	            companyRepository.save(company);
	            return "Company activated successfully";
	        } else {
	            return "Company not found";
	        }
	    }
	    return "Invalid encryption data";
	}
}



