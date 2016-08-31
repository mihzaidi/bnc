package com.icsynergy.bnc.handlers;

import Thor.API.Exceptions.tcAPIException;
import Thor.API.Exceptions.tcInvalidAttributeException;
import oracle.iam.identity.usermgmt.api.UserManagerConstants;
import oracle.iam.identity.vo.Identity;
import oracle.iam.platform.Platform;
import oracle.iam.platform.kernel.EventFailedException;
import oracle.iam.platform.kernel.spi.PostProcessHandler;
import oracle.iam.platform.kernel.vo.*;

import javax.naming.*;
import javax.sql.DataSource;

import Thor.API.Operations.tcLookupOperationsIntf;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.PreparedStatement;

import java.util.HashMap;
import java.util.logging.Logger;


public class SetUMSUserReconciledHandler implements PostProcessHandler {
    private final Logger log = Logger.getLogger("com.icsynergy");
    private final String CONFIGLOOKUP = "Lookup.BNC.UMS";

    private final String USER_TYPE_HR_PENDING = "0";
    private final String USER_TYPE_EMP = "1";

    private tcLookupOperationsIntf lookupOperationsIntf = Platform.getService(tcLookupOperationsIntf.class);

    @Override
    public EventResult execute(long arg0, long arg1, Orchestration orchestration) {
        log.entering(getClass().getName(), "execute");

        try (Connection oimConnection = getDatabaseConnection()) {
            Identity newUserState = (Identity) getNewUserStates(orchestration);
            Identity oldUserState = (Identity) getOldUserStates(orchestration);

            processUser(oimConnection, newUserState, oldUserState);
        } catch (Exception e) {
            throw new EventFailedException("Exception in processing", null, e);
        }

        log.exiting(getClass().getName(), "execute");
        return new EventResult();
    }

    @Override
    public BulkEventResult execute(long arg0, long arg1, BulkOrchestration bulkOrchestration) {
        log.entering(getClass().getName(), "bulk execute");

        try (Connection oimConnection = getDatabaseConnection()){
            Identity[] oldUserStatesIdntArr = (Identity[]) getOldUserStates(bulkOrchestration);
            Identity[] newUserStatesIdntArr = (Identity[]) getNewUserStates(bulkOrchestration);

            for (int i = 0; i < bulkOrchestration.getBulkParameters().length; i++) {
                Identity newUserState = newUserStatesIdntArr != null ? newUserStatesIdntArr[i] : null;
                Identity oldUserState = oldUserStatesIdntArr != null ? oldUserStatesIdntArr[i] : null;

                processUser(oimConnection, newUserState, oldUserState);
            }
        } catch (Exception e){
            throw new EventFailedException("Exception in bulk processing", null, e);
        }

        log.exiting(getClass().getName(), "bulk execute");
        return new BulkEventResult();
    }

    private void processUser(Connection oimConnection, Identity newUserState, Identity oldUserState)
            throws tcAPIException, SQLException {
        log.entering(getClass().getName(), "processUser");

        String olduserType = null, userType = null, empNo = null;

        if (oldUserState != null) {
            olduserType = (String) oldUserState.getAttribute(UserManagerConstants.AttributeName.EMPTYPE.getId());
            log.fine("Old user Type: " + olduserType);
        }

        if (newUserState != null) {
            userType = (String) newUserState.getAttribute(UserManagerConstants.AttributeName.EMPTYPE.getId());
            log.fine("Current user Type: " + userType);

            empNo = (String) newUserState.getAttribute(UserManagerConstants.AttributeName.EMPLOYEE_NUMBER.getId());
            log.fine("Current empNo: " + empNo);
        }

        if (!isNullOrEmpty(userType) && !isNullOrEmpty(olduserType)) {
            if (olduserType.equals(USER_TYPE_HR_PENDING) && userType.equals(USER_TYPE_EMP)) {
                log.fine("user Type changed from " + olduserType + " to " + userType +
                        ", setting User status in IAMUMS");
                setUserTypeInUMS(empNo, oimConnection);
            } else {
                log.fine("user Type changed from " + olduserType + " to " + userType +
                        ", no need to set User type in IAMUMS");
            }
        }

        log.exiting(getClass().getName(), "processUser");
    }

    private Connection getDatabaseConnection() throws SQLException, NamingException, tcAPIException {
        log.entering(getClass().getName(), "getDatabaseConnection()");

        log.finest("getting jdbc datasource JNDI name from a lookup");
        String UMSJNDINAME = "jdbc jndi name";
        String umsdbJNDIName = lookupOperationsIntf.getDecodedValueForEncodedValue(CONFIGLOOKUP, UMSJNDINAME);

        if (isNullOrEmpty(umsdbJNDIName))
            throw new RuntimeException("Can't get jdbc datasource name from the lookup");

        Connection con;
        Context initialContext = new InitialContext();

        try {
            DataSource datasource = (DataSource) initialContext.lookup(umsdbJNDIName.trim());

            if (datasource != null) {
                con = datasource.getConnection();
                log.fine("Successfully got a database connection to Server");
            } else {
                log.fine("Failed to lookup datasource.");
                throw new RuntimeException("Can't get database connection");
            }
        } finally {
            initialContext.close();
        }

        log.exiting(getClass().getName(), "getDatabaseConnection");
        return con;
    }

    private void setUserTypeInUMS(String empNo, Connection oimConnection) throws tcAPIException, SQLException {
        log.entering(getClass().getName(), "setUserTypeInUMS()");

        String UPDATE_QUERY = "updateQuery";
        String updateUMSQuery = lookupOperationsIntf.getDecodedValueForEncodedValue(CONFIGLOOKUP, UPDATE_QUERY);
        log.fine("updateUMSQuery -" + updateUMSQuery);

        if (isNullOrEmpty(updateUMSQuery))
            throw new RuntimeException("Can't get update query from the lookup");

        try (PreparedStatement ps = oimConnection.prepareStatement(updateUMSQuery)){
            ps.setString(1, empNo);

            ps.executeUpdate();
            log.fine("Employee with number " + empNo +
                    " successfully updated in UMS database as reconciled");
        }

        log.exiting(getClass().getName(), "getNewUserStates");
    }


    /**
     * Gets new user state
     *
     * @param orchestration
     * @return
     */
    private Object getNewUserStates(AbstractGenericOrchestration orchestration) {
        log.entering(getClass().getName(), "getNewUserStates");

        Object newUserStates = null;
        HashMap interEventData = orchestration.getInterEventData();

        if (interEventData != null)
            newUserStates =
                    interEventData.get("NEW_USER_STATE");

        log.exiting(getClass().getName(), "getNewUserStates");
        return newUserStates;
    }

    /**
     * Gets Old User state
     *
     * @param orchestration
     * @return
     */
    private Object getOldUserStates(AbstractGenericOrchestration orchestration) {
        log.entering(getClass().getName(), "getOldUserStates");
        Object oldUserStates = null;

        HashMap interEventData = orchestration.getInterEventData();
        if (interEventData != null)
            oldUserStates =
                    interEventData.get("CURRENT_USER");

        log.exiting(getClass().getName(), "getOldUserStates");
        return oldUserStates;
    }

    private boolean isNullOrEmpty(String strCheck)
    {
        return (strCheck == null) || strCheck.equals("null") || strCheck.trim().length() == 0;
    }


    @Override
    public void initialize(HashMap<String, String> arg0) {
    }

    @Override
    public void compensate(long arg0, long arg1, AbstractGenericOrchestration arg2) {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean cancel(long arg0, long arg1, AbstractGenericOrchestration arg2) {
        // TODO Auto-generated method stub
        return false;
    }

}
