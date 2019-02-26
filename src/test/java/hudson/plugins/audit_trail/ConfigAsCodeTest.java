package hudson.plugins.audit_trail;

import java.io.Console;
import java.util.List;
import io.jenkins.plugins.casc.ConfigurationAsCode;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import static org.junit.Assert.assertTrue;

public class ConfigAsCodeTest {

    @Rule
    public JenkinsRule r = new JenkinsRule();

    @Test public void should_support_configuration_as_code() throws Exception {
        ConfigurationAsCode.get().configure(ConfigAsCodeTest.class.getResource("configuration-as-code.yml").toString());
        List<AuditLogger> loggers = AuditTrailGlobalConfiguration.get().getLoggers();

        assertTrue(loggers.size() == 3);

        LogFileAuditLogger filelogger = (LogFileAuditLogger)loggers.get(0);
        assertTrue(filelogger.getCount() == 10);
        assertTrue(filelogger.getLimit() == 50);
        assertTrue(filelogger.getLog().equals("/var/log/jenkins/audit.log"));

        SyslogAuditLogger syslogger = (SyslogAuditLogger)loggers.get(1);
        assertTrue(syslogger.getMessageFormat().equals("RFC_3164"));
        assertTrue(syslogger.getSyslogServerHostname().equals("my.syslog.server"));
        assertTrue(syslogger.getSyslogServerPort() == 514);
        assertTrue(syslogger.getAppName().equals(SyslogAuditLogger.DEFAULT_APP_NAME));
        assertTrue(syslogger.getFacility().equals(SyslogAuditLogger.DEFAULT_FACILITY.name()));

        ConsoleAuditLogger consolelogger = (ConsoleAuditLogger)loggers.get(2);
        assertTrue(consolelogger.getDateFormat().equals("yyyy-MM-dd HH:mm:ss:SSS"));
        assertTrue(consolelogger.getLogPrefix().equals("my_log_prefix"));
        assertTrue(consolelogger.getOutput().equals(ConsoleAuditLogger.Output.STD_OUT));
    }
}