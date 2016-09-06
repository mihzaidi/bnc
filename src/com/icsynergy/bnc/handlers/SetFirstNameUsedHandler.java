package com.icsynergy.bnc.handlers;

import com.icsynergy.bnc.Constants;
import com.icsynergy.bnc.adapters.NameTrimmer;
import oracle.iam.identity.exception.NoSuchUserException;
import oracle.iam.identity.exception.UserModifyException;
import oracle.iam.identity.exception.ValidationFailedException;
import oracle.iam.identity.usermgmt.api.UserManager;
import oracle.iam.identity.usermgmt.api.UserManagerConstants;
import oracle.iam.identity.usermgmt.vo.User;
import oracle.iam.platform.Platform;
import oracle.iam.platform.kernel.EventFailedException;
import oracle.iam.platform.kernel.spi.PostProcessHandler;
import oracle.iam.platform.kernel.vo.*;

import java.io.Serializable;
import java.util.HashMap;
import java.util.logging.Logger;

public class SetFirstNameUsedHandler implements PostProcessHandler {
    private final Logger log = Logger.getLogger("com.icsynergy");
    private final UserManager um =
            Platform.getServiceForEventHandlers(
                    UserManager.class, "SetFirstNameUsedHandler", "RECON", "SetFirstNameUsedHandler", null);

    @Override
    public EventResult execute(long l, long l1, Orchestration orchestration) {
        log.entering(getClass().getName(), "execute");

        if (orchestration.getParameters().containsKey(UserManagerConstants.AttributeName.FIRSTNAME.getId())) {
            processUser(
                    orchestration.getTarget().getEntityId(),
                    (String) orchestration.getParameters().get(UserManagerConstants.AttributeName.FIRSTNAME.getId()));
        }

        log.exiting(getClass().getName(), "execute");
        return new EventResult();
    }

    private void processUser(final String strUserKey, final String strFName) {
        log.entering(getClass().getName(), "processUser");

        String strFNameUsed = String.valueOf(
                new NameTrimmer()
                        .transform(
                                new HashMap<String, String>() {{
                                    put("First Name", strFName);
                                }},
                                null, Constants.CoucheAttributes.FirstNameUsed)
        );
        log.finer("strFNameUsed = " + strFNameUsed);

        log.finest("preparing user modification...");
        User u = new User(strUserKey);
        u.setAttribute(Constants.UserAttributes.FirstNameUsed, strFNameUsed);

        try {
            um.modify(u);
            log.fine("user usr_key=" + strUserKey + " has been modified");
        } catch (ValidationFailedException | UserModifyException | NoSuchUserException e) {
            throw new EventFailedException("Error modifying the user", null, e);
        }

        log.exiting(getClass().getName(), "processUser");
    }

    @Override
    public BulkEventResult execute(long l, long l1, BulkOrchestration bulkOrchestration) {
        log.entering(getClass().getName(), "bulk execute");

        final String[] arIds = bulkOrchestration.getTarget().getAllEntityId();
        final HashMap<String, Serializable>[] bulkParams = bulkOrchestration.getBulkParameters();

        log.finest("processing all users...");
        for (int i = 0; i < arIds.length; i++) {
            if (bulkParams[i].containsKey(UserManagerConstants.AttributeName.FIRSTNAME.getId()))
                processUser(arIds[i], (String) bulkParams[i].get(UserManagerConstants.AttributeName.FIRSTNAME.getId()));
        }

        log.exiting(getClass().getName(), "bulk execute");
        return new BulkEventResult();
    }

    @Override
    public void compensate(long l, long l1, AbstractGenericOrchestration abstractGenericOrchestration) {

    }

    @Override
    public boolean cancel(long l, long l1, AbstractGenericOrchestration abstractGenericOrchestration) {
        return false;
    }

    @Override
    public void initialize(HashMap<String, String> hashMap) {

    }
}
