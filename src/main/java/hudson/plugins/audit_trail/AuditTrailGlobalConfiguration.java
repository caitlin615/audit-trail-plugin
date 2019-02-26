package hudson.plugins.audit_trail;

import hudson.Extension;
import jenkins.model.GlobalConfiguration;
import net.sf.json.JSONObject;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.StaplerRequest;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

@Extension()
@Symbol("audit-trail")
public class AuditTrailGlobalConfiguration extends GlobalConfiguration {
    private static final Logger LOGGER = Logger.getLogger(AuditTrailGlobalConfiguration.class.getName());
    private List<AuditLogger> loggers = new ArrayList<AuditLogger>();

    @DataBoundConstructor
    public AuditTrailGlobalConfiguration() {
        this.load();
    }

    public static AuditTrailGlobalConfiguration get() {
        return GlobalConfiguration.all().get(AuditTrailGlobalConfiguration.class);
    }

    public List<AuditLogger> getLoggers() { return this.loggers; }


    @DataBoundSetter
    public void setLoggers(List<AuditLogger> loggers) {
        this.loggers = loggers;
    }

    @Override
    @Nonnull
    public String getDisplayName() {
        return "Audit Trail";
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject json) {
        req.bindJSON(this, json);
        this.save();
        return true;
    }
}
