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
	private tcLookupOperationsIntf lookupOperationsIntf = Platform.getService(tcLookupOperationsIntf.class);
	tcFormInstanceOperationsIntf formInstanceOperationsIntf = Platform.getService(tcFormInstanceOperationsIntf.class);;
	tcITResourceInstanceOperationsIntf itResourceinstanceOperationsIntf = Platform
			.getService(tcITResourceInstanceOperationsIntf.class);

	// Lookup.BNC.AD.Config

	/**
	 * This method updates the process form for multiple attributes before
	 * create user in AD.
	 * 
	 * @param processInstanceKey
	 * @param strUserkey
	 * @return
	 * @throws AccessDeniedException
	 * @throws SearchKeyNotUniqueException
	 * @throws UserLookupException
	 * @throws NoSuchUserException
	 */
	public String preActionsOnADCreateUser(String processInstanceKey, String strUserkey, String itResourceName)
			throws NoSuchUserException, UserLookupException, SearchKeyNotUniqueException, AccessDeniedException {
		log.entering(getClass().getName(), "preActionsOnADCreateUser");
		log.fine("processInstanceKey =" + processInstanceKey + "strUserkey = " + strUserkey + "itResourceName =" + itResourceName);
		long processInstanceKeyL = Long.parseLong(processInstanceKey);
		tcFormInstanceOperationsIntf formInstanceOperationsIntf = null;
		formInstanceOperationsIntf = Platform.getService(tcFormInstanceOperationsIntf.class);
		// long userKeyL = Long.parseLong(strUserkey);
		HashMap<String, String> procFormHash = new HashMap<String, String>();
		String response = "FAILURE";
		String distinguishName = "";
		String sAMAccountName = "";
		String userPrincipalName = "";
		String ext8 = "";
		String ext5 = "";
		String ext9 = "";
		String o = "";
		String businessCategory = "";
		String empNo = "";
		User user = OIMUtility.getUserProfile(strUserkey);
		
		try {
			if (itResourceName.toLowerCase().contains("succ")) {
				sAMAccountName = populatesAMAccountName(processInstanceKeyL, strUserkey, user);
				userPrincipalName = polpulateUserPrincipalNAme(processInstanceKeyL, strUserkey, user);
				empNo = populateEmployeeNumber(strUserkey, user);
				log.fine("sAMAccountName " + sAMAccountName + "userPrincipalName " + userPrincipalName + "empNo "
						+ empNo);

				procFormHash.put("UD_ADUSER_UID", sAMAccountName);
				procFormHash.put("UD_ADUSER_USERPRINCIPALNAME", userPrincipalName);
				procFormHash.put("UD_ADUSER_EMPLOYEENUMBER", empNo);

			}
			if (itResourceName.toLowerCase().contains("ad-res")) {
				log.fine("Commited code");
				ext8 = populateExensionAttr8(processInstanceKeyL, strUserkey, user);
				ext5 = populateExensionAttr5(processInstanceKeyL, strUserkey, user);
				ext9 = populateExensionAttr9(processInstanceKeyL, strUserkey);
				o = populateO(processInstanceKeyL, strUserkey, user);
				businessCategory = populateBusinessCategory(processInstanceKeyL, strUserkey, user);
				empNo = populateEmployeeNumber(strUserkey, user);
				procFormHash.put("UD_ADUSER_EXT9", ext9);
				procFormHash.put("UD_ADUSER_EXT5", ext5);
				procFormHash.put("UD_ADUSER_EXT8", ext8);
				procFormHash.put("UD_ADUSER_O", o);
				procFormHash.put("UD_ADUSER_BUSINESSCATEGORY", businessCategory);
				procFormHash.put("UD_ADUSER_EMPLOYEENUMBER", empNo);
			}
			if (itResourceName.toLowerCase().contains("lbg")) {
				empNo = populateEmployeeNumber(strUserkey, user);
				procFormHash.put("UD_ADLBG_EMPLOYEENUMBER", empNo + "-l");
				
			}
			distinguishName = populateDistinguishedName(processInstanceKeyL, strUserkey, itResourceName, user);
			log.fine("distinguishName" + distinguishName);
			procFormHash.put("UD_ADUSER_DISTINGUISHEDNAME", distinguishName);
			if (procFormHash != null && procFormHash.size() > 0)
				formInstanceOperationsIntf.setProcessFormData(processInstanceKeyL, procFormHash);
			response = "SUCCESS";
		} catch (Exception e) {
			String error = e.getMessage();
			if (error.equals("AD_SUCC_FAILED")) {
				response = "AD_SUCC_FAILED";
			}
			log.log(Level.SEVERE, "preActionsOnADCreateUser", e);
		}
		log.exiting(getClass().getName(), "preActionsOnADCreateUser");
		return response;
	}

	public String postActionsOnADCreateUser(String processInstanceKey, String strUserkey, String itResourceName)
			throws NoSuchUserException, UserLookupException, SearchKeyNotUniqueException, AccessDeniedException {
		log.entering(getClass().getName(), "postActionsOnADCreateUser");
		log.fine("processInstanceKey =" + processInstanceKey + "strUserkey = " + strUserkey + "itResourceName = " + itResourceName);
		long processInstanceKeyL = Long.parseLong(processInstanceKey);
		tcFormInstanceOperationsIntf formInstanceOperationsIntf = null;
		formInstanceOperationsIntf = Platform.getService(tcFormInstanceOperationsIntf.class);
		HashMap<String, String> procFormHash = new HashMap<String, String>();
		String response = "FAILURE";
		String description = "";
		String company = "";
		String phyOfcName = "";
		String streeAddress = "";
		String title = "";
		String telephone = "";
		User user = OIMUtility.getUserProfile(strUserkey);
		log.fine("itResourceName1" + itResourceName);
		try {

			if (itResourceName.toLowerCase().contains("ad-res")) {
				description = populateDescription(strUserkey, user);
				company = populateCompany(strUserkey, user);
				phyOfcName = populatphyOfcName(strUserkey, user);
				streeAddress = populateStreetAddress(strUserkey, user);
				title = populateTitle(strUserkey, user);
				telephone = populateTelephone(strUserkey, user);
				procFormHash.put("UD_ADUSER_DESCRIPTION", description);
				procFormHash.put("UD_ADUSER_COMPANY", company);
				procFormHash.put("UD_ADUSER_OFFICE", phyOfcName);
				procFormHash.put("UD_ADUSER_STREET", streeAddress);
				procFormHash.put("UD_ADUSER_TELEPHONE", telephone);
				procFormHash.put("UD_ADUSER_TITLE", title);

			} else if (itResourceName.toLowerCase().contains("lbg")) {
				description = populateDescription(strUserkey, user);
				company = populateCompany(strUserkey, user);
				phyOfcName = populatphyOfcName(strUserkey, user);
				streeAddress = populateStreetAddress(strUserkey, user);
				title = populateTitle(strUserkey, user);
				telephone = populateTelephone(strUserkey, user);
				procFormHash.put("UD_ADLBG_DESCRIPTION", description);
				procFormHash.put("UD_ADLBG_COMPANY", company);
				procFormHash.put("UD_ADLBG_OFFICE", phyOfcName);
				procFormHash.put("UD_ADLBG_STREET", streeAddress);
				procFormHash.put("UD_ADLBG_TELEPHONE", telephone);
				procFormHash.put("UD_ADLBG_TITLE", title);

			} else if (itResourceName.toLowerCase().contains("succ")) {
				description = populateDescription(strUserkey, user);
				company = populateCompany(strUserkey, user);
				streeAddress = populateStreetAddress(strUserkey, user);
				title = populateTitle(strUserkey, user);
				procFormHash.put("UD_ADUSER_DESCRIPTION", description);
				procFormHash.put("UD_ADUSER_COMPANY", company);
				procFormHash.put("UD_ADUSER_STREET", streeAddress);
				procFormHash.put("UD_ADUSER_TITLE", title);
			}

			if (procFormHash != null && procFormHash.size() > 0)
				formInstanceOperationsIntf.setProcessFormData(processInstanceKeyL, procFormHash);
			log.exiting("ADProvisoning", "populateDistinguishedName");
			response = "SUCCESS";
		} catch (Exception e) {
			log.log(Level.SEVERE, "preActionsOnADCreateUser", e);

		}
		log.exiting(getClass().getName(), "postActionsOnADCreateUser");
		return response;
	}

	private String polpulateUserPrincipalNAme(long processInstanceKeyL, String userKey, User user)
			throws NoSuchUserException, UserLookupException, SearchKeyNotUniqueException, AccessDeniedException {
		log.entering(getClass().getName(), "polpulateUserPrincipalNAme");
		log.fine("processInstanceKey" + processInstanceKeyL + "userKey" + userKey);
		String userPrincipalNAme = (String) user.getAttribute(Constants.UserAttributes.RESupn);
		String actualUserPrincipal = "";
		log.fine("userPrincipalNAme" + userPrincipalNAme);
		String prefixArray[] = userPrincipalNAme.split("@");
		actualUserPrincipal = prefixArray[0] + "@succ.bnc.ca";
		log.fine("actualUserPrincipal" + actualUserPrincipal);
		log.exiting(getClass().getName(), "polpulateUserPrincipalNAme");
		return actualUserPrincipal;
	}

	private String populateBusinessCategory(long processInstanceKeyL, String userKey, User user)
			throws NoSuchUserException, UserLookupException, SearchKeyNotUniqueException, AccessDeniedException {
		log.entering(getClass().getName(), "populateBusinessCategory");
		String businessCategory = null;
		businessCategory = (String) user.getAttribute("PVPCode");
		if (isNullOrEmpty(businessCategory)) {
			businessCategory = "";
		}
		log.fine("businessCategory=" + businessCategory);
		log.exiting(getClass().getName(), "populateBusinessCategory");
		return businessCategory;
	}

	private String populateExensionAttr8(long procInstKey, String userKey, User user)
			throws NoSuchUserException, UserLookupException, SearchKeyNotUniqueException, AccessDeniedException {
		log.entering(getClass().getName(), "populateExensionAttr8");
		log.fine("procInstKey=" + procInstKey + ", userKey=" + userKey);
		String ext8 = (String) user.getAttribute(Constants.UserAttributes.ISMLogin);
		if (isNullOrEmpty(ext8)) {
			ext8 = "";
		}
		log.fine("ext8=" + ext8);
		log.exiting(getClass().getName(), "populateExensionAttr8");
		return ext8;
	}

	private String populateExensionAttr5(long procInstKey, String userKey, User user)
			throws NoSuchUserException, UserLookupException, SearchKeyNotUniqueException, AccessDeniedException {
		log.entering(getClass().getName(), "populateExensionAttr5");
		String ext5 = "";
		log.fine("procInstKey=" + procInstKey + ", userKey=" + userKey);
		String succursale = (String) user.getAttribute(Constants.UserAttributes.SUCCURSALE);
		if (!isNullOrEmpty(succursale) && succursale.contains("1")) {
			ext5 = "W8-SUCC";
		} else if (!isNullOrEmpty(succursale) && succursale.contains("0")) {
			ext5 = "W8";
		} else {
			ext5 = "";

		}
		log.fine("ext5=" + ext5);
		log.exiting(getClass().getName(), "populateExensionAttr5");
		return ext5;
	}

	private String populateExensionAttr9(long procInstKey, String userKeyL)
			throws NoSuchUserException, UserLookupException, SearchKeyNotUniqueException, AccessDeniedException {
		log.entering(getClass().getName(), "populateExensionAttr9");

		log.fine("procInstKey=" + procInstKey + ", userKey=" + userKeyL);
		log.exiting(getClass().getName(), "populateExensionAttr9");
		return "111001000000";
	}

	private String populateO(long procInstKey, String userKey, User user)
			throws NoSuchUserException, UserLookupException, SearchKeyNotUniqueException, AccessDeniedException {
		log.entering(getClass().getName(), "populateO");
		String o = "";
		log.fine("procInstKey=" + procInstKey + ", userKey=" + userKey);

		String worktansit = (String) user.getAttribute(Constants.UserAttributes.SUCCURSALE);
		if (!isNullOrEmpty(worktansit) && worktansit.contains("1")) {
			o = "SUCC";
		} else {
			o = "";
		}
		log.exiting(getClass().getName(), "populateO");
		return o;
	}

	private String populatesAMAccountName(long procInstKey, String userKey, User user)
			throws NoSuchUserException, UserLookupException, SearchKeyNotUniqueException, AccessDeniedException {
		log.entering(getClass().getName(), "populatesAMAccountName");
		log.fine("procInstKey=" + procInstKey + ", userKey=" + userKey);
		String sAMAccountName = (String) user.getAttribute(UserManagerConstants.AttributeName.EMPLOYEE_NUMBER.getId());
		if (isNullOrEmpty(sAMAccountName))
			sAMAccountName = "";
		log.exiting(getClass().getName(), "populatesAMAccountName");
		return sAMAccountName;
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
	private String populateDescription(String userKey, User user) throws NoSuchUserException, UserLookupException,
			SearchKeyNotUniqueException, AccessDeniedException, ParseException {
		log.entering(getClass().getName(), "populateDescription");

		String description = null;
		String prefixdesc = null;
		String date = null;
		String strStartDate = null;
		String strEndDate = null;
		// String strUserkey = String.valueOf(userKeyL);
		String cc = (String) user.getAttribute(Constants.UserAttributes.COUNTRY_CODE);
		String status = (String) user.getAttribute(UserManagerConstants.AttributeName.STATUS.getName());
		log.fine("status=" + status);
		log.fine("cc=" + cc);
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
		log.fine("description=" + description);
		log.exiting(getClass().getName(), "populateDescription");
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
	private String populateDistinguishedName(long procInstKey, String userKey, String itResourceName, User user)
			throws tcCryptoException, tcAPIException, tcUserAccountDisabledException,
			tcPasswordResetAttemptsExceededException, tcLoginAttemptsExceededException, tcUserAccountInvalidException,
			tcUserAlreadyLoggedInException, tcProcessNotFoundException, tcFormNotFoundException,
			tcNotAtomicProcessException, tcVersionNotFoundException, tcInvalidValueException,
			tcRequiredDataMissingException, tcColumnNotFoundException, NoSuchUserException, UserLookupException,
			SearchKeyNotUniqueException, AccessDeniedException {

		log.entering(getClass().getName(), "populateDistinguishedName");
		log.fine("ADProvisoning/populateDistinguishedName procInstKey=" + procInstKey + ", userKey=" + userKey
				+ "itResourceName " + itResourceName);

		String userLogin = null;
		String PVP = null;
		String distinguishedName = null;
		String iscontain = "false";
		userLogin = (String) user.getAttribute(UserManagerConstants.AttributeName.USER_LOGIN.getId());
		log.fine("userLogin" + userLogin);
		if (itResourceName.toLowerCase().contains("ad-res")) {
			PVP = (String) user.getAttribute("PVPCode");
			log.fine("PVP " + PVP);
			String PVP1 = lookupOperationsIntf.getDecodedValueForEncodedValue("Lookup.BNC.PVP", PVP);
			log.fine("PVP1 " + PVP1);
			if (isNullOrEmpty(PVP)) {
				distinguishedName = "CN=" + userLogin + ",ou=Users,ou=WKS-MIG,OU=W8Only,DC=res,DC=bngf,DC=local";
			} else {
				log.fine("PVP1 is not null: " + PVP);
				distinguishedName = "CN=" + userLogin + ",ou=Users,ou=" + "," + PVP1
						+ "OU=W8Only,DC=res,DC=bngf,DC=local";
			}
		} else if (itResourceName.toLowerCase().contains("succ")) {

			String workTransit = (String) user.getAttribute(Constants.UserAttributes.WORK_TRANSIT);
			String provinceCode = (String) user.getAttribute(Constants.UserAttributes.PROVINCE_CODE);
			log.fine("provinceCode: " + provinceCode + " workTransit: " + workTransit);

			String arrProvCode[] = { "QC", "NB", "ON", "SK", "BC", "MB", "NE", "AB", "PE" };

			for (int i = 0; i < arrProvCode.length; i++) {
				if (provinceCode.toLowerCase().contains(arrProvCode[i].toLowerCase())) {
					iscontain = "true";
				}
			}

			if (isNullOrEmpty(workTransit) || "false".equalsIgnoreCase(iscontain)) {

				String errorMessage = "AD_SUCC_FAILED";
				throw new RuntimeException(errorMessage);
			} else {
				// distinguishedName = "CN=" + userLogin +","+ ou_RES;
				distinguishedName = "CN=" + userLogin + ",OU=Utillisateurs,OU=" + provinceCode
						+ "DC=succ,DC=reseau,DC=bnc,DC=ca";
			}
		}
		log.fine("distinguishedName :" + distinguishedName);
		log.exiting(getClass().getName(), "populateDistinguishedName");
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
	private String populateEmployeeNumber(String userKey, User user)
			throws NoSuchUserException, UserLookupException, SearchKeyNotUniqueException, AccessDeniedException {
		log.entering(getClass().getName(), "populateEmployeeNumber");
		String accountID = null;
		accountID = (String) user.getAttribute(Constants.UserAttributes.AccountId);
		log.fine("accountID" + accountID);
		if (isNullOrEmpty(accountID)) {
			accountID = "";
		}
		log.fine("accountID=" + accountID);
		log.exiting(getClass().getName(), "populateEmployeeNumber");
		return accountID;
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
	private String populateCountry(String userKey, User user)
			throws NoSuchUserException, UserLookupException, SearchKeyNotUniqueException, AccessDeniedException {
		log.entering(getClass().getName(), "populateEmployeeNumber");
		String cc = "";
		cc = (String) user.getAttribute(Constants.UserAttributes.COUNTRY_CODE);
		log.fine("cc" + cc);
		if (isNullOrEmpty(cc)) {
			cc = "";
		}
		log.fine("cc=" + cc);
		log.exiting(getClass().getName(), "populateEmployeeNumber");
		return cc;
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
	public String propagateChangesFromUserProfileToAD(String processInstanceKey, String userKey, String adAttribute, String itResourceName)
			throws NoSuchUserException, UserLookupException, SearchKeyNotUniqueException, AccessDeniedException,
			tcAPIException, tcInvalidValueException, tcNotAtomicProcessException, tcFormNotFoundException,
			tcRequiredDataMissingException, tcProcessNotFoundException, ParseException {
		log.entering(getClass().getName(), "propagateChangesFromUserProfileToAD");
		log.fine("processInstanceKey= " + processInstanceKey + " userKey= " + userKey + " adAttribute= " + adAttribute + "itResourceName = " + itResourceName );

		String response = "FAILURE";
		tcFormInstanceOperationsIntf formInstanceOperationsIntf = null;
		long processInstanceKeyL = Long.parseLong(processInstanceKey);

		HashMap<String, String> procFormHash = new HashMap<String, String>();

		formInstanceOperationsIntf = Platform.getService(tcFormInstanceOperationsIntf.class);
		
		User user = OIMUtility.getUserProfile(userKey);
		if (adAttribute.equalsIgnoreCase("pvp")) {
			String businessCategory = populateBusinessCategory(processInstanceKeyL, userKey, user);
			procFormHash.put("UD_ADRES_BUSINESSCATEGORY", businessCategory);
		}

		if (adAttribute.equalsIgnoreCase("lastNameUsed") || adAttribute.equalsIgnoreCase("firstNameUsed")) {
			String displayName = populateDisplayName(userKey, user);
			procFormHash.put("UD_ADUSER_FULLNAME", displayName);
			if (adAttribute.equalsIgnoreCase("lastNameUsed")) {
				String lastName = populateLastName(userKey, user);
				procFormHash.put("UD_ADUSER_LNAME", lastName);
			}
			if (adAttribute.equalsIgnoreCase("firstNameUsed")) {
				String firstName = populateFirstName(userKey, user);
				procFormHash.put("UD_ADUSER_FNAME", firstName);
			}
		}

		if (adAttribute.equalsIgnoreCase("userPrincipalName")) {
			String userPrincipalName = "";
			if (itResourceName.toLowerCase().contains("ad-res")) {
				userPrincipalName = (String) user.getAttribute(Constants.UserAttributes.RESupn);
				procFormHash.put("UD_ADUSER_USERPRINCIPALNAME", userPrincipalName);
			}
		}

		if (adAttribute.equalsIgnoreCase("worktansit") || adAttribute.equalsIgnoreCase("transitDescription")) {
			log.fine("adAttribute= " + adAttribute);

			String company = populateCompany(userKey, user);
			String phyOfcName = populatphyOfcName(userKey, user);
			log.fine("company= " + company + " phyOfcName= " + phyOfcName);

			procFormHash.put("UD_ADUSER_COMPANY", company);
			procFormHash.put("UD_ADUSER_OFFICE", phyOfcName);
			if (adAttribute.equalsIgnoreCase("worktansit")) {
				String dept = populateDepartment(userKey, user);
				procFormHash.put("UD_ADUSER_DEPARTMENT", dept);
			}
		}

		if (adAttribute.equalsIgnoreCase("officeStreetNo") || adAttribute.equalsIgnoreCase("officeStreetName")
				|| adAttribute.equalsIgnoreCase("siteName")) {
			String streeAddress = populateStreetAddress(userKey, user);
			procFormHash.put("UD_ADUSER_STREET", streeAddress);
		}

		if (adAttribute.equalsIgnoreCase("officePhoneDirect")) {
			String telephone = populateTelephone(userKey, user);
			procFormHash.put("UD_ADUSER_TELEPHONE", telephone);
		}

		if (adAttribute.equalsIgnoreCase("prefLang") || adAttribute.equalsIgnoreCase("functionEN")
				|| adAttribute.equalsIgnoreCase("functionFR")) {
			String title = populateTitle(userKey, user);
			procFormHash.put("UD_ADUSER_TITLE", title);
		}

		if (adAttribute.equalsIgnoreCase("CountryCode") || adAttribute.equalsIgnoreCase("FirtsNameUsed")
				|| adAttribute.equalsIgnoreCase("LASTNameUsed") || adAttribute.equalsIgnoreCase("status")
				|| adAttribute.equalsIgnoreCase("StartDate") || adAttribute.equalsIgnoreCase("EndDate")) {

			String description = populateDescription(userKey, user);
			procFormHash.put("UD_ADUSER_DESCRIPTION", description);
			if (adAttribute.equalsIgnoreCase("CountryCode")) {
				String cc = populateCountry(userKey, user);
				procFormHash.put("UD_ADUSER_COUNTRY", cc);
			}
		}
		if (procFormHash != null && procFormHash.size() > 0) {
			formInstanceOperationsIntf.setProcessFormData(processInstanceKeyL, procFormHash);
			response = "SUCCESS";
		}
		log.exiting(getClass().getName(), "propagateChangesFromUserProfileToAD");
		return response;
	}

	private String populateDepartment(String userKey, User user)
			throws NoSuchUserException, UserLookupException, SearchKeyNotUniqueException, AccessDeniedException {
		log.entering(getClass().getName(), "populateDepartment");
		String workTransit = (String) user.getAttribute(Constants.UserAttributes.WORK_TRANSIT);
		if (isNullOrEmpty(workTransit)) {
			workTransit = "";
		}
		log.fine("workTransit=" + workTransit);
		log.exiting(getClass().getName(), "populateDepartment");
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
	private String populateTitle(String userKey, User user)
			throws NoSuchUserException, UserLookupException, SearchKeyNotUniqueException, AccessDeniedException {
		log.entering(getClass().getName(), "populateTitle");
		String title = null;
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
		log.fine("title=" + title);
		log.exiting(getClass().getName(), "populateTitle");
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
	private String populateTelephone(String userKey, User user)
			throws NoSuchUserException, UserLookupException, SearchKeyNotUniqueException, AccessDeniedException {
		log.entering(getClass().getName(), "populateTelephone");
		String telephone = null;
		String ofcPhoneDirect = (String) user.getAttribute(Constants.UserAttributes.Office_Phone_Direct);// ======
		if (!isNullOrEmpty(ofcPhoneDirect)) {
			telephone = "+1." + ofcPhoneDirect.substring(0, 3) + "." + ofcPhoneDirect.substring(3, 6) + "."
					+ ofcPhoneDirect.substring(6);
		}
		log.fine("telephone=" + telephone);
		log.exiting(getClass().getName(), "populateTelephone");
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
	private String populateStreetAddress(String userKey, User user)
			throws NoSuchUserException, UserLookupException, SearchKeyNotUniqueException, AccessDeniedException {
		log.entering(getClass().getName(), "populateStreetAddress");
		String streetAddress = null;
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
		log.fine("streetAddress=" + streetAddress);
		log.exiting(getClass().getName(), "populateStreetAddress");
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
	private String populatphyOfcName(String userKey, User user)
			throws NoSuchUserException, UserLookupException, SearchKeyNotUniqueException, AccessDeniedException {
		log.entering(getClass().getName(), "populatphyOfcName");
		String phyOfcName = null;
		String workTransit = (String) user.getAttribute(Constants.UserAttributes.WORK_TRANSIT);
		String transitDesc = (String) user.getAttribute(Constants.UserAttributes.TRANSIT_DESCRIPTION);
		if (isNullOrEmpty(workTransit)) {
			workTransit = "";
		}
		if (isNullOrEmpty(transitDesc)) {
			transitDesc = "";
		}
		phyOfcName = workTransit + "|" + transitDesc;
		log.fine("phyOfcName=" + phyOfcName);
		log.exiting(getClass().getName(), "populatphyOfcName");
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
	private String populateCompany(String userKey, User user)
			throws NoSuchUserException, UserLookupException, SearchKeyNotUniqueException, AccessDeniedException {
		log.entering(getClass().getName(), "populateCompany");
		String company = null;
		String workTransit = (String) user.getAttribute(Constants.UserAttributes.WORK_TRANSIT);
		String transitDesc = (String) user.getAttribute(Constants.UserAttributes.TRANSIT_DESCRIPTION);
		if (isNullOrEmpty(workTransit)) {
			workTransit = "";
		}
		if (isNullOrEmpty(transitDesc)) {
			transitDesc = "";
		}
		company = "BNC/" + workTransit + "-" + transitDesc;
		log.fine("company=" + company);
		log.exiting(getClass().getName(), "populateCompany");
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
	public String populateDisplayName(String userKey, User user)
			throws NoSuchUserException, UserLookupException, SearchKeyNotUniqueException, AccessDeniedException {
		log.entering(getClass().getName(), "populateDisplayName");
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
		log.exiting(getClass().getName(), "populateDisplayName");
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
	public String prepopulateDisplayName(String firstNameUsed, String lastNameUsed)
			throws NoSuchUserException, UserLookupException, SearchKeyNotUniqueException, AccessDeniedException {
		log.entering(getClass().getName(), "populateDisplayName");

		if (isNullOrEmpty(firstNameUsed)) {
			firstNameUsed = "";
		}
		if (isNullOrEmpty(lastNameUsed)) {
			lastNameUsed = "";
		}
		String displayName = lastNameUsed + "," + firstNameUsed;
		log.fine("displayName=" + displayName);
		log.exiting(getClass().getName(), "populateDisplayName");
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
	private String populateLastName(String userKey, User user)
			throws NoSuchUserException, UserLookupException, SearchKeyNotUniqueException, AccessDeniedException {
		log.entering(getClass().getName(), "populateLastName");
		String lnUsed = null;
		lnUsed = (String) user.getAttribute(Constants.UserAttributes.LastNameUsed);
		if (isNullOrEmpty(lnUsed)) {
			lnUsed = "";
		}
		log.exiting(getClass().getName(), "populateLastName");
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
	private String populateFirstName(String userKey, User user)
			throws NoSuchUserException, UserLookupException, SearchKeyNotUniqueException, AccessDeniedException {
		log.entering(getClass().getName(), "populateFirstName");
		String fnUsed = null;
		fnUsed = (String) user.getAttribute(Constants.UserAttributes.FirstNameUsed);
		if (isNullOrEmpty(fnUsed)) {
			fnUsed = "";
		}
		log.fine("fnUsed=" + fnUsed);
		log.exiting(getClass().getName(), "populateFirstName");
		return fnUsed;
	}

}
