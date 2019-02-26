/*
 * The MIT License
 *
 * Copyright (c) 2004-2011, Sun Microsystems, Inc., Alan Harder
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package hudson.plugins.audit_trail;

import hudson.DescriptorExtensionList;
import hudson.Extension;
import hudson.Plugin;
import hudson.model.*;
import hudson.model.Descriptor.FormException;
import hudson.util.FormValidation;
import hudson.util.PluginServletFilter;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.DataBoundSetter;

import javax.annotation.Nonnull;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Keep audit trail of particular Jenkins operations, such as configuring jobs.
 * @author Alan Harder
 */
public class AuditTrailPlugin extends Plugin {
    private String pattern = ".*/(?:configSubmit|doDelete|postBuildResult|enable|disable|"
      + "cancelQueue|stop|toggleLogKeep|doWipeOutWorkspace|createItem|createView|toggleOffline|"
      + "cancelQuietDown|quietDown|restart|exit|safeExit)";
    private boolean logBuildCause = true;
    private transient boolean started;

    private transient String log;
    private transient int limit = 1, count = 1;

    @DataBoundConstructor
    public AuditTrailPlugin() { }

    private AuditTrailGlobalConfiguration config() {
        return AuditTrailGlobalConfiguration.get();
    }

    public List<AuditLogger> getLoggers() {
        return config().getLoggers();
    }

    public String getPattern() {
        return this.pattern;
    }

    @DataBoundSetter
    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public boolean getLogBuildCause() {
        return this.logBuildCause;
    }

    @DataBoundSetter
    public void setLogBuildCause(boolean logBuildCause) {
        this.logBuildCause = logBuildCause;
    }

    @Override
    public void start() throws Exception {
        // Set a default value; will be overridden by load() once customized:
        load();
        applySettings();

        // Add Filter to watch all requests and log matching ones
        PluginServletFilter.addFilter(new AuditTrailFilter(this));
    }

    private void applySettings() {
        try {
            AuditTrailFilter.setPattern(pattern);
        }
        catch (PatternSyntaxException ex) { ex.printStackTrace(); }
        started = true;
    }

    /* package */ void onStarted(Run run) {
        if (this.started) {
            StringBuilder buf = new StringBuilder(100);
            for (CauseAction action : run.getActions(CauseAction.class)) {
                for (Cause cause : action.getCauses()) {
                    if (buf.length() > 0) buf.append(", ");
                    buf.append(cause.getShortDescription());
                }
            }
            if (buf.length() == 0) buf.append("Started");

            for (AuditLogger logger : getLoggers()) {
                logger.configure();
                logger.log(run.getParent().getUrl() + " #" + run.getNumber() + ' ' + buf.toString());
            }

        }
    }

    public void onFinalized(Run run) {
        if (run instanceof AbstractBuild) {
            onFinalized((AbstractBuild) run);
        }

    }

    public void onFinalized(AbstractBuild build) {
        if (this.started) {
            StringBuilder causeBuilder = new StringBuilder(100);
            for (CauseAction action : build.getActions(CauseAction.class)) {
                for (Cause cause : action.getCauses()) {
                    if (causeBuilder.length() > 0) causeBuilder.append(", ");
                    causeBuilder.append(cause.getShortDescription());
                }
            }
            if (causeBuilder.length() == 0) causeBuilder.append("Started");

            for (AuditLogger logger : getLoggers()) {
                String message = build.getFullDisplayName() +
                        " " + causeBuilder.toString() +
                        " on node " + buildNodeName(build) +
                        " started at " + build.getTimestampString2() +
                        " completed in " + build.getDuration() + "ms" +
                        " completed: " + build.getResult();
                logger.log(message);
            }

        }
    }

    private String buildNodeName(AbstractBuild build) {
        Node node = build.getBuiltOn();
        if (node != null) {
            return node.getDisplayName();
        }

        return "#unknown#";
    }

    /* package */ void onRequest(String uri, String extra, String username) {
        if (this.started) {
            for (AuditLogger logger : getLoggers()) {
                logger.log(uri + extra + " by " + username);
            }
        }
    }


    /**
     * Backward compatibility
     */
    private Object readResolve() {
        if (log != null) {
            if (getLoggers() == null) {
                config().setLoggers(new ArrayList<AuditLogger>());
            }
            LogFileAuditLogger logger = new LogFileAuditLogger(log, limit, count);
            if (!getLoggers().contains(logger))
                getLoggers().add(logger);
            log = null;
        }
        return this;
    }

    /**
     * Validate regular expression syntax.
     */
    public FormValidation doRegexCheck(@QueryParameter final String value)
            throws IOException, ServletException {
        // No permission needed for simple syntax check
        try {
            Pattern.compile(value);
            return FormValidation.ok();
        }
        catch (Exception ex) {
            return FormValidation.errorWithMarkup("Invalid <a href=\""
                + "http://docs.oracle.com/javase/7/docs/api/java/util/regex/Pattern.html"
                + "\">regular expression</a> (" + ex.getMessage() + ")");
        }
    }

}
