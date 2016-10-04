package com.icsynergy.bnc;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import oracle.iam.notification.impl.NotificationEventResolver;
import oracle.iam.notification.vo.NotificationAttribute;

public class BNCLeaveNotificationResolver implements NotificationEventResolver {

	final private Logger logger = Logger.getLogger("com.icsynergy");

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public List<NotificationAttribute> getAvailableData(String eventType, Map<String, Object> params) throws Exception {
		String methodName = "getAvailableData";

		logger.entering(getClass().getName(), methodName);
		logger.info("Input Parameters: Event Type:" + eventType + " Params:" + params);

		List attributes = new ArrayList();

		logger.info("Attributes:" + attributes);
		logger.exiting(getClass().getName(), methodName);

		return attributes;
	}

	private boolean isNullOrEmpty(String strCheck) {
		return (strCheck == null) || strCheck.equals("null") || strCheck.trim().length() == 0;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public HashMap<String, Object> getReplacedData(String eventType, Map<String, Object> params) throws Exception {
		String methodName = "getReplacedData";
		logger.entering(getClass().getName(), methodName);
		logger.info("Input Parameters: Event Type:" + eventType + " Params: " + params);

		HashMap resolvedData = new HashMap();
		String enddatestr = "";
		String currentDatestr = "";
		String empNo = (String) params.get("templateEmpNoAttribute");
		String firstName = (String) params.get("templateFirstNameAttribute");
		String lastName = (String) params.get("templateLastNameAttribute");
		String emailIntroductionEN = (String) params.get("templatemailIntroductionEN");
		String emailIntroductionFR = (String) params.get("templatemailIntroductionFR");

		List<String> userRolesName = new ArrayList<String>();
		userRolesName = (List<String>) params.get("templateUsersRole");

		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		Date endDate = (Date) params.get("templateEndDate");
		if (null != endDate) {
			enddatestr = dateFormat.format(endDate);
		}
		Date currentDate = (Date) params.get("templatecurrentDate");
		if (null != currentDate) {
			currentDatestr = dateFormat.format(currentDate);
		}
		if (isNullOrEmpty(empNo)) {
			empNo = "";
		}
		if (isNullOrEmpty(firstName)) {
			firstName = "";
		}
		if (isNullOrEmpty(lastName)) {
			lastName = "";
		}
		if (isNullOrEmpty(emailIntroductionEN)) {
			emailIntroductionEN = "";
		}
		if (isNullOrEmpty(emailIntroductionFR)) {
			emailIntroductionFR = "";
		}

		/*
		 * if(isNullOrEmpty(usersRole)){ usersRole=""; }
		 */
		resolvedData.put("notficationTemplateFirstName", firstName);
		resolvedData.put("notficationTemplateLastName", lastName);
		resolvedData.put("notficationTemplateEmpNo", empNo);
		resolvedData.put("notficationTemplateEndDate", enddatestr);
		resolvedData.put("notficationTemplateemailIntroductionEN", emailIntroductionEN);
		resolvedData.put("notficationTemplateemailIntroductionFR", emailIntroductionFR);
		resolvedData.put("notficationTemplatecurrentDate", currentDatestr);
		resolvedData.put("notficationTemplateUsersRole", userRolesName);

		/*
		 * for(int i=0; i<=userRolesName.size();i++){
		 * resolvedData.put("notficationTemplateUsersRole"+i,
		 * userRolesName.get(i));
		 * 
		 * }
		 */

		logger.info("Resolved Data:" + resolvedData);
		logger.exiting(getClass().getName(), methodName);

		return resolvedData;
	}
}