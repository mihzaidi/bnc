package com.icsynergy.bnc.adapters;

import java.util.logging.Logger;

import com.thortech.xl.crypto.tcCryptoException;

import Thor.API.tcResultSet;

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

import oracle.iam.identity.exception.AccessDeniedException;
import oracle.iam.identity.exception.NoSuchUserException;
import oracle.iam.identity.exception.SearchKeyNotUniqueException;
import oracle.iam.identity.exception.UserLookupException;

import oracle.iam.identity.usermgmt.vo.User;

import com.icsynergy.bnc.OIMUtility;

public class ADProvisioning {
	final private static Logger log = Logger.getLogger("com.icsynergy");

	// Lookup.BNC.AD.Config

	public static String getITresourceFromADForm(long procInstKey) throws tcCryptoException, tcAPIException,
			tcUserAccountDisabledException, tcPasswordResetAttemptsExceededException, tcLoginAttemptsExceededException,
			tcUserAccountInvalidException, tcUserAlreadyLoggedInException, tcProcessNotFoundException,
			tcFormNotFoundException, tcNotAtomicProcessException, tcVersionNotFoundException, tcInvalidValueException,
			tcRequiredDataMissingException, tcColumnNotFoundException {
		log.entering("ADProvisoning", "getITresourceFromADForm");
		tcResultSet processFormData = null;
		String itResourceName = "";
		processFormData = OIMUtility.getProcessFormData(procInstKey);
		itResourceName = processFormData.getStringValue("UD_ADUSER_SERVER");
		log.exiting("ADProvisoning", "getITresourceFromADForm");
		return itResourceName;
	}

	public static String populateDistinguishedName(long procInstKey, long userKey)
			throws tcCryptoException, tcAPIException, tcUserAccountDisabledException,
			tcPasswordResetAttemptsExceededException, tcLoginAttemptsExceededException, tcUserAccountInvalidException,
			tcUserAlreadyLoggedInException, tcProcessNotFoundException, tcFormNotFoundException,
			tcNotAtomicProcessException, tcVersionNotFoundException, tcInvalidValueException,
			tcRequiredDataMissingException, tcColumnNotFoundException, NoSuchUserException, UserLookupException,
			SearchKeyNotUniqueException, AccessDeniedException {
		log.entering("ADProvisoning", "populateDistinguishedName");
		log.fine("ADProvisoning/populateDistinguishedName procInstKey=" + procInstKey + ", userKey=" + userKey);
		String itResourceName = getITresourceFromADForm(procInstKey);
		String strUserkey = String.valueOf(userKey);
		String userLogin = null;
		String PVP = null;
		String distinguishedName = null;

		User user = OIMUtility.getUserProfile(strUserkey);
		userLogin = (String) user.getAttribute("USR_LOGIN");
		PVP = (String) user.getAttribute("USR_UDF_PVP");

		if (itResourceName.equalsIgnoreCase("AD-RES Main IT Resource")) {
			if (PVP.isEmpty()) {
				distinguishedName = "cn=" + userLogin + "ou=Users,ou= WKS-MIG,OU=W8Only,DC=res,DC=bngf,DC=local";

			} else {

			}

		} else if (itResourceName.equalsIgnoreCase("AD-SUCC Main IT Resource")) {

		} else if (itResourceName.equalsIgnoreCase("AD-LBG Main IT Resource")) {

		}

		log.exiting("ADProvisoning", "populateDistinguishedName");
		return "SUCCESS";
	}

}
