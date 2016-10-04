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

public class BNCNotificationEventResolver implements NotificationEventResolver {

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
		String endDateStr="";
		HashMap resolvedData = new HashMap();

		String empNo = (String) params.get("templateEmpNoAttribute");
		String firstName = (String) params.get("templateFirstNameAttribute");
		String lastName = (String) params.get("templateLastNameAttribute");
		String remainingDays = (String) params.get("templateRemainingDays");
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		Date endDate= (Date)params.get("templateEndDate");
		if(null != endDate){
			 endDateStr = dateFormat.format(endDate);
		}
		if(isNullOrEmpty(empNo)){
			empNo="";
		}
		if(isNullOrEmpty(firstName)){
			firstName="";
		}
		if(isNullOrEmpty(lastName)){
			lastName="";
		}
		if(isNullOrEmpty(remainingDays)){
			remainingDays="";
		}
	
		resolvedData.put("notficationTemplateFirstName", firstName);
		resolvedData.put("notficationTemplateLastName", lastName);
		resolvedData.put("notficationTemplateEmpNo", empNo);
		resolvedData.put("notficationTemplateEndDate", endDateStr);
		resolvedData.put("notficationTemplateRemainingDays", remainingDays);

		logger.info("Resolved Data:" + resolvedData);
		logger.exiting(getClass().getName(), methodName);

		return resolvedData;
	}
}