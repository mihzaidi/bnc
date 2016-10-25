package com.icsynergy.bnc.handlers;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;

import com.icsynergy.bnc.Constants;

import Thor.API.Operations.tcLookupOperationsIntf;
import oracle.iam.identity.usermgmt.api.UserManagerConstants;
import oracle.iam.platform.Platform;
import oracle.iam.platform.entitymgr.EntityManager;
import oracle.iam.platform.kernel.EventFailedException;
import oracle.iam.platform.kernel.spi.PostProcessHandler;
import oracle.iam.platform.kernel.vo.AbstractGenericOrchestration;
import oracle.iam.platform.kernel.vo.BulkEventResult;
import oracle.iam.platform.kernel.vo.BulkOrchestration;
import oracle.iam.platform.kernel.vo.EventResult;
import oracle.iam.platform.kernel.vo.Orchestration;

public class SetTransitAttributePostCreateUSRHandler implements PostProcessHandler {

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
	private final EntityManager entMgr = Platform.getService(EntityManager.class);

	@Override
	public EventResult execute(long arg0, long arg1, Orchestration orchestration) {
		log.entering(getClass().getName(), "execute");
		String strUserKey = orchestration.getTarget().getEntityId();
		String workTransit = String.valueOf(orchestration.getParameters().get(Constants.UserAttributes.WORK_TRANSIT));
		String language = String.valueOf(orchestration.getParameters().get(Constants.UserAttributes.PREF_LANG));
		log.finest("strUserKey  " + strUserKey + "workTransit = " + workTransit + "language = " + language);
		try {
			if ((!isNullOrEmpty(language)) && (!isNullOrEmpty(workTransit))) {
				Connection transitConnection = getDatabaseConnection();
				processUser(strUserKey, transitConnection, workTransit, language);
			} else {
				log.fine("Work Transit and language is null");
			}
		} catch (Exception e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			throw new EventFailedException("Error processing user", e);
		}

		log.exiting(getClass().getName(), "execute");
		return new EventResult();
	}

	@Override
	public BulkEventResult execute(long arg0, long arg1, BulkOrchestration bulkOrchestration) {
		log.entering(getClass().getName(), "bulk execute");

		String[] arstrUserKeys = bulkOrchestration.getTarget().getAllEntityId();
		HashMap<String, Serializable>[] arParams = bulkOrchestration.getBulkParameters();

		log.finest("getting DB connection");
		Connection transitConnection;
		try {
			transitConnection = getDatabaseConnection();
		} catch (Exception e) {
			throw new EventFailedException("Can't get DB connection", e);
		}

		log.finest(String.format("bulk processing %d users...", arstrUserKeys.length));
		for (int i = 0; i < arstrUserKeys.length; i++) {
			try {
				processUser(arstrUserKeys[i], transitConnection,
						String.valueOf(arParams[i].get(Constants.UserAttributes.WORK_TRANSIT)),
						String.valueOf(arParams[i].get(Constants.UserAttributes.PREF_LANG)));
			} catch (Exception e) {
				log.log(Level.SEVERE, e.getMessage(), e);
				throw new EventFailedException("Exception while processing user with key=" + arstrUserKeys[i], e);
			}
		}

		log.exiting(getClass().getName(), "bulk execute");
		return new BulkEventResult();
	}

	private void processUser(String strUserKey, Connection transitConnection, String workTransit, String language)
			throws Exception {
		log.entering(getClass().getName(), "processUser");
		PreparedStatement stmt = null;
		log.finest("strUserKey  " + strUserKey + "workTransit = " + workTransit + "language = " + language);
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

				HashMap<String, Object> hm = new HashMap<>();
				log.finest("bulk modifying users");
				hm.put(Constants.UserAttributes.SUCCURSALE, rs.getString(TRAN_ISSUCC));
				hm.put(Constants.UserAttributes.OFFICE_STREET_NUMBER, rs.getString(TRAN_STREET_NUMBER));
				hm.put(UserManagerConstants.AttributeName.POSTAL_CODE.getId(), rs.getString(TRAN_POSTALCODE));
				hm.put(Constants.UserAttributes.PROVINCE_CODE, rs.getString(TRAN_PROVINCE_CODE));
				hm.put(Constants.UserAttributes.COUNTRY_CODE, rs.getString(TRAN_COUNTRY_CODE));

				if (language.equalsIgnoreCase("fr")) {
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

				log.finer("strUserKey " + strUserKey);
				log.finer("hm " + hm);
				try {
					entMgr.modifyEntity("User", strUserKey, hm);
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
		log.exiting(getClass().getName(), "processUser");
	}

	/**
	 * Method to obtain a database connection to Transit DB
	 * 
	 * @return Connection object
	 */
	private Connection getDatabaseConnection() throws Exception {
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
				log.log(Level.SEVERE, "Failed to lookup datasource");
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
