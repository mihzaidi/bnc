package com.icsynergy.bnc.tasks;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import oracle.iam.identity.usermgmt.api.UserManager;
import oracle.iam.identity.usermgmt.api.UserManagerConstants;
import oracle.iam.identity.usermgmt.vo.User;
import oracle.iam.identity.usermgmt.vo.UserManagerResult;
import oracle.iam.platform.Platform;
import oracle.iam.platform.entitymgr.vo.SearchCriteria;
import oracle.iam.scheduler.vo.TaskSupport;

public class EnableUserOnStartDate extends TaskSupport {
	final private Logger log = Logger.getLogger("com.icsynergy");

	@Override
	public void execute(HashMap arg0) throws Exception {
		log.entering(getClass().getName(), "execute");

        // searching for users with given organization    
        SearchCriteria currentDtCriteria = new SearchCriteria(UserManagerConstants.AttributeName.ACCOUNT_START_DATE.getId(), new Date(), SearchCriteria.Operator.EQUAL);
        
        // searching for active users
        SearchCriteria critDisabled = new SearchCriteria(UserManagerConstants.AttributeName.STATUS.getName(), 
                                    UserManagerConstants.AttributeValues.USER_STATUS_DISABLED.getId(), SearchCriteria.Operator.EQUAL);
        
        // final criteria criteria
        SearchCriteria criteria = new SearchCriteria( currentDtCriteria, critDisabled, SearchCriteria.Operator.AND);
        
        // Hashset for holding ID
        Set<String> retAttr = new HashSet<String>();
        retAttr.add( UserManagerConstants.AttributeName.USER_LOGIN.getName() );
        
        // get interface
        UserManager usrmgr = Platform.getService( UserManager.class );
        
        // get user list who meet criteria
        List<User> list = usrmgr.search(criteria, retAttr, null);
        
        // If no users to process; exit
        if(list.isEmpty()){
            log.fine("No users matching search criteria");
    		log.exiting(getClass().getName(), "execute");
            return;
        }
        
        // Put userIDs into list for bulk operation
        ArrayList<String> usrIDs = new ArrayList<String>();
        
        for(User usr : list){
            log.fine("User: "+usr.getId()+" "+usr.getLogin());
            usrIDs.add(usr.getId());
        }
        
        // Bulk disable operation
        UserManagerResult usrmgrResult = usrmgr.enable(usrIDs, false);
        
        // Log operation results
        
        List bulkEnableSucceed = usrmgrResult.getSucceededResults();
        HashMap<String, String> bulkEnableFailed = usrmgrResult.getFailedResults();

        if(!bulkEnableSucceed.isEmpty()){
            log.fine("Succeeded");
            for(Object o : bulkEnableSucceed)
                log.fine("User Key for the Enable User : [" + o.toString()+"]");
        }
        
        if(!bulkEnableFailed.entrySet().isEmpty()){
            log.fine("Failed");
            for(Map.Entry<String, String> entry : bulkEnableFailed.entrySet())
                log.fine("User Key for not Enable User : ["+entry.getKey()+"] Reason of Failure : "+entry.getValue()+"]");
        }          
        
		log.exiting(getClass().getName(), "execute");
		
	}

	@Override
	public HashMap getAttributes() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setAttributes() {
		// TODO Auto-generated method stub
		
	}

}
