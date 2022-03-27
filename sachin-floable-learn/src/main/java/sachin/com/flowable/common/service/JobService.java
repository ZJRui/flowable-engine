package sachin.com.flowable.common.service;

public class JobService {

    protected JobServiceConfiguration jobServiceConfiguration;


    /**
     * 演示 CommandExecutor的使用方式
     * @param unlock
     */
    public void executeJob(final boolean unlock){
        jobServiceConfiguration.getCommandExecutor().execute(new JobCommand());

    }
}
