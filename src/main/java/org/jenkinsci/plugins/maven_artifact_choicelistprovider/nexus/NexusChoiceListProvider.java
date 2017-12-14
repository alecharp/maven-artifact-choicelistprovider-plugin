package org.jenkinsci.plugins.maven_artifact_choicelistprovider.nexus;

import java.util.Collections;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.maven_artifact_choicelistprovider.AbstractMavenArtifactChoiceListProvider;
import org.jenkinsci.plugins.maven_artifact_choicelistprovider.AbstractMavenArtifactDescriptorImpl;
import org.jenkinsci.plugins.maven_artifact_choicelistprovider.IVersionReader;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;

import hudson.Extension;
import hudson.model.Item;
import hudson.security.ACL;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import net.sf.json.JSONObject;

public class NexusChoiceListProvider extends AbstractMavenArtifactChoiceListProvider {

    private static final long serialVersionUID = -5192115026547049358L;

    private String url;
    private String credentialsId;

    @DataBoundConstructor
    public NexusChoiceListProvider(String artifactId) {
        super(artifactId);
    }

    @Extension
    public static class NexusDescriptorImpl extends AbstractMavenArtifactDescriptorImpl {

        /** For Global Options */
        private boolean useRestfulAPI;

        /**
         * the display name shown in the dropdown to select a choice provider.
         * 
         * @return display name
         * @see hudson.model.Descriptor#getDisplayName()
         */
        @Override
        public String getDisplayName() {
            return "Nexus Artifact Choice Parameter";
        }

        public ListBoxModel doFillCredentialsIdItems(@AncestorInPath Item instance) {
            return new StandardListBoxModel().includeEmptyValue().includeMatchingAs(ACL.SYSTEM, instance, StandardUsernamePasswordCredentials.class,
                    Collections.<DomainRequirement> emptyList(), CredentialsMatchers.instanceOf(StandardUsernamePasswordCredentials.class));
        }

        public FormValidation doTest(@QueryParameter String url, @QueryParameter String credentialsId, @QueryParameter String repositoryId, @QueryParameter String groupId, @QueryParameter String artifactId,
                @QueryParameter String packaging, @QueryParameter String classifier, @QueryParameter boolean reverseOrder) {
            final IVersionReader service = new NexusLuceneSearchService(url);

            // If configured, set User Credentials
            final UsernamePasswordCredentialsImpl c = getCredentials(credentialsId);
            if (c != null) {
                service.setCredentials(c.getUsername(), c.getPassword().getPlainText());
            }
            return super.performTest(service, repositoryId, groupId, artifactId, packaging, classifier, reverseOrder);
        }

        @Override
        protected Map<String, String> wrapTestConnection(IVersionReader pService, String pRepositoryId, String pGroupId, String pArtifactId, String pPackaging, String pClassifier,
                boolean pReverseOrder) {
            return readURL(pService, pRepositoryId, pGroupId, pArtifactId, pPackaging, pClassifier, pReverseOrder);
        }

        public FormValidation doCheckUrl(@QueryParameter String url) {
            if (StringUtils.isBlank(url)) {
                return FormValidation.error("The nexus URL cannot be empty");
            }

            return FormValidation.ok();
        }

        /**
         * Saves the Global-Option Settings
         */
        @Override
        public boolean configure(StaplerRequest staplerRequest, JSONObject json) throws FormException {
            useRestfulAPI = json.getBoolean("useRestfulAPI");
            save();
            return super.configure(staplerRequest, json);
        }

        /**
         * Returns the value for checkbox in the "Manage Jenkins" section.
         * 
         * @return
         */
        public boolean getUseRestfulAPI() {
            return useRestfulAPI;
        }
    }

    @Override
    public IVersionReader createServiceInstance() {
        // this comes from the global settings
        boolean useRestfulAPI = ((NexusDescriptorImpl) getDescriptor()).getUseRestfulAPI();
        
        // init the service
        final IVersionReader retVal = new NexusLuceneSearchService(url, useRestfulAPI);
        final UsernamePasswordCredentialsImpl c = getCredentials(getCredentialsId());
        if (c != null) {
            retVal.setCredentials(c.getUsername(), c.getPassword().getPlainText());
        }
        return retVal;
    }

    @DataBoundSetter
    public void setCredentialsId(String credentialsId) {
        this.credentialsId = credentialsId;
    }

    public String getCredentialsId() {
        return credentialsId;
    }

    @DataBoundSetter
    public void setUrl(String url) {
        this.url = StringUtils.trim(url);
    }

    public String getUrl() {
        return url;
    }

}
