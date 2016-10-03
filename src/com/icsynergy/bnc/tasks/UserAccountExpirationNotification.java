package com.icsynergy.bnc.tasks;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import oracle.iam.identity.usermgmt.api.UserManager;
import oracle.iam.identity.usermgmt.api.UserManagerConstants;
import oracle.iam.identity.usermgmt.vo.User;
import oracle.iam.notification.api.NotificationService;
import oracle.iam.notification.vo.NotificationEvent;
import oracle.iam.platform.Platform;
import oracle.iam.platform.entitymgr.vo.SearchCriteria;
import oracle.iam.scheduler.vo.TaskSupport;

public class UserAccountExpirationNotification extends TaskSupport {

	final private Logger log = Logger.getLogger("com.icsynergy");

	private UserManager userManager;

	@Override
	public void execute(HashMap taskParameters) throws Exception {
		log.entering(getClass().getName(), "execute");
		String templateName = (String) taskParameters.get("Template Name");
		NotificationService notificationService = null;
		Connection con = Platform.getOperationalDS().getConnection();
		String sqlQuery = null;
		PreparedStatement stmt = null;
		long mgrKey = 0;
		String managerLogin = null;
		if (con != null) {
			sqlQuery = "select USR_END_DATE,USR_MANAGER_KEY,USR_DISPLAY_NAME,USR_UDF_FIRSTNAMEUSED,USR_UDF_LASTNAMEUSED, USR_EMP_NO, USR_LOGIN from usr"
					+ " where (USR_END_DATE BETWEEN SYSDATE + 6 AND SYSDATE + 30) AND USR_STATUS='Active'";

			stmt = con.prepareStatement(sqlQuery);
			ResultSet rs = stmt.executeQuery(sqlQuery);
			userManager = Platform.getService(UserManager.class);
			while (rs.next()) {
				Date endDate = rs.getDate("USR_END_DATE");
				String fnUsed = rs.getString("USR_UDF_FIRSTNAMEUSED");
				String lnUsed = rs.getString("USR_UDF_LASTNAMEUSED");
				String empNo = rs.getString("USR_EMP_NO");
				String login = rs.getString("USR_LOGIN");

				long deffDays = getDaysDifference(endDate);
				log.finest("Days remains to expire : " + (deffDays + 1));
				log.finest("Users End Date : " + endDate);

				String daysToExp = null;
				if (deffDays == 29 || deffDays == 6) {
					log.finest("Days remains to expire in if block: " + (deffDays + 1));

					daysToExp = deffDays + 1 + " Days";
					mgrKey = rs.getLong("USR_MANAGER_KEY");
					log.finest("mgrKey : " + mgrKey);
					try {
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
						notificationService = Platform.getService(NotificationService.class);

						HashMap<String, Object> notificationData = new HashMap<String, Object>();
						notificationData.put("templateFirstNameAttribute", fnUsed);
						notificationData.put("templateLastNameAttribute", lnUsed);
						notificationData.put("templateEmpNoAttribute", empNo);
						notificationData.put("templateEndDate", endDate);
						notificationData.put("templateRemainingDays", daysToExp);

						log.finest("notificationData: " + notificationData);
						NotificationEvent event = new NotificationEvent();
						event.setTemplateName(templateName);
						String[] receiverUserIds = { managerLogin };
						event.setUserIds(receiverUserIds);
						event.setParams(notificationData);
						log.info(":: Notification Data added to Event :: " + event);
						notificationService.notify(event);
						log.info(":: Notification Event has been sent" + event);

						log.finest("Email sent ");

					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}

		}
		log.exiting(getClass().getName(), "execute");

	}


	private final long getDaysDifference(Date EndDate) {
		log.entering(getClass().getName(), "getDaysDifference");

		Date CurrentDate = new Date();
		log.finest("validateDateDifference() - Current Date  = " + CurrentDate);

		Calendar end = Calendar.getInstance();
		Calendar current = Calendar.getInstance();
		end.setTime(EndDate);
		current.setTime(CurrentDate);

		long EndInMillis = end.getTimeInMillis();
		long CurrentInMillis = current.getTimeInMillis();
		long DiffMillis = EndInMillis - CurrentInMillis;
		long DiffDays = DiffMillis / (24 * 60 * 60 * 1000);

		log.finest("validateDateDifference() - Difference in Date " + DiffDays + "days !!!");

		log.exiting(getClass().getName(), "getDaysDifference");
		return DiffDays;
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
