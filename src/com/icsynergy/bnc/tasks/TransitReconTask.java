package com.icsynergy.bnc.tasks;

import Thor.API.Exceptions.tcAPIException;
import Thor.API.Operations.tcLookupOperationsIntf;
import com.icsynergy.bnc.Constants;
import oracle.iam.identity.exception.UserSearchException;
import oracle.iam.identity.usermgmt.api.UserManager;
import oracle.iam.identity.usermgmt.api.UserManagerConstants;
import oracle.iam.identity.usermgmt.vo.User;
import oracle.iam.platform.Platform;
import oracle.iam.platform.authz.exception.AccessDeniedException;
import oracle.iam.platform.entitymgr.vo.SearchCriteria;
import oracle.iam.scheduler.api.SchedulerService;
import oracle.iam.scheduler.exception.*;
import oracle.iam.scheduler.vo.JobDetails;
import oracle.iam.scheduler.vo.JobParameter;
import oracle.iam.scheduler.vo.TaskSupport;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.sql.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TransitReconTask extends TaskSupport {
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
    private static final String TRAN_CODE = "TRAN_CODE";
    private static final String SYS_MODIFIED = "SYS_MODIFIED";
    private final Logger log = Logger.getLogger("com.icsynergy");
    private final String PARAM_TOKEN = "Last Seen Token";

    @Override
    public void execute(HashMap hashMap) throws Exception {
        log.entering(getClass().getName(), "execute", hashMap.toString());

        final String strToken = hashMap.containsKey(PARAM_TOKEN) ? (String) hashMap.get(PARAM_TOKEN) : "0";
        log.finer("token=" + strToken);

        final UserManager um = Platform.getService(UserManager.class);

        final String SQL = "select " +
                TRAN_CODE + "," +
                TRAN_ISSUCC + "," +
                TRAN_STREET_NUMBER + "," +
                TRAN_POSTALCODE + "," +
                TRAN_PROVINCE_CODE + "," +
                TRAN_COUNTRY_CODE + "," +
                TRAN_FR_DESCRIPTION + "," +
                TRAN_TYPE_FR_DESCRIPTION + "," +
                TRAN_STREET_NAME_FR + "," +
                TRAN_PROVINCE_EN + "," +
                TRAN_PROVINCE_FR + "," +
                TRAN_SITE_NAME_FR + "," +
                TRAN_CITY_FR + "," +
                TRAN_COUNTRY_FR + "," +
                TRAN_EN_DESCRIPTION + "," +
                TRAN_TYPE_EN_DESCRIPTION + "," +
                TRAN_STREET_NAME_EN + "," +
                TRAN_SITE_NAME_EN + "," +
                TRAN_CITY_EN + "," +
                SYS_MODIFIED + "," +
                TRAN_COUNTRY_EN +
                " from oimportal.tb_tran_transit where sys_modified > ? or sys_modified is null " +
                "order by sys_modified";

        // to save last seen token
        String strLastSeenToken = "";

        try (
                Connection conn = getDatabaseConnection();
                PreparedStatement stmt = conn.prepareStatement(SQL)
        ) {
            stmt.setString(1, strToken);
            ResultSet rs = stmt.executeQuery();

            // criteria for active users
            SearchCriteria critActive =
                    new SearchCriteria(
                            UserManagerConstants.AttributeName.STATUS.getId(),
                            UserManagerConstants.AttributeValues.USER_STATUS_ACTIVE.getId(),
                            SearchCriteria.Operator.EQUAL);

            // language = fr
            SearchCriteria critFr =
                    new SearchCriteria(
                            UserManagerConstants.AttributeName.LANGUAGE.getId(),
                            "fr",
                            SearchCriteria.Operator.EQUAL);

            // language <> fr
            SearchCriteria critNotFr =
                    new SearchCriteria(
                            UserManagerConstants.AttributeName.LANGUAGE.getId(),
                            "fr",
                            SearchCriteria.Operator.NOT_EQUAL);

            while (rs.next()) {
                log.finest("iterating through result set...");

                String strTranCode = rs.getString(TRAN_CODE);
                log.finer("strTransCode=" + strTranCode);

                // users with work transit
                SearchCriteria critTransit =
                        new SearchCriteria(Constants.UserAttributes.WORK_TRANSIT, strTranCode, SearchCriteria.Operator.EQUAL);
                // work transit AND active
                SearchCriteria critActiveWorkTransit =
                        new SearchCriteria(critActive, critTransit, SearchCriteria.Operator.AND);


                // for every language
                for (String strLang: new String[] {"en", "fr"} ) {
                    // combine criteria based on language
                    SearchCriteria critFinal =
                            new SearchCriteria(critActiveWorkTransit,
                                    strLang.equals("fr") ? critFr : critNotFr,
                                    SearchCriteria.Operator.AND);

                    List<User> lstUsr;
                    try {
                        log.finest("performing search for users with language=" + strLang);
                        lstUsr = um.search(
                                critFinal,
                                Collections.singleton(UserManagerConstants.AttributeName.USER_KEY.getId()),
                                null);
                    } catch (AccessDeniedException | UserSearchException e) {
                        log.log(Level.SEVERE, "Exception searching for users", e);
                        continue;
                    }

                    log.finest("preparing attributes for bulk user modification");
                    HashMap<String, Object> mapAttrs = prepareAttributeMap(rs, strLang);
                    log.finer("mapAttrs=" + mapAttrs);

                    log.finest("preparing array of user IDs to modify");
                    ArrayList<String> lst = new ArrayList<>(lstUsr.size());
                    for (User u: lstUsr)
                        lst.add(u.getEntityId());
                    log.finer("lst=" + lst);

                    if (lst.size() > 0) {
                        log.finest("bulk modifying users");
                        um.modify(lst, mapAttrs, false);
                    }
                }

                // save timestamp of the transit record
                strLastSeenToken = rs.getString(SYS_MODIFIED);
            }

            rs.close();

            // update the job with the max timestamp
            updateJob(strLastSeenToken);
        }


        log.exiting(getClass().getName(), "execute");
    }

    /**
     * Method to update scheduled job with a new value of a last modification timestamp
     * @param strNewToken Token to assign
     * @throws SchedulerException in case of exceptions
     * @throws RequiredParameterNotSetException in case of exceptions
     * @throws ParameterValueTypeNotSupportedException in case of exceptions
     * @throws IncorrectScheduleTaskDefinationException in case of exceptions
     * @throws LastModifyDateNotSetException in case of exceptions
     * @throws SchedulerAccessDeniedException in case of exceptions
     */
    private void updateJob(String strNewToken)
            throws SchedulerException, RequiredParameterNotSetException, ParameterValueTypeNotSupportedException,
            IncorrectScheduleTaskDefinationException, LastModifyDateNotSetException, SchedulerAccessDeniedException {

        log.entering(getClass().getName(), "updateJob", strNewToken);

        SchedulerService ss = Platform.getService(SchedulerService.class);

        log.finest("looking up job details");
        JobDetails jobDetails = ss.getJobDetail(getName());

        log.finest("replacing job parameter");
        HashMap<String, JobParameter> map = jobDetails.getParams();
        JobParameter jobParam = map.get(PARAM_TOKEN);
        jobParam.setValue(strNewToken != null ? strNewToken : "");
        map.put(PARAM_TOKEN, jobParam);

        log.finest("updating job parameters");
        jobDetails.setParams(map);

        log.finest("updating job");
        ss.updateJob(jobDetails);

        log.exiting(getClass().getName(), "updateJob");
    }

    /**
     * Method to prepare attribute map for user bulk modify operation based on the language
     * @param rs RecordSet holding values from DB
     * @param strLang Language
     * @return HashMap of user attributes and values
     * @throws SQLException in case of exceptions
     */
    private HashMap<String, Object> prepareAttributeMap(ResultSet rs, String strLang) throws SQLException {
        log.entering(getClass().getName(), "prepareAttributeMap", new Object[] {rs, strLang});
        log.finer("strLang=" + strLang);

        HashMap<String, Object> hm = new HashMap<>();

        hm.put(Constants.UserAttributes.SUCCURSALE, rs.getString(TRAN_ISSUCC));
        hm.put(Constants.UserAttributes.OFFICE_STREET_NUMBER, rs.getString(TRAN_STREET_NUMBER));
        hm.put(UserManagerConstants.AttributeName.POSTAL_CODE.getId(), rs.getString(TRAN_POSTALCODE));
        hm.put(Constants.UserAttributes.PROVINCE_CODE, rs.getString(TRAN_PROVINCE_CODE));
        hm.put(Constants.UserAttributes.COUNTRY_CODE, rs.getString(TRAN_COUNTRY_CODE));


        if (strLang.toLowerCase().equals("fr")) {
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

        log.exiting(getClass().getName(), "prepareAttributeMap", hm);
        return hm;
    }

    /**
     * Method to obtain a database connection to Transit DB
     * @return Connection object
     * @throws tcAPIException in case of exceptions
     * @throws NamingException in case of exceptions
     * @throws SQLException in case of exceptions
     */
    private Connection getDatabaseConnection() throws tcAPIException, NamingException, SQLException {
        log.entering(getClass().getName(), "getDatabaseConnection");

        final String CONFIGLOOKUP = "Lookup.BNC.Transit";
        final String DBJNDINAME = "jdbc jndi name";

        log.finest("getting jdbc datasource JNDI name from a lookup");
        final String dbJNDIName =
                Platform.getService(tcLookupOperationsIntf.class)
                        .getDecodedValueForEncodedValue(CONFIGLOOKUP, DBJNDINAME);

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

    @Override
    public HashMap getAttributes() {
        return null;
    }

    @Override
    public void setAttributes() {

    }
}
