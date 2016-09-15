package com.icsynergy.bnc;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.logging.Logger;

import com.thortech.xl.crypto.tcCryptoException;
import com.thortech.xl.crypto.tcCryptoUtil;
import com.thortech.xl.crypto.tcSignatureMessage;

import Thor.API.tcResultSet;
import Thor.API.tcUtilityFactory;
import Thor.API.Exceptions.tcAPIException;
import Thor.API.Exceptions.tcColumnNotFoundException;
import Thor.API.Exceptions.tcFormNotFoundException;
import Thor.API.Exceptions.tcInvalidLookupException;
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
import oracle.iam.identity.usermgmt.api.UserManager;
import oracle.iam.identity.usermgmt.vo.User;
import oracle.iam.platform.Platform;
import oracle.iam.platform.utils.ConfigurationClient;



public class OIMUtility {
	final private static Logger log = Logger.getLogger("com.icsynergy");
	private static Object syncObject = new Object();
	static final String LOOKUPCODE = "Lookup Definition.Lookup Code Information.Code Key";
	static final String LOOKUPDECODE = "Lookup Definition.Lookup Code Information.Decode";
	private HashMap<String, String> configHashMap = null;
	public static final String USER_KEY = "usr_key";
	
	private UserManager um = Platform.getService(UserManager.class);

	public static tcUtilityFactory getUtilityFactory() throws tcCryptoException, tcAPIException,
			tcUserAccountDisabledException, tcPasswordResetAttemptsExceededException, tcLoginAttemptsExceededException,
			tcUserAccountInvalidException, tcUserAlreadyLoggedInException {
		tcUtilityFactory moFactory = null;

		synchronized (syncObject) {
			ConfigurationClient.ComplexSetting config = ConfigurationClient
					.getComplexSettingByPath("Discovery.CoreServer");
			final Hashtable env = config.getAllSettings();
			tcSignatureMessage moSignature;
			moSignature = tcCryptoUtil.sign("xelsysadm", "PrivateKey");
			moFactory = new tcUtilityFactory(env, moSignature);
		}

		return moFactory;
	}

	public static tcResultSet getProcessFormData(long procInstKey) throws tcCryptoException, tcAPIException,
			tcUserAccountDisabledException, tcPasswordResetAttemptsExceededException, tcLoginAttemptsExceededException,
			tcUserAccountInvalidException, tcUserAlreadyLoggedInException, tcProcessNotFoundException,
			tcFormNotFoundException, tcNotAtomicProcessException, tcVersionNotFoundException, tcInvalidValueException,
			tcRequiredDataMissingException {
		log.entering("ADProvisoning", "getProcessFormData");
		log.fine("OIMUtil/getProcessFormData procInstKey=" + procInstKey);
		tcUtilityFactory apiFactory = getUtilityFactory();

		tcFormInstanceOperationsIntf formIntf = (tcFormInstanceOperationsIntf) apiFactory
				.getUtility("Thor.API.Operations.tcFormInstanceOperationsIntf");

		tcResultSet procData = formIntf.getProcessFormData(procInstKey);

		apiFactory.close();

		log.exiting("ADProvisoning", "getProcessFormData");
		return procData;
	}

	/**
	 * 
	 * This method takes the procInstKey,fieldName and fieldValue and populate
	 * it in process form
	 * 
	 * @param 1.
	 *            procInstKey : Process Instance key of a process form 2.
	 *            fieldName : Column name of a process form 3. fieldValue :
	 *            Field value which is going to be populate in process form
	 * 
	 * @return : SUCCESS
	 * 
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
	 * 
	 */

	public static String populateFieldInProcessForm(long procInstKey, String fieldName, String fieldValue)
			throws tcCryptoException, tcAPIException, tcUserAccountDisabledException,
			tcPasswordResetAttemptsExceededException, tcLoginAttemptsExceededException, tcUserAccountInvalidException,
			tcUserAlreadyLoggedInException, tcProcessNotFoundException, tcFormNotFoundException,
			tcNotAtomicProcessException, tcVersionNotFoundException, tcInvalidValueException,
			tcRequiredDataMissingException {
		log.entering("ADProvisoning", "populateFieldInProcessForm");
		log.fine("OIMUtil/populateFieldInProcessForm procInstKey=" + procInstKey + ", fieldName=" + fieldName
				+ ", fieldValue=" + fieldValue);
		tcUtilityFactory apiFactory = getUtilityFactory();

		tcFormInstanceOperationsIntf formIntf = (tcFormInstanceOperationsIntf) apiFactory
				.getUtility("Thor.API.Operations.tcFormInstanceOperationsIntf");

		HashMap<String, String> hash = new HashMap<String, String>();
		hash.put(fieldName, fieldValue);
		formIntf.setProcessFormData(procInstKey, hash);

		apiFactory.close();

		log.exiting("ADProvisoning", "populateFieldInProcessForm");
		return "SUCCESS";
	}

	public static HashMap<String, String> getHashMapFromLookup(String lookupName)
			throws tcAPIException, tcInvalidLookupException, tcColumnNotFoundException, tcCryptoException,
			tcUserAccountDisabledException, tcPasswordResetAttemptsExceededException, tcLoginAttemptsExceededException,
			tcUserAccountInvalidException, tcUserAlreadyLoggedInException {
		tcUtilityFactory apiFactory = getUtilityFactory();
		tcLookupOperationsIntf lookupIntf = (tcLookupOperationsIntf) apiFactory
				.getUtility("Thor.API.Operations.tcLookupOperationsIntf");
		HashMap<String, String> lookupMap = getHashMapFromLookup(lookupName, lookupIntf);

		apiFactory.close();

		return lookupMap;
	}

	/**
	 * 
	 * This method takes the Lookup name and Lookup operation interface instance
	 * parameters as argument and returns map that contain Lookup coded and
	 * decoded values as key and value of the map. Method parameters are:
	 * 
	 * 1. lookupName : Lookup name from which Hash map is created 2. lookupIntf
	 * : Lookup operation interface instance
	 * 
	 * @return : Hash Map that contains lookup coded value as key and decoded
	 *         value as value
	 * 
	 * @throws tcAPIException
	 * @throws tcInvalidLookupException
	 * @throws tcColumnNotFoundException
	 */
	public static HashMap<String, String> getHashMapFromLookup(String lookupName, tcLookupOperationsIntf lookupIntf)
			throws tcAPIException, tcInvalidLookupException, tcColumnNotFoundException {
		log.entering("ADProvisoning", "getHashMapFromLookup");
		log.fine("Lookup Name = " + lookupName);
		tcResultSet lookupResultSet = lookupIntf.getLookupValues(lookupName);
		HashMap<String, String> map = new HashMap<String, String>();
		int rowCount = lookupResultSet.getRowCount();
		for (int i = 0; i < rowCount; i++) {
			lookupResultSet.goToRow(i);
			map.put(lookupResultSet.getStringValue(LOOKUPCODE), lookupResultSet.getStringValue(LOOKUPDECODE));
		}
		log.exiting("ADProvisoning", "getHashMapFromLookup");
		return map;
	}
	
	public static User getUserProfile(String userKey)
			throws NoSuchUserException, UserLookupException,
			SearchKeyNotUniqueException, AccessDeniedException {
		log.entering("ADProvisoning", "getUserProfile");
		UserManager userService = Platform.getService(UserManager.class);
		User userDetail = userService.getDetails(USER_KEY, userKey, null);
		log.exiting("ADProvisoning", "getUserProfile");
		return userDetail;
	}
	/**
	 * This method fetches the value for ITResource being passed in the Process form for given procInstanceKey.
	 * @param formInstanceOperationsIntf
	 * @param processInstanceKey
	 * @param logger
	 * @return
	 */
		public static String getITResourceNameFromUserProcesssForm(tcFormInstanceOperationsIntf formInstanceOperationsIntf,
				tcITResourceInstanceOperationsIntf itResourceinstanceOperationsIntf,
				long processInstanceKey) {   
			
			long itResourceKey = 0L;
			int countResult = 0;
			HashMap<String, String> hashMap = null; 
			tcResultSet itResourceDefinitionData = null;
			String itResourceName = "";
			String strMethodName="/getITResourceNameFromUserProcesssForm";
			try {
				tcResultSet resultGetProcessFormData = 
					formInstanceOperationsIntf.getProcessFormData(processInstanceKey);
			
				//printTcResultSet(resultGetProcessFormData, "resultGetProcessFormData");
				if(!isResultSetNullOrEmpty(resultGetProcessFormData)) {
			 
				countResult = resultGetProcessFormData.getRowCount();
				log.fine("resultGetProcessFormData length . countResult =  " + countResult);
		 
				//Get the It resource Key from the process form data
       			resultGetProcessFormData.goToRow(0);
				itResourceKey = resultGetProcessFormData.getLongValue("UD_ADUSER_SERVER");
				log.fine("resultGetProcessFormData length . itResourceKey  =  " + itResourceKey);

				//Get the IT Resource Name from IT resource Key, based on the IT Resource definition
				hashMap = new HashMap<String, String>(); 
				hashMap.put("IT Resources.Key", String.valueOf(itResourceKey));
				log.fine("LFGUtil:getITResourceNameFromUserProcesssForm()/ itResourceKey =  " + itResourceKey);
				itResourceDefinitionData = itResourceinstanceOperationsIntf.findITResourceInstances(hashMap);
				//printTcResultSet(itResourceDefinitionData, "itResourceDefinitionData");
				itResourceDefinitionData.goToRow(0);
				itResourceName = itResourceDefinitionData.getStringValue("IT Resources.Name");

				log.fine("resultGetProcessFormData length . itResource  Name   =  " + itResourceName);
				}
				return itResourceName;
				
			} catch (Exception e) {
				
				log.fine(e.getMessage());
				
				return "Error";
			}    	

		}
		
	    /**
		 * This is a helper method to verify whether a tcResultSet is Null or Empty..
		 * @param s
		 * @return
		 */
		public static boolean isResultSetNullOrEmpty(tcResultSet resultSet) {
			boolean isResultSetNullOrEmpty = true;
			try {
				isResultSetNullOrEmpty = resultSet == null || resultSet.isEmpty() || resultSet.getRowCount() == 0;
			} catch (Throwable t) {
				log.fine(t.getMessage());
			} finally {			
			}
			return isResultSetNullOrEmpty;
		}   
		

}
