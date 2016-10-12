package com.icsynergy.bnc.handlers;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import Thor.API.tcResultSet;
import Thor.API.Exceptions.tcAPIException;

import Thor.API.Exceptions.tcFormNotFoundException;
import Thor.API.Exceptions.tcNotAtomicProcessException;
import Thor.API.Exceptions.tcProcessNotFoundException;
import Thor.API.Operations.tcFormInstanceOperationsIntf;
import oracle.iam.identity.usermgmt.api.UserManager;
import oracle.iam.identity.usermgmt.api.UserManagerConstants;
import oracle.iam.identity.usermgmt.vo.User;
import oracle.iam.platform.Platform;
import oracle.iam.platform.authz.exception.AccessDeniedException;
import oracle.iam.platform.kernel.spi.PostProcessHandler;
import oracle.iam.platform.kernel.vo.AbstractGenericOrchestration;
import oracle.iam.platform.kernel.vo.BulkEventResult;
import oracle.iam.platform.kernel.vo.BulkOrchestration;
import oracle.iam.platform.kernel.vo.EventResult;
import oracle.iam.platform.kernel.vo.Orchestration;
import oracle.iam.provisioning.api.ProvisioningService;
import oracle.iam.provisioning.exception.AccountNotFoundException;
import oracle.iam.provisioning.exception.GenericProvisioningException;
import oracle.iam.provisioning.exception.UserNotFoundException;
import oracle.iam.provisioning.vo.Account;

public class DisableLBGAccountHandler implements PostProcessHandler {
	private Logger log = Logger.getLogger("com.icsynergy");
	ProvisioningService provService = (ProvisioningService) Platform.getService(ProvisioningService.class);
	Long userKey = null;

	@Override
	public EventResult execute(long arg0, long arg1, Orchestration orchestration) {
		log.entering(getClass().getName(), "execute");
		HashMap<String, Serializable> modParams = orchestration.getParameters();
    	String usrKeyStr = (String) modParams.get("BeneficiaryKey");

		log.info("usrKeyStr [" + usrKeyStr + "]");
		log.info("orchestration.getTarget().getEntityId() : execute"+ orchestration.getTarget().getEntityId());
		disableADAccount(usrKeyStr);
		return new EventResult();
	}

	private void disableADAccount(String userKeyStr) {
		ProvisioningService provService = Platform.getService(ProvisioningService.class);
		List<Account> listAccount;
		tcFormInstanceOperationsIntf formOperations = Platform.getService(tcFormInstanceOperationsIntf.class);
		try {
			Long userKey = Long.parseLong(userKeyStr);
			listAccount = provService.getAccountsProvisionedToUser(userKeyStr);
			tcResultSet provisionedProcessFromData = null;
			for (Account account : listAccount) {
				String appplInstName = account.getAppInstance().getApplicationInstanceName();
				log.info("appplInst [" + appplInstName + "]");
				String accountStatus = account.getAccountStatus();
				log.info("accountStatus [" + accountStatus + "]");
				UserManager userManager = Platform.getService(UserManager.class);
				Set<String> userAttribs = null;
				User usrDetails = null;
				Date startDate = null;
				Date endDate = null;

				try {
					usrDetails = (User) userManager.getDetails(UserManagerConstants.AttributeName.USER_KEY.getId(),
							userKey, userAttribs);
				} catch (Exception e) {
					log.log(Level.SEVERE, "Exception", e.toString());
				}

				if (usrDetails != null) {
					startDate = (Date) usrDetails
							.getAttribute(UserManagerConstants.AttributeName.ACCOUNT_START_DATE.getId());
					endDate = (Date) usrDetails
							.getAttribute(UserManagerConstants.AttributeName.ACCOUNT_END_DATE.getId());
				}
				if (appplInstName.toLowerCase().contains("lbg")) {
					System.out.println("Account is lbg Account");
					String processInstanceKey = account.getProcessInstanceKey();
					Long processInstanceKeyL = Long.parseLong(processInstanceKey);
					provisionedProcessFromData = formOperations.getProcessFormData(processInstanceKeyL);

					if (accountStatus.equalsIgnoreCase("Provisioned")) {
						provService.disable(Long.parseLong(account.getAccountID()));

					} else {
						log.info("AD LBG account with application Instance Name [" + appplInstName + "]"
								+ " is not Provisioned.");
					}
				}
				if (appplInstName.toLowerCase().contains("res") || appplInstName.toLowerCase().contains("succ")) {
					System.out.println("Account is res Account");
					if (endDate != null && startDate != null) {
						if (!startDate.before(new Date()) && !endDate.before(new Date())) {
							log.info(
									"startDate [" + startDate + "] endDate [" + endDate + "] so not disabling account");
						} else {
							log.info("startDate [" + startDate + "] endDate [" + endDate + "]");

							if (accountStatus.equalsIgnoreCase("Provisioned")) {
								provService.disable(Long.parseLong(account.getAccountID()));

							} else {
								log.info("AD LBG account with application Instance Name [" + appplInstName + "]"
										+ " is not Provisioned.");
							}
						}
					}
				}
			}
		} catch (UserNotFoundException e) {
			log.log(Level.SEVERE, "Exception", e.toString());
		} catch (GenericProvisioningException e) {
			log.log(Level.SEVERE, "Exception", e.toString());
		} catch (tcAPIException e) {
			log.log(Level.SEVERE, "Exception", e.toString());
		} catch (tcNotAtomicProcessException e) {
			log.log(Level.SEVERE, "Exception", e.toString());
		} catch (tcFormNotFoundException e) {
			log.log(Level.SEVERE, "Exception", e.toString());
		} catch (tcProcessNotFoundException e) {
			log.log(Level.SEVERE, "Exception", e.toString());
		} catch (AccessDeniedException e) {
			log.log(Level.SEVERE, "Exception", e.toString());
		} catch (NumberFormatException e) {
			log.log(Level.SEVERE, "Exception", e.toString());
		} catch (AccountNotFoundException e) {
			log.log(Level.SEVERE, "Exception", e.toString());
		} catch (Exception e) {
			log.log(Level.SEVERE, "Exception", e.toString());
		}

	}

	@Override
	public BulkEventResult execute(long processId, long l1, BulkOrchestration bulkOrchestration) {
		log.entering(getClass().getName(), "bulk execute");

		HashMap<String, Serializable>[] bulkParameters = bulkOrchestration.getBulkParameters();
		String[] entityIds = bulkOrchestration.getTarget().getAllEntityId();

		for (int i = 0; i < entityIds.length; i++) {

			HashMap<String, Serializable> modParams = bulkParameters[i];
			String usrKeyStr = (String) modParams.get("BeneficiaryKey");
			log.info("usrKeyStr [" + usrKeyStr + "]");
			disableADAccount(usrKeyStr);
			log.info("orchestration.getTarget().getEntityId() : BulkEventResult"+ entityIds[i]);

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
