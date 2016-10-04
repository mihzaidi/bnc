package com.icsynergy.bnc.handlers;


import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.icsynergy.bnc.Constants;

import Thor.API.Exceptions.tcAPIException;
import Thor.API.Operations.tcLookupOperationsIntf;
import oracle.iam.identity.exception.UserMembershipException;
import oracle.iam.identity.exception.UserSearchException;
import oracle.iam.identity.rolemgmt.api.RoleManager;
import oracle.iam.identity.usermgmt.api.UserManager;
import oracle.iam.identity.usermgmt.api.UserManagerConstants;
import oracle.iam.identity.usermgmt.vo.User;
import oracle.iam.identity.vo.Identity;
import oracle.iam.notification.api.NotificationService;
import oracle.iam.notification.exception.EventException;
import oracle.iam.notification.exception.MultipleTemplateException;
import oracle.iam.notification.exception.NotificationException;
import oracle.iam.notification.exception.NotificationResolverNotFoundException;
import oracle.iam.notification.exception.TemplateNotFoundException;
import oracle.iam.notification.exception.UnresolvedNotificationDataException;
import oracle.iam.notification.exception.UserDetailsNotFoundException;
import oracle.iam.notification.vo.NotificationEvent;
import oracle.iam.platform.Platform;
import oracle.iam.platform.authz.exception.AccessDeniedException;
import oracle.iam.platform.entitymgr.vo.SearchCriteria;
import oracle.iam.platform.kernel.EventFailedException;
import oracle.iam.platform.kernel.spi.PostProcessHandler;
import oracle.iam.platform.kernel.vo.AbstractGenericOrchestration;
import oracle.iam.platform.kernel.vo.BulkEventResult;
import oracle.iam.platform.kernel.vo.BulkOrchestration;
import oracle.iam.platform.kernel.vo.EventResult;
import oracle.iam.platform.kernel.vo.Orchestration;
import oracle.iam.identity.rolemgmt.vo.Role;

public class UserExpirationNotificationHandler implements PostProcessHandler{
	final String CONFIGLOOKUP = "Lookup.BNC.Notifications";
    tcLookupOperationsIntf lookupOperationsIntf = Platform.getService(tcLookupOperationsIntf.class);
	RoleManager roleMgr = Platform	.getService(RoleManager.class);
	UserManager userManager;
	
	private final Logger log = Logger.getLogger("com.icsynergy");
	public EventResult execute(long arg0, long arg1, Orchestration orchestration) {
		log.entering(getClass().getName(), "execute");
		userManager = Platform.getService(UserManager.class);
		try {
			Identity newUserState = (Identity) getNewUserStates(orchestration);
			Identity oldUserState = (Identity) getOldUserStates(orchestration);

			ProcessUser(newUserState,oldUserState,orchestration.getTarget().getEntityId());
			
		} catch (Exception e) {
			throw new EventFailedException("Exception in processing", null, e);
		}

		log.exiting(getClass().getName(), "execute");
		return new EventResult();
	}
	
	@Override
	public BulkEventResult execute(long arg0, long arg1, BulkOrchestration bulkOrchestration) {
		log.entering(getClass().getName(), "bulk execute");
		try {

			String[] entityIds = bulkOrchestration.getTarget().getAllEntityId();
			Identity[] oldUserStatesIdntArr = (Identity[]) getOldUserStates(bulkOrchestration);
			Identity[] newUserStatesIdntArr = (Identity[]) getNewUserStates(bulkOrchestration);
			for (int i = 0; i < entityIds.length; i++) {
				Identity newUserState = newUserStatesIdntArr != null ? newUserStatesIdntArr[i] : null;
				Identity oldUserState = oldUserStatesIdntArr != null ? oldUserStatesIdntArr[i] : null;
				ProcessUser(newUserState, oldUserState, entityIds[i]);
				}

		} catch (Exception e) {
			throw new EventFailedException("Exception in bulk processing", null, e);

		}
		log.exiting(getClass().getName(), "bulk execute");
		return new BulkEventResult();
	}
	private void ProcessUser(Identity newUserState, Identity oldUserState, String userKey) {
		long mgrKey = 0;
		String managerLogin = null;
		String templateName =null;
		NotificationService notificationService = null;
		Date currentDate = new Date();
		String mailIntroductionFR= null;
		String mailIntroductionEN = null;

//		String mailIntroductionFRKey_1 = "mailIntroductionFRKey1";
//		String mailIntroductionENKey_1 = "mailIntroductionENKey1";
//		String mailIntroductionFRKey_2 = "mailIntroductionFRKey2";
//		String mailIntroductionENKey_2 = "mailIntroductionENKey2";	
		boolean isUserStatusChanged = isUserStatusChanged(newUserState, oldUserState);

		Date endDate= (Date) newUserState.getAttribute(UserManagerConstants.AttributeName.ACCOUNT_END_DATE.getId());
		String empNo= (String) newUserState.getAttribute(UserManagerConstants.AttributeName.EMPLOYEE_NUMBER.getId());
		String fnUsed= (String) newUserState.getAttribute(Constants.UserAttributes.FirstNameUsed);
		String lnUsed= (String) newUserState.getAttribute(Constants.UserAttributes.LastNameUsed);
		mgrKey= (long) newUserState.getAttribute(UserManagerConstants.AttributeName.MANAGER_KEY.getId());
		String userType = (String) newUserState.getAttribute(UserManagerConstants.AttributeName.EMPTYPE.getId());
		try {
			templateName = lookupOperationsIntf.getDecodedValueForEncodedValue(CONFIGLOOKUP, "templateName");
			if (mgrKey != 0) {
				SearchCriteria criteria = new SearchCriteria(
						UserManagerConstants.AttributeName.USER_KEY.getId(), mgrKey,
						SearchCriteria.Operator.EQUAL);
				Set<String> retAttrs = new HashSet<String>();
				retAttrs.add(UserManagerConstants.AttributeName.EMAIL.getId());
				retAttrs.add(UserManagerConstants.AttributeName.USER_LOGIN.getId());
				List<User> users = userManager.search(criteria, retAttrs, null);
				for (User user : users) {
					managerLogin = (String) user
							.getAttribute(UserManagerConstants.AttributeName.USER_LOGIN.getId());
					log.finest("User's Manager User Id: " + managerLogin);
				}
			}
		
			log.finest("userType " + userType);
			log.finest("endDate " + endDate);
		if ((isUserStatusChanged || endDate.before(new Date())) && 
				(userType.equalsIgnoreCase("1") || userType.equalsIgnoreCase("2"))) {
			if(userType.equalsIgnoreCase("1")){
			   mailIntroductionFR = "Dans le cadre de l’initiative d’amélioration de la gestion des accès, nous avons instauré une mesure de désactivation partielle des accès suite à la notification des Ressources Humaines du départ permanent d’un employé. En tant que gestionnaire de cet employé, nous vous faisons parvenir cet avis de désactivation des accès pour :";
			   mailIntroductionEN = "As part of the activities to improve the access management, we implemented a measure to partially deactivate accesses following the Human Resources notification that an employee permanently left the Bank. As the manager of the employee, we are sending you this access deactivation notice for :";
			}
			else if(userType.equalsIgnoreCase("2")){
				  mailIntroductionFR = "Dans le cadre de l’initiative d’amélioration de la gestion des accès et afin d’optimiser le contrôle des accès aux environnements TI, nous avons instauré une mesure de désactivation partielle des accès suite à l’atteinte de la date de départ du consultant. En tant que gestionnaire de ce consultant, nous vous faisons arvenir cet avis de désactivation des accès pour :";
				  mailIntroductionEN = "As part of the activities to improve the access management and in order to optimize the control of access to IT environments, we implemented a measure to partially deactivate accesses when the consultant’s departure date is reached. As the manager of the consultant, we are sending you this access deactivation notice for :";	
			}
		}	
		
		log.info(":: mailIntroductionFR :: " + mailIntroductionFR + ":: mailIntroductionEN :: " + mailIntroductionEN);

	
		List<Role> userRoles = roleMgr.getUserMemberships(userKey, false);
		List<String> userRolesName = new ArrayList<String>();
		for (Role role : userRoles) {
			role.getDisplayName();
			userRolesName.add(role.getDisplayName());
		}
		
		log.info(":: userRolesName :: " + userRolesName);

		notificationService = Platform.getService(NotificationService.class);

		HashMap<String, Object> notificationData = new HashMap<String, Object>();
		notificationData.put("templateFirstNameAttribute", fnUsed);
		notificationData.put("templateLastNameAttribute", lnUsed);
		notificationData.put("templateEmpNoAttribute", empNo);
		notificationData.put("templateUsersRole", userRolesName);
		notificationData.put("templatemailIntroductionEN", mailIntroductionEN);
		notificationData.put("templatemailIntroductionFR", mailIntroductionFR);
		notificationData.put("templatecurrentDate", currentDate);
		notificationData.put("templateEndDate", endDate);

		log.finest("templateName: " + templateName);

		log.finest("notificationData: " + notificationData);
		NotificationEvent event = new NotificationEvent();
		String[] receiverUserIds = { managerLogin };
		event.setUserIds(receiverUserIds);
		event.setTemplateName(templateName);
		event.setParams(notificationData);
		log.info(":: Notification Data added to Event :: " + event);
		notificationService.notify(event);
		log.info(":: Notification Event has been sent" + event);
		
		log.finest("Email sent ");
		} catch (UserMembershipException | AccessDeniedException | UserSearchException | tcAPIException | UserDetailsNotFoundException | EventException |UnresolvedNotificationDataException |
				TemplateNotFoundException | MultipleTemplateException | NotificationResolverNotFoundException |NotificationException e) {
			log.log(Level.SEVERE, "Exception : ", e);
		}
	}

	private boolean isUserStatusChanged(Identity newUserState, Identity oldUserState) {
		String oldStatus = null, newStatus = null;

		if (oldUserState != null) {
			oldStatus = (String) oldUserState.getAttribute(UserManagerConstants.AttributeName.STATUS.getName());
			log.fine("Old Status: " + oldStatus);
		}

		if (newUserState != null) {
			newStatus = (String) newUserState.getAttribute(UserManagerConstants.AttributeName.STATUS.getName());
			log.fine("Current Status: " + newStatus);
		}
		if (!oldStatus.equals(newStatus)&& "Active".equalsIgnoreCase(oldStatus) && "Disabled".equalsIgnoreCase(newStatus)) {
			log.fine("Status is changed from " + oldStatus + " to " + newStatus);
			return true;
		} else {
			log.fine("status is same, no action is required");
			return false;
		}
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
			newUserStates = interEventData.get("NEW_USER_STATE");

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
			oldUserStates = interEventData.get("CURRENT_USER");

		log.exiting(getClass().getName(), "getOldUserStates");
		return oldUserStates;
	}


	@Override
	public void initialize(HashMap<String, String> arg0) {
		// TODO Auto-generated method stub
		
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
	
	private boolean isNullOrEmpty(String strCheck) {
		return (strCheck == null) || strCheck.equals("null") || strCheck.trim().length() == 0;
	}

	
}
