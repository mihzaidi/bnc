package com.icsynergy.bnc.adapters;

import com.icsynergy.bnc.Constants;
import com.icsynergy.bnc.OIMUtility;

import Thor.API.Exceptions.tcAPIException;
import Thor.API.Exceptions.tcFormNotFoundException;
import Thor.API.Exceptions.tcInvalidValueException;
import Thor.API.Exceptions.tcNotAtomicProcessException;
import Thor.API.Exceptions.tcProcessNotFoundException;
import Thor.API.Exceptions.tcRequiredDataMissingException;
import Thor.API.Operations.tcFormInstanceOperationsIntf;
import Thor.API.Operations.tcITResourceInstanceOperationsIntf;
import Thor.API.Operations.tcLookupOperationsIntf;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.logging.Logger;
import oracle.iam.identity.exception.AccessDeniedException;
import oracle.iam.identity.exception.NoSuchUserException;
import oracle.iam.identity.exception.SearchKeyNotUniqueException;
import oracle.iam.identity.exception.UserLookupException;
import oracle.iam.identity.usermgmt.api.UserManagerConstants;
import oracle.iam.identity.usermgmt.vo.User;
import oracle.iam.platform.Platform;

public class ADProvisioning {
	private static final Logger log = Logger.getLogger("com.icsynergy");
	private tcLookupOperationsIntf lookupOperationsIntf = (tcLookupOperationsIntf) Platform
			.getService(tcLookupOperationsIntf.class);
	tcFormInstanceOperationsIntf formInstanceOperationsIntf = (tcFormInstanceOperationsIntf) Platform
			.getService(tcFormInstanceOperationsIntf.class);

	tcITResourceInstanceOperationsIntf itResourceinstanceOperationsIntf = (tcITResourceInstanceOperationsIntf) Platform
			.getService(tcITResourceInstanceOperationsIntf.class);

	public String prepolpulateUserPrincipalNAme(String userPrincipalNAme)
			throws NoSuchUserException, UserLookupException, SearchKeyNotUniqueException, AccessDeniedException {
		log.entering(super.getClass().getName(), "prepolpulateUserPrincipalNAme");
		log.fine("userPrincipalNAme" + userPrincipalNAme);
		String actualUserPrincipal = "";
		String[] prefixArray = userPrincipalNAme.split("@");
		actualUserPrincipal = prefixArray[0] + "@succ.bnc.ca";
		log.fine("actualUserPrincipal" + actualUserPrincipal);
		log.exiting(super.getClass().getName(), "prepolpulateUserPrincipalNAme");
		return actualUserPrincipal;
	}

	public String prepopEmployeeNumber(String accountID) {
		log.entering(super.getClass().getName(), "prepopEmployeeNumber");
		log.fine("userPrincipalNAme" + accountID);
		String employeeNumber = accountID + "-l";
		log.fine("employeeNumber" + employeeNumber);
		log.exiting(super.getClass().getName(), "prepopEmployeeNumber");
		return employeeNumber;
	}

	public String prepopulateExensionAttr5(String branchOffice) {
		log.entering(super.getClass().getName(), "prepopulateExensionAttr5");
		log.fine("branchOffice=" + branchOffice);
		String ext5 = "";
		if ((!isNullOrEmpty(branchOffice)) && (branchOffice.contains("1")))
			ext5 = "W8-SUCC";
		else if ((!isNullOrEmpty(branchOffice)) && (branchOffice.contains("0"))) {
			ext5 = "W8";
		}
		log.fine("ext5=" + ext5);
		log.exiting(super.getClass().getName(), "prepopulateExensionAttr5");
		return ext5;
	}

	public String prepopulateO(String branchOffice) {
		log.entering(super.getClass().getName(), "prepopulateO");
		log.fine("branchOffice=" + branchOffice);
		String o = "";
		if ((!isNullOrEmpty(branchOffice)) && (branchOffice.contains("1")))
			o = "SUCC";
		else {
			o = "";
		}
		log.fine("o=" + o);
		log.exiting(super.getClass().getName(), "prepopulateO");
		return o;
	}

	public String prepopulateDescription(String countryCode, String status, Date startDate, Date endDate, String fnUsed,
			String lnUsed) {
		log.entering(super.getClass().getName(), "prepopulateDescription");
		log.fine("ADProvisoning/populateDescription countryCode =" + countryCode + ", status =" + status
				+ "startDate = " + startDate + "endDate =" + endDate + "fnUsed =" + fnUsed + "lnUsed =" + lnUsed);
		String description = null;
		String prefixdesc = null;
		String date = null;
		String strStartDate = null;
		String strEndDate = null;
		if (isNullOrEmpty(status))
			status = "";
		if (isNullOrEmpty(fnUsed))
			fnUsed = "";
		if (isNullOrEmpty(fnUsed))
			fnUsed = "";
		SimpleDateFormat sm = new SimpleDateFormat("dd-MM-yyyy");
		if (startDate != null)
			strStartDate = sm.format(startDate);
		else {
			log.fine("Start date is null");
		}
		if (endDate != null)
			strEndDate = sm.format(endDate);
		else {
			log.fine("End date is null");
		}
		if (isNullOrEmpty(countryCode)) {
			description = "";
		} else {
			if ("Disabled".equalsIgnoreCase(status))
				date = strEndDate;
			else {
				date = strStartDate;
			}
			if ("CA".equalsIgnoreCase(countryCode))
				prefixdesc = "649/C//BNC/";
			else if ("US".equalsIgnoreCase(countryCode))
				prefixdesc = "897/C//BNC/";
			else if ("GB".equalsIgnoreCase(countryCode))
				prefixdesc = "866/C//BNC/";
			else if ("CH".equalsIgnoreCase(countryCode))
				prefixdesc = "848/C//BNC/";
			else {
				prefixdesc = "999/??//BNC/";
			}
			description = prefixdesc + lnUsed + "." + fnUsed + " " + date;
		}
		log.fine("description=" + description);
		log.exiting(super.getClass().getName(), "prepopulateDescription");
		return description;
	}

	public String prepopulateSUCCOU(String workTransit, String provinceCode) {
		log.entering(super.getClass().getName(), "prepopulateSUCCOU");
		log.fine("ADProvisoning/prepopulateSUCCOU workTransit="
				+ workTransit + "provinceCode " + provinceCode);
		String succOU = null;
		String iscontain = "false";
		String[] arrProvCode = { "QC", "NB", "ON", "SK", "BC", "MB", "NE", "AB", "PE" };
		for (int i = 0; i < arrProvCode.length; ++i) {
			if (provinceCode.toLowerCase().contains(arrProvCode[i].toLowerCase())) {
				iscontain = "true";
			}
		}
		if ((isNullOrEmpty(workTransit)) || ("false".equalsIgnoreCase(iscontain))) {
			String errorMessage = "AD_SUCC_FAILED";
			throw new RuntimeException(errorMessage);
		}
		succOU = "OU=Utillisateurs,OU=" + provinceCode + "DC=succ,DC=reseau,DC=bnc,DC=ca";

		log.fine("succOU :" + succOU);
		log.exiting(super.getClass().getName(), "prepopulateSUCCOU");
		return succOU;
	}

	public String prepopulateRESOU(String PVP) throws tcAPIException {
		log.entering(super.getClass().getName(), "prepopulateRESOU");
		log.fine("ADProvisoning/prepopulateRESOU PVP=" + PVP);
		String resOU = "";
		if (isNullOrEmpty(PVP)) {
			//resOU = "ou=Users,ou=WKS-MIG,OU=W8Only,DC=res,DC=bngf,DC=local";
			resOU = "OU=users,OU=WKS-MIG,DC=RES,DC=local";
		} else {
			log.fine("PVP1 is not null: " + PVP);
			String PVPDecode = this.lookupOperationsIntf.getDecodedValueForEncodedValue("Lookup.BNC.PVP", PVP);
			log.fine("PVPDecode = " + PVPDecode);
			//resOU = "ou=Users,ou="+ PVPDecode +",OU=W8Only,DC=res,DC=bngf,DC=local";
			resOU = "OU=users,OU=" + PVPDecode + ",DC=RES,DC=local";
		}
		log.fine("resOU :" + resOU);
		log.exiting(super.getClass().getName(), "prepopulateRESOU");
		return resOU;
	}

	public String propagateChangesFromUserProfileToAD(String processInstanceKey, String userKey, String adAttribute,
			String itResourceName) throws NoSuchUserException, UserLookupException, SearchKeyNotUniqueException,
					AccessDeniedException, tcAPIException, tcInvalidValueException, tcNotAtomicProcessException,
					tcFormNotFoundException, tcRequiredDataMissingException, tcProcessNotFoundException {
		log.entering(super.getClass().getName(), "propagateChangesFromUserProfileToAD");
		log.fine("processInstanceKey= " + processInstanceKey + " userKey= " + userKey + " adAttribute= " + adAttribute
				+ "itResourceName = " + itResourceName);
		String response = "FAILURE";
		tcFormInstanceOperationsIntf formInstanceOperationsIntf = null;
		long processInstanceKeyL = Long.parseLong(processInstanceKey);
		HashMap<String, String> procFormHash = new HashMap<String, String>();
		formInstanceOperationsIntf = (tcFormInstanceOperationsIntf) Platform
				.getService(tcFormInstanceOperationsIntf.class);
		User user = OIMUtility.getUserProfile(userKey);
		if (itResourceName.toLowerCase().contains("lbg")) {
			if ((adAttribute.equalsIgnoreCase("lastNameUsed")) || (adAttribute.equalsIgnoreCase("firstNameUsed"))) {
				String displayName = populateDisplayName(userKey, user);
				procFormHash.put("UD_ADLBG_FULLNAME", displayName);
				if (adAttribute.equalsIgnoreCase("lastNameUsed")) {
					String lastName = populateLastName(userKey, user);
					procFormHash.put("UD_ADLBG_LNAME", lastName);
				}
				if (adAttribute.equalsIgnoreCase("firstNameUsed")) {
					String firstName = populateFirstName(userKey, user);
					procFormHash.put("UD_ADLBG_FNAME", firstName);
				}
			}
			if ((adAttribute.equalsIgnoreCase("worktansit")) || (adAttribute.equalsIgnoreCase("transitDescription"))) {
				log.fine("adAttribute= " + adAttribute);
				String workTransit = (String) user.getAttribute(Constants.UserAttributes.WORK_TRANSIT);
				String transitDesc = (String) user.getAttribute(Constants.UserAttributes.TRANSIT_DESCRIPTION);
				String company = prepopulateCompany(workTransit, transitDesc);
				String phyOfcName = prepopulatphyOfcName(workTransit, transitDesc);
				log.fine("company= " + company + " phyOfcName= " + phyOfcName);
				procFormHash.put("UD_ADLBG_COMPANY", company);
				procFormHash.put("UD_ADLBG_OFFICE", phyOfcName);
				if (adAttribute.equalsIgnoreCase("worktansit")) {
					String dept = prepopulateDepartment(userKey, user);
					procFormHash.put("UD_ADLBG_DEPARTMENT", dept);
				}
			}
			if ((adAttribute.equalsIgnoreCase("CountryCode")) || (adAttribute.equalsIgnoreCase("firstNameUsed"))
					|| (adAttribute.equalsIgnoreCase("lastNameUsed")) || (adAttribute.equalsIgnoreCase("status"))
					|| (adAttribute.equalsIgnoreCase("StartDate")) || (adAttribute.equalsIgnoreCase("EndDate"))) {
				String fnUsed = (String) user.getAttribute(Constants.UserAttributes.FirstNameUsed);
				String lnUsed = (String) user.getAttribute(Constants.UserAttributes.LastNameUsed);
				String countryCode = (String) user.getAttribute(Constants.UserAttributes.COUNTRY_CODE);
				Date startDate = (Date) user
						.getAttribute(UserManagerConstants.AttributeName.ACCOUNT_START_DATE.getId());
				Date endDate = (Date) user.getAttribute(UserManagerConstants.AttributeName.ACCOUNT_END_DATE.getId());
				String status = (String) user.getAttribute(UserManagerConstants.AttributeName.STATUS.getId());
				String description = prepopulateDescription(countryCode, status, startDate, endDate, fnUsed, lnUsed);
				if (adAttribute.equalsIgnoreCase("CountryCode")) {
					procFormHash.put("UD_ADLBG_COUNTRY", countryCode);
				}
				procFormHash.put("UD_ADLBG_DESCRIPTION", description);
			}
			if ((adAttribute.equalsIgnoreCase("officeStreetNo")) || (adAttribute.equalsIgnoreCase("officeStreetName"))
					|| (adAttribute.equalsIgnoreCase("siteName"))) {
				String ofcStreetNo = (String) user.getAttribute(Constants.UserAttributes.OFFICE_STREET_NUMBER);
				String ofcStreetName = (String) user.getAttribute(Constants.UserAttributes.OFFICE_STREET_NAME);
				String siteName = (String) user.getAttribute(Constants.UserAttributes.SITE_NAME);
				String streeAddress = prepopulateStreetAddress(ofcStreetNo, ofcStreetName, siteName);
				procFormHash.put("UD_ADLBG_STREET", streeAddress);
			}
			if (adAttribute.equalsIgnoreCase("officePhoneDirect")) {
				String ofcPhoneDirect = (String) user.getAttribute(Constants.UserAttributes.Office_Phone_Direct);
				String telephone = prepopulateTelephone(ofcPhoneDirect);
				procFormHash.put("UD_ADLBG_TELEPHONE", telephone);
			}
			if ((adAttribute.equalsIgnoreCase("prefLang")) || (adAttribute.equalsIgnoreCase("functionEN"))
					|| (adAttribute.equalsIgnoreCase("functionFR"))) {
				String prefLang = (String) user.getAttribute(Constants.UserAttributes.PREF_LANG);
				String funEN = (String) user.getAttribute(Constants.UserAttributes.Function_EN);
				String funFR = (String) user.getAttribute(Constants.UserAttributes.Function_FR);
				String title = prepopulateTitle(prefLang, funEN, funFR);
				procFormHash.put("UD_ADLBG_TITLE", title);
			}
		} else if (itResourceName.toLowerCase().contains("ad-res")) {
			
			if ((adAttribute.equalsIgnoreCase("lastNameUsed")) || (adAttribute.equalsIgnoreCase("firstNameUsed"))) {
				String displayName = populateDisplayName(userKey, user);
				procFormHash.put("UD_ADRES_FULLNAME", displayName);
				if (adAttribute.equalsIgnoreCase("lastNameUsed")) {
					String lastName = populateLastName(userKey, user);
					procFormHash.put("UD_ADRES_LNAME", lastName);
				}
				if (adAttribute.equalsIgnoreCase("firstNameUsed")) {
					String firstName = populateFirstName(userKey, user);
					procFormHash.put("UD_ADRES_FNAME", firstName);
				}
			}
			if ((adAttribute.equalsIgnoreCase("CountryCode")) || (adAttribute.equalsIgnoreCase("firstNameUsed"))
					|| (adAttribute.equalsIgnoreCase("lastNameUsed")) || (adAttribute.equalsIgnoreCase("status"))
					|| (adAttribute.equalsIgnoreCase("StartDate")) || (adAttribute.equalsIgnoreCase("EndDate"))) {
				String fnUsed = (String) user.getAttribute(Constants.UserAttributes.FirstNameUsed);
				String lnUsed = (String) user.getAttribute(Constants.UserAttributes.LastNameUsed);
				String countryCode = (String) user.getAttribute(Constants.UserAttributes.COUNTRY_CODE);
				Date startDate = (Date) user
						.getAttribute(UserManagerConstants.AttributeName.ACCOUNT_START_DATE.getId());
				Date endDate = (Date) user.getAttribute(UserManagerConstants.AttributeName.ACCOUNT_END_DATE.getId());
				String status = (String) user.getAttribute(UserManagerConstants.AttributeName.STATUS.getId());
				String description = prepopulateDescription(countryCode, status, startDate, endDate, fnUsed, lnUsed);
				if (adAttribute.equalsIgnoreCase("CountryCode")) {
					procFormHash.put("UD_ADRES_COUNTRY", countryCode);
				}
				procFormHash.put("UD_ADRES_DESCRIPTION", description);
			}
			if ((adAttribute.equalsIgnoreCase("worktansit")) || (adAttribute.equalsIgnoreCase("transitDescription"))) {
				log.fine("adAttribute= " + adAttribute);
				String workTransit = (String) user.getAttribute(Constants.UserAttributes.WORK_TRANSIT);
				String transitDesc = (String) user.getAttribute(Constants.UserAttributes.TRANSIT_DESCRIPTION);
				String company = prepopulateCompany(workTransit, transitDesc);
				String phyOfcName = prepopulatphyOfcName(workTransit, transitDesc);
				log.fine("company= " + company + " phyOfcName= " + phyOfcName);
				procFormHash.put("UD_ADRES_COMPANY", company);
				procFormHash.put("UD_ADRES_OFFICE", phyOfcName);
				if (adAttribute.equalsIgnoreCase("worktansit")) {
					String dept = prepopulateDepartment(userKey, user);
					procFormHash.put("UD_ADRES_DEPARTMENT", dept);
				}
			}
			if ((adAttribute.equalsIgnoreCase("officeStreetNo")) || (adAttribute.equalsIgnoreCase("officeStreetName"))
					|| (adAttribute.equalsIgnoreCase("siteName"))) {
				String ofcStreetNo = (String) user.getAttribute(Constants.UserAttributes.OFFICE_STREET_NUMBER);
				String ofcStreetName = (String) user.getAttribute(Constants.UserAttributes.OFFICE_STREET_NAME);
				String siteName = (String) user.getAttribute(Constants.UserAttributes.SITE_NAME);
				String streeAddress = prepopulateStreetAddress(ofcStreetNo, ofcStreetName, siteName);
				procFormHash.put("UD_ADRES_STREET", streeAddress);
			}
			if (adAttribute.equalsIgnoreCase("officePhoneDirect")) {
				String ofcPhoneDirect = (String) user.getAttribute(Constants.UserAttributes.Office_Phone_Direct);
				String telephone = prepopulateTelephone(ofcPhoneDirect);
				procFormHash.put("UD_ADRES_TELEPHONE", telephone);
			}
			if ((adAttribute.equalsIgnoreCase("prefLang")) || (adAttribute.equalsIgnoreCase("functionEN"))
					|| (adAttribute.equalsIgnoreCase("functionFR"))) {
				String prefLang = (String) user.getAttribute(Constants.UserAttributes.PREF_LANG);
				String funEN = (String) user.getAttribute(Constants.UserAttributes.Function_EN);
				String funFR = (String) user.getAttribute(Constants.UserAttributes.Function_FR);
				String title = prepopulateTitle(prefLang, funEN, funFR);
				procFormHash.put("UD_ADRES_TITLE", title);
			}
		} else if (itResourceName.toLowerCase().contains("succ")) {
			if (adAttribute.equalsIgnoreCase("userPrincipalName")) {
				String userPrincipalName = "";
				if (itResourceName.toLowerCase().contains("ad-res")) {
					userPrincipalName = (String) user.getAttribute("RESUserPrincipalName");
					procFormHash.put("UD_ADUSER_USERPRINCIPALNAME", userPrincipalName);
				}
			}
		}

		if ((procFormHash != null) && (procFormHash.size() > 0)) {
			formInstanceOperationsIntf.setProcessFormData(processInstanceKeyL, procFormHash);
			response = "SUCCESS";
		}
		log.exiting(super.getClass().getName(), "propagateChangesFromUserProfileToAD");
		return response;
	}

	public String prepopulateDepartment(String userKey, User user)
			throws NoSuchUserException, UserLookupException, SearchKeyNotUniqueException, AccessDeniedException {
		log.entering(super.getClass().getName(), "prepopulateDepartment");
		String workTransit = (String) user.getAttribute("WorkingTransit");
		if (isNullOrEmpty(workTransit)) {
			workTransit = "";
		}
		log.fine("workTransit=" + workTransit);
		log.exiting(super.getClass().getName(), "prepopulateDepartment");
		return workTransit;
	}

	public String prepopulateTitle(String prefLang, String funEN, String funFR)
			throws NoSuchUserException, UserLookupException, SearchKeyNotUniqueException, AccessDeniedException {
		log.entering(super.getClass().getName(), "prepopulateTitle");
		log.fine("prefLang= " + prefLang + " funEN= " + funEN + " funFR= " + funFR);
		String title = null;

		if (!isNullOrEmpty(prefLang)) {
			if ("fr".equalsIgnoreCase(prefLang.toLowerCase())) {
				if (isNullOrEmpty(funFR))
					title = "";
				else {
					title = funFR;
				}
			} else if (isNullOrEmpty(funEN))
				title = "";
			else {
				title = funEN;
			}
		}
		log.fine("title=" + title);
		log.exiting(super.getClass().getName(), "prepopulateTitle");
		return title;
	}

	public String prepopulateTelephone(String ofcPhoneDirect)
			throws NoSuchUserException, UserLookupException, SearchKeyNotUniqueException, AccessDeniedException {
		log.entering(super.getClass().getName(), "prepopulateTelephone");
		log.fine("ofcPhoneDirect= " + ofcPhoneDirect);
		String telephone = null;
		if (!isNullOrEmpty(ofcPhoneDirect)) {
			telephone = "+1." + ofcPhoneDirect.substring(0, 3) + "." + ofcPhoneDirect.substring(3, 6) + "."
					+ ofcPhoneDirect.substring(6);
		}
		log.fine("telephone=" + telephone);
		log.exiting(super.getClass().getName(), "prepopulateTelephone");
		return telephone;
	}

	public String prepopulateStreetAddress(String ofcStreetNo, String ofcStreetName, String siteName)
			throws NoSuchUserException, UserLookupException, SearchKeyNotUniqueException, AccessDeniedException {
		log.entering(super.getClass().getName(), "prepopulateStreetAddress");
		log.fine("ofcStreetNo = " + ofcStreetNo + "ofcStreetName =" + ofcStreetName + "siteName =" + siteName);
		String streetAddress = null;
		if (isNullOrEmpty(ofcStreetNo)) {
			ofcStreetNo = "";
		}
		if (isNullOrEmpty(ofcStreetName)) {
			ofcStreetName = "";
		}
		if (isNullOrEmpty(siteName)) {
			siteName = "";
		}
		streetAddress = ofcStreetNo + " " + ofcStreetName + " " + siteName;
		log.fine("streetAddress=" + streetAddress);
		log.exiting(super.getClass().getName(), "prepopulateStreetAddress");
		return streetAddress;
	}

	public String prepopulatphyOfcName(String workTransit, String transitDesc) {
		log.entering(super.getClass().getName(), "prepopulatphyOfcName");
		log.fine("workTransit = " + workTransit + "transitDesc =" + transitDesc);
		String phyOfcName = null;
		if (isNullOrEmpty(workTransit)) {
			workTransit = "";
		}
		if (isNullOrEmpty(transitDesc)) {
			transitDesc = "";
		}
		phyOfcName = workTransit + "|" + transitDesc;
		log.fine("phyOfcName=" + phyOfcName);
		log.exiting(super.getClass().getName(), "prepopulatphyOfcName");
		return phyOfcName;
	}

	public String prepopulateCompany(String workTransit, String transitDesc)
			throws NoSuchUserException, UserLookupException, SearchKeyNotUniqueException, AccessDeniedException {
		log.entering(super.getClass().getName(), "prepopulateCompany");
		log.fine("workTransit = " + workTransit + "transitDesc =" + transitDesc);
		String company = null;
		if (isNullOrEmpty(workTransit)) {
			workTransit = "";
		}
		if (isNullOrEmpty(transitDesc)) {
			transitDesc = "";
		}
		company = "BNC/" + workTransit + "-" + transitDesc;
		log.fine("company=" + company);
		log.exiting(super.getClass().getName(), "prepopulateCompany");
		return company;
	}

	public String populateDisplayName(String userKey, User user)
			throws NoSuchUserException, UserLookupException, SearchKeyNotUniqueException, AccessDeniedException {
		log.entering(super.getClass().getName(), "populateDisplayName");
		String fnUsed = null;
		String lnUsed = null;
		String displayName = null;
		fnUsed = (String) user.getAttribute(Constants.UserAttributes.FirstNameUsed);
		lnUsed = (String) user.getAttribute(Constants.UserAttributes.LastNameUsed);
		if (isNullOrEmpty(fnUsed)) {
			fnUsed = "";
		}
		if (isNullOrEmpty(lnUsed)) {
			lnUsed = "";
		}
		displayName = lnUsed + "," + fnUsed;
		log.fine("displayName=" + displayName);
		log.exiting(super.getClass().getName(), "populateDisplayName");
		return displayName;
	}

	public String prepopulateDisplayName(String firstNameUsed, String lastNameUsed)
			throws NoSuchUserException, UserLookupException, SearchKeyNotUniqueException, AccessDeniedException {
		log.entering(super.getClass().getName(), "populateDisplayName");
		log.fine("firstNameUsed = " + firstNameUsed + "lastNameUsed =" + lastNameUsed);
		if (isNullOrEmpty(firstNameUsed)) {
			firstNameUsed = "";
		}
		if (isNullOrEmpty(lastNameUsed)) {
			lastNameUsed = "";
		}
		String displayName = lastNameUsed + "," + firstNameUsed;
		log.fine("displayName=" + displayName);
		log.exiting(super.getClass().getName(), "populateDisplayName");
		return displayName;
	}

	private String populateLastName(String userKey, User user)
			throws NoSuchUserException, UserLookupException, SearchKeyNotUniqueException, AccessDeniedException {
		log.entering(super.getClass().getName(), "populateLastName");
		String lnUsed = null;
		lnUsed = (String) user.getAttribute(Constants.UserAttributes.LastNameUsed);
		if (isNullOrEmpty(lnUsed)) {
			lnUsed = "";
		}
		log.exiting(super.getClass().getName(), "populateLastName");
		return lnUsed;
	}

	private String populateFirstName(String userKey, User user)
			throws NoSuchUserException, UserLookupException, SearchKeyNotUniqueException, AccessDeniedException {
		log.entering(super.getClass().getName(), "populateFirstName");
		String fnUsed = null;
		fnUsed = (String) user.getAttribute(Constants.UserAttributes.FirstNameUsed);
		if (isNullOrEmpty(fnUsed)) {
			fnUsed = "";
		}
		log.fine("fnUsed=" + fnUsed);
		log.exiting(super.getClass().getName(), "populateFirstName");
		return fnUsed;
	}

	private boolean isNullOrEmpty(String strCheck) {
		return (strCheck == null) || (strCheck.equals("null")) || (strCheck.trim().length() == 0);
	}
}