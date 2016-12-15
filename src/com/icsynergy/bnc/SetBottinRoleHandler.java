package com.icsynergy.bnc.handlers;

import Thor.API.Operations.tcLookupOperationsIntf;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import oracle.iam.identity.exception.NoSuchRoleException;
import oracle.iam.identity.exception.NoSuchUserException;
import oracle.iam.identity.exception.RoleGrantException;
import oracle.iam.identity.exception.RoleGrantRevokeException;
import oracle.iam.identity.exception.RoleLookupException;
import oracle.iam.identity.exception.SearchKeyNotUniqueException;
import oracle.iam.identity.exception.UserLookupException;
import oracle.iam.identity.exception.UserMembershipException;
import oracle.iam.identity.exception.ValidationFailedException;
import oracle.iam.identity.rolemgmt.api.RoleManager;
import oracle.iam.identity.rolemgmt.api.RoleManagerConstants;


import oracle.iam.identity.rolemgmt.vo.Role;
import oracle.iam.identity.usermgmt.api.UserManager;
import oracle.iam.identity.usermgmt.api.UserManagerConstants;
import oracle.iam.identity.usermgmt.vo.User;
import oracle.iam.platform.Platform;
import oracle.iam.platform.kernel.spi.PostProcessHandler;

import oracle.iam.platform.kernel.vo.AbstractGenericOrchestration;
import oracle.iam.platform.kernel.vo.BulkEventResult;
import oracle.iam.platform.kernel.vo.BulkOrchestration;
import oracle.iam.platform.kernel.vo.EventResult;

import oracle.iam.platform.kernel.vo.Orchestration;

public class SetBottinRoleHandler implements PostProcessHandler {

    private Logger logger = Logger.getLogger("com.icsynergy");

    public SetBottinRoleHandler() {
        super();
    }
    //Single Target Bottin Rule Assign

    public EventResult execute(long processID, long eventID, Orchestration orchestration) {

        logger.fine("***Inside of Bottin Handler***");
        String strUserkey = orchestration.getTarget().getEntityId();

        //Invoke Role Manager
        RoleManager rmgr = Platform.getService(RoleManager.class);

        //Invoke User Manager
        UserManager umgr = Platform.getService(UserManager.class);

        //Set Working Transit, UserType, and Account End date as returned details for user
        Set<String> setUattr = new HashSet<String>();
        setUattr.add("WorkingTransit");
        setUattr.add(UserManagerConstants.AttributeName.EMPTYPE.getId().toString());
        setUattr.add(UserManagerConstants.AttributeName.ACCOUNT_END_DATE.getId().toString());

        //Set UserKey in Set for grantRole
        Set<String> setUkey = new HashSet<String>();
        setUkey.add(strUserkey);

        // return Role Attributes
        Set<String> setRetAttrs = new HashSet<String>();

        //Setting Date Value to yesterday from Current Day
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -1);
        Date yd = cal.getTime();

        // Lookup Attributes
        final String strLookUp = "Lookup.BNC.Bottin";
        final String strCodeKey = "Role Name";

        logger.fine("***Setting User Attributes for Criteria***");
        try {


            User u = umgr.getDetails(strUserkey, setUattr, false);

            // Get Assigned Bottin Role Name
            final String BottinRole =
                Platform.getService(tcLookupOperationsIntf.class).getDecodedValueForEncodedValue(strLookUp,
                                                                                                 strCodeKey);

            //Get Role name then Role Key
            Role r =
                rmgr.getDetails(RoleManagerConstants.RoleAttributeName.NAME.getId().toString(), BottinRole, setRetAttrs);
            String Rkey = r.getEntityId();

            logger.fine("Role Key= " + Rkey);

            // Set Working Transit
            logger.fine("***Define Values for Comparison***");
            String WT = String.valueOf(u.getAttribute("WorkingTransit"));
            logger.fine("WorkingTransit= " + WT);
            // Set UserType
            String UT = String.valueOf(u.getAttribute(UserManagerConstants.AttributeName.EMPTYPE.getId()));
            logger.fine("UserType= " + UT);
            // Set Date
            Date ET = new Date();
            if (u.getAttribute(UserManagerConstants.AttributeName.ACCOUNT_END_DATE.getId()) == null) {
                ET = null;
            } else {
                String TET =
                    String.valueOf(u.getAttribute(UserManagerConstants.AttributeName.ACCOUNT_END_DATE.getId()));
                SimpleDateFormat dt = new SimpleDateFormat("EEE MMM dd HH:mm:ss Z yyyy");
                ET = dt.parse(TET);

            }
            logger.fine("End Date= " + ET);
            logger.fine("***Check User Criteria to Grant or Revoke Role***");

            /*
            //Checking for Requirements test for debug use
            if(WT.matches("^[0-9]+$") || WT.matches("^[fF]\\S*$")) {
                logger.fine("I passed the Transit Test");
            }
            else {logger.fine("I failed the Transit Test");}
            if (UT.equals("1") || UT.equals("2")) {
                logger.fine("I passed the UserType Test");
            }
            else {logger.fine("I failed the UserType Test");}
            if (ET==null || ET.after(yd)) {
                logger.fine("I passed the End-Date Test");
            }
            else {logger.fine("I failed the End-Date Test");}
            */

            //Grant Role to role key returned by search criteria to UserKey set
            if ((WT.matches("^[0-9]+$") || WT.matches("^[fF]\\S*$")) && (UT.equals("1") || UT.equals("2")) &&
                (ET == null || ET.after(yd))) {

                logger.fine("***Role Requirements Met***");

                if (rmgr.isRoleGranted(Rkey, strUserkey, true)) {

                    logger.fine("***Role Already Granted***");

                }

                else {
                    logger.fine("***Grant Bottin Role***");
                    rmgr.grantRole(Rkey, setUkey);

                }
            }

            else {

                logger.fine("***Role Requirements Not Met***");
                if (rmgr.isRoleGranted(Rkey, strUserkey, true)) {

                    logger.fine("***Revoke Bottin Role***");
                    rmgr.revokeRoleGrant(Rkey, setUkey);

                }

            }

        }

        catch (RoleGrantRevokeException | UserMembershipException | ParseException | NoSuchUserException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        }

        catch (UserLookupException | NoSuchRoleException | SearchKeyNotUniqueException | RoleLookupException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        } catch (ValidationFailedException | RoleGrantException | Thor.API.Exceptions.tcAPIException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        }

        return new EventResult();

    }

    public BulkEventResult execute(long processID, long eventId, BulkOrchestration bulkOrchestration) {
        //List of User Keys

        logger.fine("***Inside Bulk Bottin Handler***");
        String[] arrUserkey = bulkOrchestration.getTarget().getAllEntityId();

        //Invoke Role Manager

        RoleManager rmgr = Platform.getService(RoleManager.class);

        //Set Role Return Attribute

        Set<String> setRetAttrs = new HashSet<String>();


        //Invoke User Manager
        UserManager umgr = Platform.getService(UserManager.class);

        //Set Defined fields as returned details for user

        Set<String> setUattr = new HashSet<String>();
        setUattr.add("WorkingTransit");
        setUattr.add(UserManagerConstants.AttributeName.EMPTYPE.getId().toString());
        setUattr.add(UserManagerConstants.AttributeName.ACCOUNT_END_DATE.getId().toString());

        //Create Set of UserKeys for Grant Role

        Set<String> setUkey = new HashSet<String>();

        //Create Set of UserKeys for Revoke Role

        Set<String> setRUkey = new HashSet<String>();

        //Set Yesterday's Date
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -1);
        Date yd = cal.getTime();

        // Lookup Attributes
        final String strLookUp = "Lookup.BNC.Bottin";
        final String strCodeKey = "Role Name";


        try {

            // Get Assigned Bottin Role Name
            final String BottinRole =
                Platform.getService(tcLookupOperationsIntf.class).getDecodedValueForEncodedValue(strLookUp,
                                                                                                 strCodeKey);

            // Get Role Key
            Role r =
                rmgr.getDetails(RoleManagerConstants.RoleAttributeName.NAME.getId().toString(), BottinRole, setRetAttrs);
            String Rkey = r.getEntityId();
            logger.fine("Role Key= " + Rkey);

            // Place only matching criteria defined fields userkeys into Ukey Set
            for (int i = 0; i < arrUserkey.length; i++) {

                User u = umgr.getDetails(arrUserkey[i], setUattr, false);
                // Set Working Transit
                logger.fine("***Define Values for Comparison***");
                String WT = String.valueOf(u.getAttribute("WorkingTransit"));
                logger.fine("Working Transit= " + WT);
                // Set UserType
                String UT = String.valueOf(u.getAttribute(UserManagerConstants.AttributeName.EMPTYPE.getId()));
                logger.fine("UserType= " + UT);
                // Set Date
                Date ET = new Date();
                if (u.getAttribute(UserManagerConstants.AttributeName.ACCOUNT_END_DATE.getId()) == null) {
                    ET = null;
                } else {
                    String TET =
                        String.valueOf(u.getAttribute(UserManagerConstants.AttributeName.ACCOUNT_END_DATE.getId()));
                    SimpleDateFormat dt = new SimpleDateFormat("EEE MMM dd HH:mm:ss Z yyyy");
                    ET = dt.parse(TET);
                }
                logger.fine("End Date= " + ET);
                //Add Userkeys to be granted Role into Ukey Set
                if ((WT.matches("^[0-9]+$") || WT.matches("^[fF]\\S*$")) && (UT == "1" || UT == "2") &&
                    (ET == null || ET.after(yd))) {

                    logger.fine("***Add Each User Key if Role Already not Granted***");
                    if (rmgr.isRoleGranted(Rkey, arrUserkey[i], true)) {

                        logger.fine("***Role Already Granted***");

                    }

                    else {
                        setUkey.add(arrUserkey[i]);
                    }

                }

                //Add Userkeys to be revoked Role into rUkey Set
                else {

                    logger.fine("***Adding User Keys to be Revoked***");
                    if (rmgr.isRoleGranted(Rkey, arrUserkey[i], true)) {

                        setRUkey.add(arrUserkey[i]);

                    }

                }

            }

            //Grant Roles to Users if need Granted and Revoke from Users that need Revoked

            logger.fine("***Grant Role Bulk Users***");
            rmgr.grantRole(Rkey, setUkey);

            logger.fine("***Revoke Role Bulk Users***");
            rmgr.revokeRoleGrant(Rkey, setRUkey);

        }

        catch (RoleGrantRevokeException | UserMembershipException | ParseException | NoSuchUserException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        }

        catch (UserLookupException | NoSuchRoleException | SearchKeyNotUniqueException | RoleLookupException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        } catch (ValidationFailedException | RoleGrantException | Thor.API.Exceptions.tcAPIException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        }
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
