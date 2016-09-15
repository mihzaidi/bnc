package com.icsynergy.bnc.adapters;

import Thor.API.Exceptions.*;
import Thor.API.Operations.tcITResourceInstanceOperationsIntf;
import Thor.API.Operations.tcLookupOperationsIntf;
import Thor.API.tcResultSet;
import com.icsynergy.bnc.OIMUtility;
import oracle.iam.platform.Platform;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SetLDAPObjectClass {
    private final String CONFIGURATION_LOOKUP = "Configuration Lookup";
    private final String UID_ATTRIBUTE = "uidAttribute";
    private final String PRINCIPAL = "principal";
    private final String CREDENTIALS = "credentials";
    private final String PORT = "port";
    private final String HOST = "host";
    private final String SSL = "ssl";

    private final String ACCOUNT_OBJECT_CLASSES = "accountObjectClasses";

    private final String ACNT_TYPE = "UD_LDAP_USR_ACCOUNTTYPE";
    private final String IT_RES_KEY = "UD_LDAP_USR_SERVER";
    private final String UUID = "UD_LDAP_USR_NSUNIQUEID";

    private final Logger log = Logger.getLogger("com.icsynergy");

    private String UNIQUEID;

    public String setObjectClasses(long lProcInstKey, String strLookupName) {
        log.entering(getClass().getName(), "setObjectClasses", String.valueOf(lProcInstKey) + " " + strLookupName);

        String strRet = "FAILURE";

        try {
            log.finest("getting process form data...");
            tcResultSet formData = OIMUtility.getProcessFormData(lProcInstKey);

            final String strAcccountType = formData.getStringValue(ACNT_TYPE);
            log.finer("strAccountType=" + strAcccountType);

            log.finest("getting lookup operations interface...");
            final tcLookupOperationsIntf ifLookupOps = Platform.getService(tcLookupOperationsIntf.class);

            // object classes as per design requirements
            log.finest("getting object classes from the lookup");
            final String strObjClasses = ifLookupOps.getDecodedValueForEncodedValue(strLookupName, strAcccountType);
            log.finer("strObjClasses=" + strObjClasses);

            final String strItResKey = formData.getStringValue(IT_RES_KEY);
            log.finer("strItResKey=" + strItResKey);

            log.finest("getting configuration lookup from IT res");
            final String strConfigLookup = getParamForItRes(strItResKey, CONFIGURATION_LOOKUP);
            log.finer("strConfigLookup=" + strConfigLookup);

            UNIQUEID = ifLookupOps.getDecodedValueForEncodedValue(strConfigLookup, UID_ATTRIBUTE);
            log.finer("UNIQUEID=" + UNIQUEID);

            // account object classes from the configuration lookup
            // "xxx", "yyy", "zzz"
            final String strAccountObjectClasses =
                    ifLookupOps.getDecodedValueForEncodedValue(strConfigLookup, ACCOUNT_OBJECT_CLASSES);
            log.finer("accountObjectClasses=" + strAccountObjectClasses);

            // calculating object classes to add
            Set<String> newSet = new HashSet<>(Arrays.asList(strObjClasses.split(",")));
            Set<String> oldSet = new HashSet<>(Arrays.asList(strAccountObjectClasses.split(",")));
            log.finer("old set: " + oldSet);
            log.finer("new set: " + newSet);

            Set<String> setToAdd = new HashSet<>(newSet);
            setToAdd.removeAll(oldSet);
            log.finer("setToAdd: " + setToAdd);

            final String strUUID = formData.getStringValue(UUID);
            log.finer("strUUID=" + strUUID);

            log.finest("getting connection parameters from IT resource...");
            final Map<String, String> mapConnParams = getConnectionParams(strItResKey);

            log.finest("getting dir context...");
            DirContext ctx = getLdapConnection(mapConnParams);

            log.finest("adding object classes...");
            addObjClasses(ctx, strUUID, setToAdd);

            strRet = "SUCCESS";
        } catch (Exception e) {
            log.log(Level.SEVERE, "Exception", e);
        }

        log.exiting(getClass().getName(), "setObjectClasses", strRet);
        return strRet;
    }

    private Map<String, String> getConnectionParams(String strItResKey)
            throws Exception {
        log.entering(getClass().getName(), "getConnectionParams", strItResKey);

        Map<String, String> mapRet = new HashMap<>();

        String strKey = HOST;
        mapRet.put(strKey, getParamForItRes(strItResKey, strKey));

        strKey = PORT;
        mapRet.put(strKey, getParamForItRes(strItResKey, strKey));

        strKey = PRINCIPAL;
        mapRet.put(strKey, getParamForItRes(strItResKey, strKey));

        strKey = CREDENTIALS;
        mapRet.put(strKey, getParamForItRes(strItResKey, strKey));

        strKey = "baseContexts";
        mapRet.put(strKey, getParamForItRes(strItResKey, strKey));

        strKey = SSL;
        mapRet.put(strKey, getParamForItRes(strItResKey, strKey));

        log.exiting(getClass().getName(), "getConnectionParams", mapRet);
        return mapRet;
    }

    private DirContext getLdapConnection(Map<String, String> mapConnParams) throws NamingException {
        log.entering(getClass().getName(), "getLdapConnection", mapConnParams);

        Hashtable<String, Object> env = new Hashtable<>();
        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        env.put(Context.SECURITY_PRINCIPAL, mapConnParams.get(PRINCIPAL));
        env.put(Context.SECURITY_CREDENTIALS, mapConnParams.get(CREDENTIALS));
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");

        if (mapConnParams.get(SSL).equalsIgnoreCase("yes")) {
            env.put(Context.PROVIDER_URL,
                    String.format("ldaps://%s:%s", mapConnParams.get(HOST), mapConnParams.get(PORT)));
        } else {
            env.put(Context.PROVIDER_URL,
                    String.format("ldap://%s:%s", mapConnParams.get(HOST), mapConnParams.get(PORT)));
        }

        DirContext ctx = new InitialDirContext(env);

        log.exiting(getClass().getName(), "getLdapConnection");
        return ctx;
    }

    private void addObjClasses(DirContext ctx, String strUUID, Set<String> setToAdd) throws NamingException {
        log.entering(getClass().getName(), "addObjClasses");

        log.finest("setting search controls...");
        SearchControls ctls = new SearchControls();
        ctls.setSearchScope(SearchControls.SUBTREE_SCOPE);

        log.finest("running search....");
        NamingEnumeration<SearchResult> searchResults = ctx.search("", String.format("%s=%s", UNIQUEID, strUUID), ctls);

        if (!searchResults.hasMore()) {
            log.severe("object not found in LDAP");
            throw new RuntimeException("Object not found in LDAP");
        }

        SearchResult searchResult = searchResults.next();
        String strDn = searchResult.getName();
        log.finer("strDn=" + strDn);

        List<ModificationItem> lstMods = new ArrayList<>(setToAdd.size());
        for (String strClass: setToAdd) {
            lstMods.add(
                    new ModificationItem(DirContext.ADD_ATTRIBUTE,
                    new BasicAttribute("objectclass", strClass.replaceAll("\"",""))));
        }
        log.finer("lstMods: " + lstMods);

        ModificationItem[] mods = lstMods.toArray(new ModificationItem[lstMods.size()]);

        log.finest("modifying entry....");
        ctx.modifyAttributes(strDn, mods);

        log.exiting(getClass().getName(), "addObjClasses");
    }

    /**
     * Gets the name of Configuration Lookup for IT resource instance
     * @param strItResKey IT Resource key
     * @param strParam Parameter to get from IT resource
     * @return Configuration Lookup for IT Resource
     * @throws tcITResourceNotFoundException
     * @throws tcAPIException
     * @throws tcColumnNotFoundException
     */
    private String getParamForItRes(String strItResKey, String strParam)
            throws tcITResourceNotFoundException, tcAPIException, tcColumnNotFoundException {
        log.entering(getClass().getName(), "getParamForItRes", strItResKey + " " + strParam);

        log.finest("getting ITResOps interface...");
        tcITResourceInstanceOperationsIntf ifITResOps = Platform.getService(tcITResourceInstanceOperationsIntf.class);

        tcResultSet rsItResParams = ifITResOps.getITResourceInstanceParameters(Long.parseLong(strItResKey));
        OIMUtility.printResultSet(log, rsItResParams);

        String strRet = rsItResParams.getStringValue(strParam);


        log.exiting(getClass().getName(), "getParamForItRes", strRet);
        return strRet;
    }
}
