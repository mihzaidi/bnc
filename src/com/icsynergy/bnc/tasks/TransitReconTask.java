package com.icsynergy.bnc.tasks;

import oracle.iam.platform.Platform;
import oracle.iam.scheduler.api.SchedulerService;
import oracle.iam.scheduler.vo.JobDetails;
import oracle.iam.scheduler.vo.JobParameter;
import oracle.iam.scheduler.vo.TaskSupport;

import java.util.HashMap;
import java.util.logging.Logger;

public class TransitReconTask extends TaskSupport {
    private final Logger log = Logger.getLogger("com.icsynergy");
    private final String PARAM_TOKEN = "Last Seen Token";


    @Override
    public void execute(HashMap hashMap) throws Exception {
        log.entering(getClass().getName(), "execute", hashMap.toString());

        final String strToken = hashMap.containsKey(PARAM_TOKEN) ? (String) hashMap.get(PARAM_TOKEN) : "";
        log.finer("token=" + strToken);

        if (strToken.equals("")) {
            SchedulerService ss = Platform.getService(SchedulerService.class);

            log.finest("looking up job details");
            JobDetails jobDetails = ss.getJobDetail(getName());

            log.finest("replacing job parameter");
            HashMap<String, JobParameter> map = jobDetails.getParams();
            JobParameter jobParam = map.get(PARAM_TOKEN);
            jobParam.setValue("right now");
            map.put(PARAM_TOKEN, jobParam);

            log.finest("updating job parameters");
            jobDetails.setParams(map);

            log.finest("updating job");
            ss.updateJob(jobDetails);
        }
        log.exiting(getClass().getName(), "execute");
    }

    @Override
    public HashMap getAttributes() {
        return null;
    }

    @Override
    public void setAttributes() {

    }
}
