package com.icsynergy.bnc.adapters;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.thortech.xl.crypto.tcCryptoException;
import Thor.API.Exceptions.tcAPIException;
import Thor.API.Exceptions.tcColumnNotFoundException;
import Thor.API.Exceptions.tcFormNotFoundException;

import Thor.API.Exceptions.tcInvalidValueException;
import Thor.API.Exceptions.tcLoginAttemptsExceededException;
import Thor.API.Exceptions.tcNotAtomicProcessException;
import Thor.API.Exceptions.tcPasswordResetAttemptsExceededException;
import Thor.API.Exceptions.tcProcessNotFoundException;
import Thor.API.Exceptions.tcRequiredDataMissingException;
import Thor.API.Exceptions.tcUserAccountDisabledException;
import Thor.API.Exceptions.tcUserAccountInvalidException;
import Thor.API.Exceptions.tcUserAlreadyLoggedInException;
import Thor.API.Exceptions.tcVersionNotFoundException;
import Thor.API.Operations.tcFormInstanceOperationsIntf;
import Thor.API.Operations.tcITResourceInstanceOperationsIntf;
import Thor.API.Operations.tcLookupOperationsIntf;
import oracle.iam.identity.exception.AccessDeniedException;
import oracle.iam.identity.exception.NoSuchUserException;
import oracle.iam.identity.exception.SearchKeyNotUniqueException;
import oracle.iam.identity.exception.UserLookupException;
import oracle.iam.identity.usermgmt.api.UserManagerConstants;
import oracle.iam.identity.usermgmt.vo.User;
import oracle.iam.platform.Platform;

import com.icsynergy.bnc.Constants;
import com.icsynergy.bnc.OIMUtility;

public class ADProvisioning {
	final private static Logger log = Logger.getLogger("com.icsynergy");
	private final String CONFIGLOOKUP = "Lookup.BNC.AD.Config";
	private tcLookupOperationsIntf lookupOperationsIntf = Platform.getService(tcLookupOperationsIntf.class);

	// Lookup.BNC.AD.Config

	/**
	 * This method updates the process form for multiple attributes before
	 * create user in AD.
	 * 
	 * @param processInstanceKey
	 * @param strUserkey
	 * @return
	 */
	public String preActionsOnADCreateUser(String processInstanceKey, String strUserkey) {
		log.entering(getClass().getName(), "preActionsOnADCreateUser");

		long processInstanceKeyL = Long.parseLong(processInstanceKey);
		tcFormInstanceOperationsIntf formInstanceOperationsIntf = null;
		long userKeyL = Long.parseLong(strUserkey);
		HashMap<String, String> procFormHash = new HashMap<String, String>();
		String response = "FAILURE";
		try {
			formInstanceOperationsIntf = Platform.getService(tcFormInstanceOperationsIntf.class);
			String distinguishName = populateDistinguishedName(processInstanceKeyL, userKeyL);
			String empNo = populateEmpplyeeNumber(userKeyL);
			String displayName = populateDisplayName(userKeyL);
			String description = populateDescription(userKeyL);
			String company = populateCompany(userKeyL);
			String phyOfcName = populatphyOfcName(userKeyL);
			String streeAddress = populateStreetAddress(userKeyL);
			String title = populateTitle(userKeyL);
			String telephone = populateTelephone(userKeyL);

			procFormHash.put("UD_ADUSER_DISTINGUISHEDNAME", distinguishName);
			procFormHash.put("UD_ADUSER_EMPLOYEENUMBER", empNo);
			procFormHash.put("UD_ADUSER_FULLNAME", displayName);
			procFormHash.put("UD_ADUSER_DESCRIPTION", description);
			procFormHash.put("UD_ADUSER_COMPANY", company);
			procFormHash.put("UD_ADUSER_OFFICE", phyOfcName);
			procFormHash.put("UD_ADUSER_STREET", streeAddress);
			procFormHash.put("UD_ADUSER_TELEPHONE", telephone);
			procFormHash.put("UD_ADUSER_TITLE", title);

			if (procFormHash != null && procFormHash.size() > 0)
				formInstanceOperationsIntf.setProcessFormData(processInstanceKeyL, procFormHash);
			log.exiting("ADProvisoning", "populateDistinguishedName");
			response = "SUCCESS";
		} catch (Exception e) {
			log.log(Level.SEVERE, "preActionsOnADCreateUser", e);

		}
		log.exiting(getClass().getName(), "preActionsOnADCreateUser");
		return response;
	}

	/**
	 * This method populate Description according to Country Type, Status,
	 * StartDate, End Date, First Name Used and Last Name Used.
	 * 
	 * @param userKeyL
	 * @return
	 * @throws NoSuchUserException
	 * @throws UserLookupException
	 * @throws SearchKeyNotUniqueException
	 * @throws AccessDeniedException
	 * @throws ParseException
	 */
	private String populateDescription(long userKeyL) throws NoSuchUserException, UserLookupException,
			SearchKeyNotUniqueException, AccessDeniedException, ParseException {
		String description = null;
		String prefixdesc = null;
		String date = null;
		String strStartDate = null;
		String strEndDate = null;
		String strUserkey = String.valueOf(userKeyL);
		User user = OIMUtility.getUserProfile(strUserkey);
		String cc = (String) user.getAttribute(Constants.UserAttributes.COUNTRY_CODE);
		String status = (String) user.getAttribute(UserManagerConstants.AttributeName.STATUS.getName());
		log.fine("status=" + status);

		Date startDate = (Date) user.getAttribute(UserManagerConstants.AttributeName.ACCOUNT_START_DATE.getId());
		Date endDate = (Date) user.getAttribute(UserManagerConstants.AttributeName.ACCOUNT_END_DATE.getId());
		String fnUsed = (String) user.getAttribute(Constants.UserAttributes.FirstNameUsed);
		String lnUsed = (String) user.getAttribute(Constants.UserAttributes.LastNameUsed);
		log.fine("startDate=" + startDate + ", entdDate=" + endDate);

		if (isNullOrEmpty(status))
			status = "";
		if (isNullOrEmpty(fnUsed))
			fnUsed = "";
		if (isNullOrEmpty(fnUsed))
			fnUsed = "";
		SimpleDateFormat sm = new SimpleDateFormat("dd-MM-yyyy");
		if (startDate != null) {
			strStartDate = sm.format(startDate);
		} else {
			log.fine("Start date is null");
		}
		if (endDate != null) {
			strEndDate = sm.format(endDate);
		} else {
			log.fine("End date is null");
		}
		if (isNullOrEmpty(cc)) {
			description = "";
		} else {
			if ("Disabled".equalsIgnoreCase(status)) {
				date = strEndDate;
			} else {
				date = strStartDate;
			}
			if ("CA".equalsIgnoreCase(cc)) {
				prefixdesc = "649/C/BNC/";
			} else if ("US".equalsIgnoreCase(cc)) {
				prefixdesc = "897/C/BNC/";
			} else if ("GB".equalsIgnoreCase(cc)) {
				prefixdesc = "866/C/BNC/";
			} else if ("CH".equalsIgnoreCase(cc)) {
				prefixdesc = "848/C/BNC/";
			} else {
				prefixdesc = "999/??/BNC/";
			}
			description = prefixdesc + lnUsed + "." + fnUsed + " " + date;
		}
		log.exiting("ADProvisoning", "populateDistinguishedName");
		return description;
	}

	/**
	 * This method updates the process form for multiple attributes.
	 * 
	 * @param procInstKey
	 * @param userKey
	 * @return
	 * @throws tcCryptoException
	 * @throws tcAPIException
	 * @throws tcUserAccountDisabledException
	 * @throws tcPasswordResetAttemptsExceededException
	 * @throws tcLoginAttemptsExceededException
	 * @throws tcUserAccountInvalidException
	 * @throws tcUserAlreadyLoggedInException
	 * @throws tcProcessNotFoundException
	 * @throws tcFormNotFoundException
	 * @throws tcNotAtomicProcessException
	 * @throws tcVersionNotFoundException
	 * @throws tcInvalidValueException
	 * @throws tcRequiredDataMissingException
	 * @throws tcColumnNotFoundException
	 * @throws NoSuchUserException
	 * @throws UserLookupException
	 * @throws SearchKeyNotUniqueException
	 * @throws AccessDeniedException
	 */
	private String populateDistinguishedName(long procInstKey, long userKey) throws tcCryptoException, tcAPIException,
			tcUserAccountDisabledException, tcPasswordResetAttemptsExceededException, tcLoginAttemptsExceededException,
			tcUserAccountInvalidException, tcUserAlreadyLoggedInException, tcProcessNotFoundException,
			tcFormNotFoundException, tcNotAtomicProcessException, tcVersionNotFoundException, tcInvalidValueException,
			tcRequiredDataMissingException, tcColumnNotFoundException, NoSuchUserException, UserLookupException,
			SearchKeyNotUniqueException, AccessDeniedException {

		log.entering(getClass().getName(), "populateDistinguishedName");
		tcFormInstanceOperationsIntf formInstanceOperationsIntf = null;
		tcITResourceInstanceOperationsIntf itResourceinstanceOperationsIntf = null;
		formInstanceOperationsIntf = Platform.getService(tcFormInstanceOperationsIntf.class);
		itResourceinstanceOperationsIntf = Platform.getService(tcITResourceInstanceOperationsIntf.class);

		log.fine("ADProvisoning/populateDistinguishedName procInstKey=" + procInstKey + ", userKey=" + userKey);
		String itResourceName = OIMUtility.getITResourceNameFromUserProcesssForm(formInstanceOperationsIntf,
				itResourceinstanceOperationsIntf, procInstKey);
		String strUserkey = String.valueOf(userKey);
		log.fine("itResourceName1" + itResourceName);
		String userLogin = null;
		String PVP = null;
		String distinguishedName = null;

		User user = OIMUtility.getUserProfile(strUserkey);
		userLogin = (String) user.getAttribute(UserManagerConstants.AttributeName.USER_LOGIN.getId());
		log.fine("userLogin" + userLogin);
		PVP = (String) user.getAttribute("PVP");

		if (itResourceName.equalsIgnoreCase("AD-RES Main IT Resource")) {
			String ou = lookupOperationsIntf.getDecodedValueForEncodedValue(CONFIGLOOKUP, "AD_RES");

			if (PVP.isEmpty()) {
				distinguishedName = "cn=" + userLogin + ou;
				// "ou=Users,ou= WKS-MIG,OU=W8Only,DC=res,DC=bngf,DC=local";
			} else {
			}
		} else if (itResourceName.equalsIgnoreCase("AD-SUCC Main IT Resource")) {
			String ou = lookupOperationsIntf.getDecodedValueForEncodedValue(CONFIGLOOKUP, "AD_SUCC");

			distinguishedName = "cn=" + userLogin + ou;
			// "ou=Users,ou= WKS-MIG,OU=W8Only,DC=res,DC=bngf,DC=local";

		} else if (itResourceName.equalsIgnoreCase("AD-LBG Main IT Resource")) {

			String ou = lookupOperationsIntf.getDecodedValueForEncodedValue(CONFIGLOOKUP, "AD_LBG");
			log.fine("ou " + ou);
			if (isNullOrEmpty(ou)) {
				log.fine("Missing entry for OU ");
				distinguishedName = "";
			} else {
				distinguishedName = "CN=" + userLogin + "," + ou;
				// "ou=Users,ou= LinkedMailboxes,ou=Internal,dc=nbfg, dc=ca";
			}
			log.fine("distinguishedName :" + distinguishedName);
		}
		log.exiting("ADProvisoning", "populateDistinguishedName");
		return distinguishedName;
	}

	/**
	 * This method populates employee number according to account ID.
	 * 
	 * @param userKeyL
	 * @return
	 * @throws NoSuchUserException
	 * @throws UserLookupException
	 * @throws SearchKeyNotUniqueException
	 * @throws AccessDeniedException
	 */
	private String populateEmpplyeeNumber(long userKeyL)
			throws NoSuchUserException, UserLookupException, SearchKeyNotUniqueException, AccessDeniedException {
		String accountID = null;

		String strUserkey = String.valueOf(userKeyL);
		User user = OIMUtility.getUserProfile(strUserkey);
		accountID = (String) user.getAttribute(Constants.UserAttributes.AccountId);
		log.fine("accountID" + accountID);
		if (isNullOrEmpty(accountID)) {
			return "";
		} else {
			return accountID + "-l";
		}
	}

	/**
	 * This method update the process form according to given attribute.
	 * 
	 * @param processInstanceKey
	 * @param userKey
	 * @param adAttribute
	 * @return
	 * @throws NoSuchUserException
	 * @throws UserLookupException
	 * @throws SearchKeyNotUniqueException
	 * @throws AccessDeniedException
	 * @throws tcAPIException
	 * @throws tcInvalidValueException
	 * @throws tcNotAtomicProcessException
	 * @throws tcFormNotFoundException
	 * @throws tcRequiredDataMissingException
	 * @throws tcProcessNotFoundException
	 * @throws ParseException
	 */
	public String propagateChangesFromUserProfileToAD(String processInstanceKey, String userKey, String adAttribute)
			throws NoSuchUserException, UserLookupException, SearchKeyNotUniqueException, AccessDeniedException,
			tcAPIException, tcInvalidValueException, tcNotAtomicProcessException, tcFormNotFoundException,
			tcRequiredDataMissingException, tcProcessNotFoundException, ParseException {
		String response = "FAILURE";
		tcFormInstanceOperationsIntf formInstanceOperationsIntf = null;
		long processInstanceKeyL = Long.parseLong(processInstanceKey);

		HashMap<String, String> procFormHash = new HashMap<String, String>();
		long userKeyL = Long.parseLong(userKey);
		formInstanceOperationsIntf = Platform.getService(tcFormInstanceOperationsIntf.class);

		if (adAttribute.equalsIgnoreCase("lastNameUsed") || adAttribute.equalsIgnoreCase("firstNameUsed")) {
			String displayName = populateDisplayName(userKeyL);
			procFormHash.put("UD_ADUSER_FULLNAME", displayName);
			if (adAttribute.equalsIgnoreCase("lastNameUsed")) {
				String lastName = populateLastName(userKeyL);
				procFormHash.put("UD_ADUSER_LNAME", lastName);
			}
			if (adAttribute.equalsIgnoreCase("firstNameUsed")) {
				String firstName = populateFirstName(userKeyL);
				procFormHash.put("UD_ADUSER_FNAME", firstName);
			}
		}

		if (adAttribute.equalsIgnoreCase("worktansit") || adAttribute.equalsIgnoreCase("transitDescription")) {
			String company = populateCompany(userKeyL);
			String phyOfcName = populatphyOfcName(userKeyL);

			procFormHash.put("UD_ADUSER_COMPANY", company);
			procFormHash.put("UD_ADUSER_OFFICE", phyOfcName);
			if (adAttribute.equalsIgnoreCase("worktansit")) {
				String dept = populateDepartmrnt(userKeyL);
				procFormHash.put("UD_ADUSER_DEPARTMENT", dept);
			}

		}

		if (adAttribute.equalsIgnoreCase("officeStreetNo") || adAttribute.equalsIgnoreCase("officeStreetName")
				|| adAttribute.equalsIgnoreCase("siteName")) {
			String streeAddress = populateStreetAddress(userKeyL);
			procFormHash.put("UD_ADUSER_STREET", streeAddress);
		}

		if (adAttribute.equalsIgnoreCase("officePhoneDirect")) {
			String telephone = populateTelephone(userKeyL);
			procFormHash.put("UD_ADUSER_TELEPHONE", telephone);
		}

		if (adAttribute.equalsIgnoreCase("prefLang") || adAttribute.equalsIgnoreCase("functionEN")
				|| adAttribute.equalsIgnoreCase("functionFR")) {
			String title = populateTitle(userKeyL);
			procFormHash.put("UD_ADUSER_TITLE", title);
		}

		if (adAttribute.equalsIgnoreCase("CountryCode") || adAttribute.equalsIgnoreCase("FirtsNameUsed")
				|| adAttribute.equalsIgnoreCase("LASTNameUsed") || adAttribute.equalsIgnoreCase("status")
				|| adAttribute.equalsIgnoreCase("StartDate") || adAttribute.equalsIgnoreCase("EndDate")) {

			String description = populateDescription(userKeyL);
			procFormHash.put("UD_ADUSER_DESCRIPTION", description);
		}
		if (procFormHash != null && procFormHash.size() > 0) {
			formInstanceOperationsIntf.setProcessFormData(processInstanceKeyL, procFormHash);
			response = "SUCCESS";
		}
		return response;
	}

	private String populateDepartmrnt(long userKeyL)
			throws NoSuchUserException, UserLookupException, SearchKeyNotUniqueException, AccessDeniedException {

		String strUserkey = String.valueOf(userKeyL);
		User user = OIMUtility.getUserProfile(strUserkey);
		String workTransit = (String) user.getAttribute(Constants.UserAttributes.WORK_TRANSIT);

		return workTransit;

	}

	/**
	 * This function returns true is given string is empty or null.
	 * 
	 * @param strCheck
	 * @return
	 */
	private boolean isNullOrEmpty(String strCheck) {
		return (strCheck == null) || strCheck.equals("null") || strCheck.trim().length() == 0;
	}

	/**
	 * This method populate Title according to Locale, Function EN and Function
	 * FR.
	 * 
	 * @param userKeyL
	 * @return
	 * @throws NoSuchUserException
	 * @throws UserLookupException
	 * @throws SearchKeyNotUniqueException
	 * @throws AccessDeniedException
	 */
	private String populateTitle(long userKeyL)
			throws NoSuchUserException, UserLookupException, SearchKeyNotUniqueException, AccessDeniedException {
		String title = null;
		String strUserkey = String.valueOf(userKeyL);
		User user = OIMUtility.getUserProfile(strUserkey);
		String prefLang = (String) user.getAttribute(Constants.UserAttributes.PREF_LANG);
		String funEN = (String) user.getAttribute(Constants.UserAttributes.Function_EN);
		String funFR = (String) user.getAttribute(Constants.UserAttributes.Function_FR);

		if (!isNullOrEmpty(prefLang)) {
			if ("fr".equalsIgnoreCase(prefLang.toLowerCase())) {
				if (isNullOrEmpty(funFR)) {
					title = "";
				} else {
					title = funFR;
				}
			} else {
				if (isNullOrEmpty(funEN)) {
					title = "";
				} else {
					title = funEN;
				}
			}
		}
		return title;
	}

	/**
	 * This method populates telephome.
	 * 
	 * @param userKeyL
	 * @return
	 * @throws NoSuchUserException
	 * @throws UserLookupException
	 * @throws SearchKeyNotUniqueException
	 * @throws AccessDeniedException
	 */
	private String populateTelephone(long userKeyL)
			throws NoSuchUserException, UserLookupException, SearchKeyNotUniqueException, AccessDeniedException {
		String telephone = null;
		String strUserkey = String.valueOf(userKeyL);
		User user = OIMUtility.getUserProfile(strUserkey);
		String ofcPhoneDirect = (String) user.getAttribute(Constants.UserAttributes.Office_Phone_Direct);// ======
		if (!isNullOrEmpty(ofcPhoneDirect)) {
			telephone = "+1." + ofcPhoneDirect.substring(0, 3) + "." + ofcPhoneDirect.substring(3, 6) + "."
					+ ofcPhoneDirect.substring(6);
		}
		return telephone;
	}

	/**
	 * This method populate Street Address according to Office Street Number,
	 * Office Street Name and Site Name.
	 * 
	 * @param userKeyL
	 * @return
	 * @throws NoSuchUserException
	 * @throws UserLookupException
	 * @throws SearchKeyNotUniqueException
	 * @throws AccessDeniedException
	 */
	private String populateStreetAddress(long userKeyL)
			throws NoSuchUserException, UserLookupException, SearchKeyNotUniqueException, AccessDeniedException {
		String streetAddress = null;
		String strUserkey = String.valueOf(userKeyL);
		User user = OIMUtility.getUserProfile(strUserkey);
		String ofcStreetNo = (String) user.getAttribute(Constants.UserAttributes.OFFICE_STREET_NUMBER);
		String ofcStreetName = (String) user.getAttribute(Constants.UserAttributes.OFFICE_STREET_NAME);
		String siteName = (String) user.getAttribute(Constants.UserAttributes.SITE_NAME);

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

		return streetAddress;
	}

	/**
	 * This method populate Physical Delivery Office Name according to work
	 * transit and transit description.
	 * 
	 * @param userKeyL
	 * @return
	 * @throws NoSuchUserException
	 * @throws UserLookupException
	 * @throws SearchKeyNotUniqueException
	 * @throws AccessDeniedException
	 */
	private String populatphyOfcName(long userKeyL)
			throws NoSuchUserException, UserLookupException, SearchKeyNotUniqueException, AccessDeniedException {
		String phyOfcName = null;
		String strUserkey = String.valueOf(userKeyL);
		User user = OIMUtility.getUserProfile(strUserkey);
		String workTransit = (String) user.getAttribute(Constants.UserAttributes.WORK_TRANSIT);
		String transitDesc = (String) user.getAttribute(Constants.UserAttributes.TRANSIT_DESCRIPTION);
		if (isNullOrEmpty(workTransit)) {
			workTransit = "";
		}
		if (isNullOrEmpty(transitDesc)) {
			transitDesc = "";
		}
		phyOfcName = workTransit + "|" + transitDesc;
		return phyOfcName;
	}

	/**
	 * This method populate Company according to work transit and transit
	 * description.
	 * 
	 * @param userKeyL
	 * @return
	 * @throws NoSuchUserException
	 * @throws UserLookupException
	 * @throws SearchKeyNotUniqueException
	 * @throws AccessDeniedException
	 */
	private String populateCompany(long userKeyL)
			throws NoSuchUserException, UserLookupException, SearchKeyNotUniqueException, AccessDeniedException {
		String company = null;
		String strUserkey = String.valueOf(userKeyL);
		User user = OIMUtility.getUserProfile(strUserkey);
		String workTransit = (String) user.getAttribute(Constants.UserAttributes.WORK_TRANSIT);
		String transitDesc = (String) user.getAttribute(Constants.UserAttributes.TRANSIT_DESCRIPTION);
		if (isNullOrEmpty(workTransit)) {
			workTransit = "";
		}
		if (isNullOrEmpty(transitDesc)) {
			transitDesc = "";
		}
		company = "BNC/" + workTransit + "-" + transitDesc;
		return company;
	}

	/**
	 * This method populate Display name according to First Name Used and Last
	 * Name Used.
	 * 
	 * @param userKeyL
	 * @return
	 * @throws NoSuchUserException
	 * @throws UserLookupException
	 * @throws SearchKeyNotUniqueException
	 * @throws AccessDeniedException
	 */
	private String populateDisplayName(long userKeyL)
			throws NoSuchUserException, UserLookupException, SearchKeyNotUniqueException, AccessDeniedException {
		String fnUsed = null;
		String lnUsed = null;
		String displayName = null;
		String strUserkey = String.valueOf(userKeyL);
		User user = OIMUtility.getUserProfile(strUserkey);
		fnUsed = (String) user.getAttribute(Constants.UserAttributes.FirstNameUsed);
		lnUsed = (String) user.getAttribute(Constants.UserAttributes.LastNameUsed);
		if (isNullOrEmpty(fnUsed)) {
			fnUsed = "";
		}
		if (isNullOrEmpty(lnUsed)) {
			lnUsed = "";
		}
		displayName = lnUsed + "," + fnUsed;
		return displayName;
	}

	/**
	 * This method populate Display name according to First Name Used and Last
	 * Name Used.
	 * 
	 * @param userKeyL
	 * @return
	 * @throws NoSuchUserException
	 * @throws UserLookupException
	 * @throws SearchKeyNotUniqueException
	 * @throws AccessDeniedException
	 */
	private String populateLastName(long userKeyL)
			throws NoSuchUserException, UserLookupException, SearchKeyNotUniqueException, AccessDeniedException {
		String lnUsed = null;
		String strUserkey = String.valueOf(userKeyL);
		User user = OIMUtility.getUserProfile(strUserkey);
		lnUsed = (String) user.getAttribute(Constants.UserAttributes.LastNameUsed);
		return lnUsed;
	}

	/**
	 * This method populate Display name according to First Name Used and Last
	 * Name Used.
	 * 
	 * @param userKeyL
	 * @return
	 * @throws NoSuchUserException
	 * @throws UserLookupException
	 * @throws SearchKeyNotUniqueException
	 * @throws AccessDeniedException
	 */
	private String populateFirstName(long userKeyL)
			throws NoSuchUserException, UserLookupException, SearchKeyNotUniqueException, AccessDeniedException {
		String fnUsed = null;
		String strUserkey = String.valueOf(userKeyL);
		User user = OIMUtility.getUserProfile(strUserkey);
		fnUsed = (String) user.getAttribute(Constants.UserAttributes.FirstNameUsed);
		return fnUsed;
	}

}
