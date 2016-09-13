package com.icsynergy.bnc.handlers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import com.icsynergy.bnc.Constants;

import Thor.API.Exceptions.tcAPIException;
import Thor.API.Operations.tcLookupOperationsIntf;
import oracle.iam.identity.usermgmt.api.UserManagerConstants;
import oracle.iam.identity.vo.Identity;
import oracle.iam.platform.Platform;
import oracle.iam.platform.entitymgr.EntityManager;
import oracle.iam.platform.kernel.EventFailedException;
import oracle.iam.platform.kernel.spi.PostProcessHandler;
import oracle.iam.platform.kernel.vo.AbstractGenericOrchestration;
import oracle.iam.platform.kernel.vo.BulkEventResult;
import oracle.iam.platform.kernel.vo.BulkOrchestration;
import oracle.iam.platform.kernel.vo.EventResult;
import oracle.iam.platform.kernel.vo.Orchestration;

public class SetTransitAttributeHandler implements PostProcessHandler {

	private final Logger log = Logger.getLogger("com.icsynergy");
	private static final String TRAN_ISSUCC = "TRAN_ISSUCC";
	private static final String TRAN_STREET_NUMBER = "TRAN_STREET_NUMBER";
	private static final String TRAN_POSTALCODE = "TRAN_POSTALCODE";
	private static final String TRAN_PROVINCE_CODE = "TRAN_PROVINCE_CODE";
	private static final String TRAN_PROVINCE_EN = "TRAN_PROVINCE_EN";
	private static final String TRAN_PROVINCE_FR = "TRAN_PROVINCE_FR";
	private static final String TRAN_COUNTRY_CODE = "TRAN_COUNTRY_CODE";
	private static final String TRAN_FR_DESCRIPTION = "TRAN_FR_DESCRIPTION";
	private static final String TRAN_TYPE_FR_DESCRIPTION = "TRAN_TYPE_FR_DESCRIPTION";
	private static final String TRAN_STREET_NAME_FR = "TRAN_STREET_NAME_FR";
	private static final String TRAN_SITE_NAME_FR = "TRAN_SITE_NAME_FR";
	private static final String TRAN_CITY_FR = "TRAN_CITY_FR";
	private static final String TRAN_COUNTRY_FR = "TRAN_COUNTRY_FR";
	private static final String TRAN_EN_DESCRIPTION = "TRAN_EN_DESCRIPTION";
	private static final String TRAN_TYPE_EN_DESCRIPTION = "TRAN_TYPE_EN_DESCRIPTION";
	private static final String TRAN_STREET_NAME_EN = "TRAN_STREET_NAME_EN";
	private static final String TRAN_SITE_NAME_EN = "TRAN_SITE_NAME_EN";
	private static final String TRAN_CITY_EN = "TRAN_CITY_EN";
	private static final String TRAN_COUNTRY_EN = "TRAN_COUNTRY_EN";
	final EntityManager entMgr = Platform.getService(EntityManager.class);

	@Override
	public EventResult execute(long arg0, long arg1, Orchestration orchestration) {
		log.entering(getClass().getName(), "execute");
		Connection transitConnection = null;
		String targetType = null;
		String targetID = null;
		try {
			Identity newUserState = (Identity) getNewUserStates(orchestration);
			Identity oldUserState = (Identity) getOldUserStates(orchestration);

			String workTransitAndLanguage = isWorkingTransitChanged(newUserState, oldUserState);
			if (!isNullOrEmpty(workTransitAndLanguage)) {
				String workTransit[] = workTransitAndLanguage.split("~");
				log.fine("Work Transit : 0 " + workTransit[0] + "Work Transit 1: " + workTransit[1]);
				transitConnection = getDatabaseConnection();
				targetType = orchestration.getTarget().getType();
				targetID = newUserState.getEntityId();
				processUser(orchestration.getTarget().getEntityId(), transitConnection, workTransit[0].trim(),
						workTransit[1].trim(), targetType, targetID);
			}
		} catch (Exception e) {
			throw new EventFailedException("Exception in processing", null, e);
		} finally {
			if (transitConnection != null) {
				try {
					transitConnection.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					log.log(Level.SEVERE, "Exception while closing connection", e);
				}
			}
		}

		log.exiting(getClass().getName(), "execute");
		return new EventResult();
	}

	@Override
	public BulkEventResult execute(long arg0, long arg1, BulkOrchestration bulkOrchestration) {
		log.entering(getClass().getName(), "bulk execute");
		Connection transitConnection = null;

		try {

			String[] entityIds = bulkOrchestration.getTarget().getAllEntityId();
			Identity[] oldUserStatesIdntArr = (Identity[]) getOldUserStates(bulkOrchestration);
			Identity[] newUserStatesIdntArr = (Identity[]) getNewUserStates(bulkOrchestration);
			transitConnection = getDatabaseConnection();
			for (int i = 0; i < entityIds.length; i++) {
				String targetType = null;
				String targetID = null;
				Identity newUserState = newUserStatesIdntArr != null ? newUserStatesIdntArr[i] : null;
				Identity oldUserState = oldUserStatesIdntArr != null ? oldUserStatesIdntArr[i] : null;

				targetType = bulkOrchestration.getTarget().getType();
				targetID = newUserState.getEntityId();
				String workTransitAndLanguage = isWorkingTransitChanged(newUserState, oldUserState);

				if (!isNullOrEmpty(workTransitAndLanguage)) {
					String workTransit[] = workTransitAndLanguage.split("~");
					if (!isNullOrEmpty(workTransit[1])) {
						processUser(entityIds[i], transitConnection, workTransit[0].trim(), workTransit[1].trim(),
								targetType, targetID);
					} else {
						log.fine("Preffered language is null or empty.");
					}
				}
			}
		} catch (Exception e) {
			throw new EventFailedException("Exception in bulk processing", null, e);
		} finally {
			if (transitConnection != null) {
				try {
					transitConnection.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					log.log(Level.SEVERE, "Exception while closing connection", e);
				}
			}
		}
		log.exiting(getClass().getName(), "bulk execute");
		return new BulkEventResult();
	}

	private String isWorkingTransitChanged(Identity newUserState, Identity oldUserState) {
		String oldWorkTransit = null, workTransit = null, language = null;

		if (oldUserState != null) {
			oldWorkTransit = (String) oldUserState.getAttribute(Constants.UserAttributes.WORK_TRANSIT);
			log.fine("Old Work Transit: " + oldWorkTransit);
		}

		if (newUserState != null) {
			workTransit = (String) newUserState.getAttribute(Constants.UserAttributes.WORK_TRANSIT);
			log.fine("Current Work Transit: " + workTransit);
			language = (String) newUserState.getAttribute(Constants.UserAttributes.PREF_LANG);
			log.fine("Language: " + language);
		}
		if ((!oldWorkTransit.equals(workTransit)) && (!isNullOrEmpty(workTransit))) {
			log.fine("workTransit language" + workTransit + "~" + language);
			return workTransit + "~" + language;
		} else {
			log.fine("Work Transit is same, no action is required");
			return "";
		}
	}

	private void processUser(String strUserKey, Connection transitConnection, String workTransit, String language,
			String targetType, String targetID) throws tcAPIException, SQLException, Exception {
		log.entering(getClass().getName(), "processUser");
		PreparedStatement stmt = null;

		final String SQL = "select " + TRAN_ISSUCC + "," + TRAN_STREET_NUMBER + "," + TRAN_POSTALCODE + ","
				+ TRAN_PROVINCE_CODE + "," + TRAN_COUNTRY_CODE + "," + TRAN_FR_DESCRIPTION + ","
				+ TRAN_TYPE_FR_DESCRIPTION + "," + TRAN_STREET_NAME_FR + "," + TRAN_PROVINCE_EN + "," + TRAN_PROVINCE_FR
				+ "," + TRAN_SITE_NAME_FR + "," + TRAN_CITY_FR + "," + TRAN_COUNTRY_FR + "," + TRAN_EN_DESCRIPTION + ","
				+ TRAN_TYPE_EN_DESCRIPTION + "," + TRAN_STREET_NAME_EN + "," + TRAN_SITE_NAME_EN + "," + TRAN_CITY_EN
				+ "," + TRAN_COUNTRY_EN + " from tb_tran_transit where TRAN_CODE = ?";

		log.finest("SQL..." + SQL);

		try {
			stmt = transitConnection.prepareStatement(SQL);
			stmt.setString(1, workTransit);
			ResultSet rs = stmt.executeQuery();
			log.finest("rs..." + rs);

			while (rs.next()) {
				/*
				 * log.finest("rs.getString(TRAN_ISSUCC) .." +
				 * rs.getString(TRAN_ISSUCC)); log.finest(
				 * "rs.getString(TRAN_STREET_NUMBER) .." +
				 * rs.getString(TRAN_STREET_NUMBER)); log.finest(
				 * "rs.getString(TRAN_EN_DESCRIPTION) .." +
				 * rs.getString(TRAN_EN_DESCRIPTION)); log.finest(
				 * "rs.getString(TRAN_CITY_FR) .." +
				 * rs.getString(TRAN_CITY_FR)); log.finest(
				 * "rs.getString(TRAN_PROVINCE_CODE) .." +
				 * rs.getString(TRAN_PROVINCE_CODE)); log.finest(
				 * "rs.getString(TRAN_PROVINCE_EN) .." +
				 * rs.getString(TRAN_PROVINCE_EN)); log.finest(
				 * "rs.getString(TRAN_COUNTRY_EN) .." +
				 * rs.getString(TRAN_COUNTRY_EN)); log.finest(
				 * "rs.getString(TRAN_POSTALCODE) .." +
				 * rs.getString(TRAN_POSTALCODE)); log.finest(
				 * "iterating through result set...");
				 */

				HashMap<String, Object> hm = new HashMap<String, Object>();
				log.finest("bulk modifying users");
				hm.put(Constants.UserAttributes.SUCCURSALE, rs.getString(TRAN_ISSUCC));
				hm.put(Constants.UserAttributes.OFFICE_STREET_NUMBER, rs.getString(TRAN_STREET_NUMBER));
				hm.put(UserManagerConstants.AttributeName.POSTAL_CODE.getId(), rs.getString(TRAN_POSTALCODE));
				hm.put(Constants.UserAttributes.PROVINCE_CODE, rs.getString(TRAN_PROVINCE_CODE));
				hm.put(Constants.UserAttributes.COUNTRY_CODE, rs.getString(TRAN_COUNTRY_CODE));

				if (language.toLowerCase().equals("fr")) {
					log.finest("Preffered Language is fr");
					hm.put(Constants.UserAttributes.TRANSIT_DESCRIPTION, rs.getString(TRAN_FR_DESCRIPTION));
					hm.put(Constants.UserAttributes.TRANSIT_TYPE_DESCRIPTION, rs.getString(TRAN_TYPE_FR_DESCRIPTION));
					hm.put(Constants.UserAttributes.OFFICE_STREET_NAME, rs.getString(TRAN_STREET_NAME_FR));
					hm.put(Constants.UserAttributes.SITE_NAME, rs.getString(TRAN_SITE_NAME_FR));
					hm.put(Constants.UserAttributes.TRAN_CITY, rs.getString(TRAN_CITY_FR));
					hm.put(Constants.UserAttributes.PROVINCE, rs.getString(TRAN_PROVINCE_FR));
					hm.put(UserManagerConstants.AttributeName.USER_COUNTRY.getId(), rs.getString(TRAN_COUNTRY_FR));
				} else {
					hm.put(Constants.UserAttributes.TRANSIT_DESCRIPTION, rs.getString(TRAN_EN_DESCRIPTION));
					hm.put(Constants.UserAttributes.TRANSIT_TYPE_DESCRIPTION, rs.getString(TRAN_TYPE_EN_DESCRIPTION));
					hm.put(Constants.UserAttributes.OFFICE_STREET_NAME, rs.getString(TRAN_STREET_NAME_EN));
					hm.put(Constants.UserAttributes.SITE_NAME, rs.getString(TRAN_SITE_NAME_EN));
					hm.put(Constants.UserAttributes.TRAN_CITY, rs.getString(TRAN_CITY_EN));
					hm.put(Constants.UserAttributes.PROVINCE, rs.getString(TRAN_PROVINCE_EN));
					hm.put(UserManagerConstants.AttributeName.USER_COUNTRY.getId(), rs.getString(TRAN_COUNTRY_EN));
				}
				try {
					log.fine("targetType " + targetType);
					log.fine("targetID " + targetID);
					log.fine("hm " + hm);
					entMgr.modifyEntity(targetType, targetID, hm);
					log.fine("User modified");
				} catch (Exception e) {
					log.log(Level.SEVERE, "Exception modifying user", e);
					throw new EventFailedException("Exception modifying user", e);
				}
			}

		} finally {
			if (stmt != null) {
				try {
					stmt.close();
				} catch (SQLException e) {
					log.log(Level.SEVERE, "Exception while closing preparedStatement", e);
				}

			}
		}

	}

	/**
	 * Method to obtain a database connection to Transit DB
	 * 
	 * @return Connection object
	 * @throws tcAPIException
	 *             in case of exceptions
	 * @throws NamingException
	 *             in case of exceptions
	 * @throws SQLException
	 *             in case of exceptions
	 */
	private Connection getDatabaseConnection() throws tcAPIException, NamingException, SQLException {
		log.entering(getClass().getName(), "getDatabaseConnection");

		final String CONFIGLOOKUP = "Lookup.BNC.Transit";
		final String DBJNDINAME = "jdbc jndi name";

		log.finest("getting jdbc datasource JNDI name from a lookup");
		final String dbJNDIName = Platform.getService(tcLookupOperationsIntf.class)
				.getDecodedValueForEncodedValue(CONFIGLOOKUP, DBJNDINAME);

		log.fine("dbJNDIName: " + dbJNDIName);
		if (isNullOrEmpty(dbJNDIName))
			throw new RuntimeException("Can't get jdbc datasource name from the lookup");

		Connection con;
		Context initialContext = new InitialContext();

		try {
			DataSource datasource = (DataSource) initialContext.lookup(dbJNDIName.trim());

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

	private boolean isNullOrEmpty(String strCheck) {
		return (strCheck == null) || strCheck.equals("null") || strCheck.trim().length() == 0;
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
}
