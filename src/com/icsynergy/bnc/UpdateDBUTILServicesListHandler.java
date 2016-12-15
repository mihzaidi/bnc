package com.icsynergy.bnc.handlers;

import Thor.API.Operations.tcLookupOperationsIntf;

import java.io.Serializable;

import java.util.ArrayList;
import java.util.HashMap;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import oracle.iam.identity.exception.UserMembershipException;
import oracle.iam.identity.rolemgmt.api.RoleManager;
import oracle.iam.identity.rolemgmt.vo.Role;
import oracle.iam.platform.Platform;
import oracle.iam.platform.authopss.exception.AccessDeniedException;
import oracle.iam.platform.entitymgr.vo.SearchCriteria;
import oracle.iam.platform.kernel.spi.PostProcessHandler;
import oracle.iam.platform.kernel.vo.AbstractGenericOrchestration;
import oracle.iam.platform.kernel.vo.BulkEventResult;
import oracle.iam.platform.kernel.vo.BulkOrchestration;
import oracle.iam.platform.kernel.vo.EventResult;
import oracle.iam.platform.kernel.vo.Orchestration;
import oracle.iam.provisioning.api.ProvisioningConstants;
import oracle.iam.provisioning.api.ProvisioningService;
import oracle.iam.provisioning.exception.AccountNotFoundException;
import oracle.iam.provisioning.exception.GenericProvisioningException;
import oracle.iam.provisioning.exception.UserNotFoundException;
import oracle.iam.provisioning.vo.Account;
import oracle.iam.provisioning.vo.AccountData;
import oracle.iam.provisioning.vo.ApplicationInstance;
import oracle.iam.provisioning.vo.FormField;


public class UpdateDBUTILServicesListHandler implements PostProcessHandler {

    private final static Logger logger = Logger.getLogger("com.icsynergy");
    private final static String tag = UpdateDBUTILServicesListHandler.class.getCanonicalName();


    public UpdateDBUTILServicesListHandler() {
        super();
    }
    // Single Event

    public EventResult execute(long processID, long eventID, Orchestration orchestration) {
        logger.entering(tag, "execte");
        // Get Single User Key
        Map<String, Serializable> map = orchestration.getParameters();
        logger.log(Level.SEVERE, "This is the map: " + map);
        List<String> lstUserKey = (List<String>)map.get("userKeys");
        // Invoke Provisioning Service
        ProvisioningService pS = Platform.getService(ProvisioningService.class);
        //Search Criteria for DB_UTIL Account
        SearchCriteria DB_UTIL =
            new SearchCriteria(ProvisioningConstants.AccountSearchAttribute.OBJ_NAME.getId(), "DB_UTIL User",
                               SearchCriteria.Operator.EQUAL);
        // Initialize List of Accounts to User
        List<Account> lstAccounts = new ArrayList<Account>();
        //Create Result Value that needs to be inserted into DB_UTIL Account
        String result = "";
        //Invoke Role Manager
        RoleManager rmgr = Platform.getService(RoleManager.class);
        // Initialize list of Roles retrieved by Role Manager
        List<Role> lstRoles = new ArrayList<Role>();
        // Initialize String List of Role Names
        List<String> lstRName = new ArrayList<String>();
        // Initialize List of Services provided through Role Name lookup
        List<String> lstServices = new ArrayList<String>();
        // LookupCode String value for BNC Services
        final String strLookUp = "Lookup.BNC.ServicesList";
        for (String Ukey : lstUserKey) {
            try {
                //Get User Roles
                lstRoles = rmgr.getUserMemberships(Ukey, false);
                for (Role r : lstRoles) {
                    try {
                        // Get Role Name and append to List Role Names
                        String strRName = r.getName();
                        lstRName.add(strRName);
                    } catch (Exception e) {
                        logger.log(Level.SEVERE,
                                   "Could not get Role Name: " + r.getName() + " Exception in: " + e.getMessage(), e);
                    }
                }
                for (String strRole : lstRName) {
                    try {
                        // Lookup Decoded Value and append to List Services
                        String strService =
                            Platform.getService(tcLookupOperationsIntf.class).getDecodedValueForEncodedValue(strLookUp,
                                                                                                             strRole);
                        if (strService != "") {
                            lstServices.add(strService);
                        }
                    } catch (Thor.API.Exceptions.tcAPIException e) {
                        logger.log(Level.SEVERE, "Role " + strRole + "not a coded value: " + e.getMessage(), e);

                    }
                }


            } catch (UserMembershipException e) {
                logger.log(Level.SEVERE, "Could not acquire roles for userkey " + Ukey + ": " + e.getMessage(), e);
            }
            Iterator<String> it = lstServices.iterator();
            if (it.hasNext()) {
                result = it.next();
            }
            while (it.hasNext()) {
                result += "," + it.next();
            }
            // Get all DB_UTIL accounts
            try {
                lstAccounts = pS.getAccountsProvisionedToUser(Ukey, DB_UTIL, null, true);
            } catch (UserNotFoundException | GenericProvisioningException e) {
                logger.log(Level.SEVERE, e.getMessage(), e);
            }
            // Parent Attribute needing to be Modified inside the resource account
            Map<String, Object> modAttrs = new HashMap<String, Object>();
            //If yes get all Role Memberships of User
            for (Account uAccount : lstAccounts) {
                if (uAccount != null) {
                    // Get Application Instance
                    ApplicationInstance AI = uAccount.getAppInstance();
                    // Get fields in Application Instance
                    FormField ServList = AI.getAccountForm().getFormField("UD_DB_UTILU_SERVICES_LIST");
                    modAttrs.put(ServList.getName(), result);
                    String accountID = uAccount.getAccountID();
                    String processFormInstanceKey = uAccount.getProcessInstanceKey();

                    Account modAccount = new Account(accountID, processFormInstanceKey, Ukey);
                    String formKey = uAccount.getAccountData().getFormKey();
                    String udTablePrimaryKey = uAccount.getAccountData().getUdTablePrimaryKey();
                    AccountData data = new AccountData(formKey, udTablePrimaryKey, modAttrs);
                    modAccount.setAccountData(data);
                    modAccount.setAppInstance(AI);
                    //Run Services List Update Account Task
                    try {
                        pS.modify(modAccount);
                    } catch (AccountNotFoundException | AccessDeniedException | GenericProvisioningException e) {
                        logger.log(Level.SEVERE, e.getMessage(), e);
                    }
                } else {
                    logger.log(Level.SEVERE, "Account is Empty: " + uAccount);
                }
            }
        }
        logger.exiting(tag, "Assigned BNC Services to DB_UTIL account for single user complete");
        return new EventResult();
    }
    // BulkEvent

    public BulkEventResult execute(long processID, long eventId, BulkOrchestration bulkOrchestration) {
        logger.entering(tag, "bulk execte");
        // Invoke Provisioning Service
        ProvisioningService pS = Platform.getService(ProvisioningService.class);
        //Search Criteria for DB_UTIL Account
        SearchCriteria DB_UTIL =
            new SearchCriteria(ProvisioningConstants.AccountSearchAttribute.OBJ_NAME.getId(), "DB_UTIL User",
                               SearchCriteria.Operator.EQUAL);
        // Initialize List of Accounts to User
        List<Account> lstAccounts = new ArrayList<Account>();
        //Create Result Value that needs to be inserted into DB_UTIL Account
        String result = "";
        //Invoke Role Manager
        RoleManager rmgr = Platform.getService(RoleManager.class);
        // Initialize list of Roles retrieved by Role Manager
        List<Role> lstRoles = new ArrayList<Role>();
        // Initialize String List of Role Names
        List<String> lstRName = new ArrayList<String>();
        // Initialize List of Services provided through Role Name lookup
        List<String> lstServices = new ArrayList<String>();
        // LookupCode String value for BNC Services
        final String strLookUp = "Lookup.BNC.ServicesList";
        // Get User Keys in Bulk Orchestration
        //String[] arrUserkey = bulkOrchestration.getTarget().getAllEntityId();
        Map<String, Serializable> map = bulkOrchestration.getParameters();
        List<String> lstUsrKeys = (List<String>)map.get("userKeys");
        // For each userkey
        for (String UKey : lstUsrKeys) {
            result = "";
            logger.log(Level.SEVERE, "Current result is: " + result);
            logger.log(Level.SEVERE, "Get roles for user: " + UKey);
            try {
                //Get User Roles
                lstRoles = rmgr.getUserMemberships(UKey, false);
                logger.log(Level.SEVERE, UKey + " has roles: " + lstRoles);
                lstRName = new ArrayList<String>();
                for (Role r : lstRoles) {
                    try {
                        // Get Role Name and append to List Role Names
                        String strRName = r.getName();
                        lstRName.add(strRName);
                    } catch (Exception e) {
                        logger.log(Level.SEVERE,
                                   "Could not get Role Name: " + r.getName() + " Exception in: " + e.getMessage(), e);
                    }
                }
                logger.log(Level.SEVERE, UKey + "has roles with names: " + lstRName);
                lstServices = new ArrayList<String>();
                for (String strRole : lstRName) {
                    try {
                        // Lookup Decoded Value and append to List Services
                        String strService =
                            Platform.getService(tcLookupOperationsIntf.class).getDecodedValueForEncodedValue(strLookUp,
                                                                                                             strRole);

                        if (strService != "") {
                            lstServices.add(strService);
                        }
                    } catch (Thor.API.Exceptions.tcAPIException e) {
                        logger.log(Level.SEVERE, "Role " + strRole + "not a coded value: " + e.getMessage(), e);

                    }
                    logger.log(Level.SEVERE, "Services for " + UKey + " are " + lstServices);
                }


            } catch (UserMembershipException e) {
                logger.log(Level.SEVERE, "Could not acquire roles for userkey " + UKey + ": " + e.getMessage(), e);
            }
            Iterator<String> it = lstServices.iterator();
            if (it.hasNext()) {
                result = it.next();
            }
            while (it.hasNext()) {
                result += "," + it.next();
            }
            logger.log(Level.SEVERE, "result is" + result);
            // Get all DB_UTIL accounts
            try {
                lstAccounts = pS.getAccountsProvisionedToUser(UKey, DB_UTIL, null, true);
            } catch (UserNotFoundException | GenericProvisioningException e) {
                logger.log(Level.SEVERE, "Search with Object Name DB_UTIL User failed :" + e.getMessage(), e);
            }
            // Parent Attribute needing to be Modified inside the resource account
            Map<String, Object> modAttrs = new HashMap<String, Object>();
            //If yes get all Role Memberships of User
            for (Account uAccount : lstAccounts) {
                if (uAccount != null) {
                    // Get Application Instance
                    ApplicationInstance AI = uAccount.getAppInstance();
                    // Get fields in Application Instance
                    FormField ServList = AI.getAccountForm().getFormField("UD_DB_UTILU_SERVICES_LIST");
                    modAttrs.put(ServList.getName(), result);
                    String accountID = uAccount.getAccountID();
                    String processFormInstanceKey = uAccount.getProcessInstanceKey();

                    Account modAccount = new Account(accountID, processFormInstanceKey, UKey);
                    String formKey = uAccount.getAccountData().getFormKey();
                    String udTablePrimaryKey = uAccount.getAccountData().getUdTablePrimaryKey();
                    AccountData data = new AccountData(formKey, udTablePrimaryKey, modAttrs);
                    modAccount.setAccountData(data);
                    modAccount.setAppInstance(AI);
                    //Run Services List Update Account Task
                    try {
                        pS.modify(modAccount);
                    } catch (AccountNotFoundException | AccessDeniedException | GenericProvisioningException e) {
                        logger.log(Level.SEVERE, e.getMessage(), e);
                    }
                } else {
                    logger.log(Level.SEVERE, "lstAccounts is Empty: " + lstAccounts);
                }
            }
        }
        logger.exiting(tag, "Assigned BNC Services to DB_UTIL account for Bulk User event complete");
        return new BulkEventResult();
    }

    public void compensate(long processID, long eventId, AbstractGenericOrchestration AGO) {

    }

    public boolean cancel(long processID, long eventId, AbstractGenericOrchestration AGO) {
        return false;
    }

    public void initialize(HashMap<String, String> hashMap) {
    }

}
