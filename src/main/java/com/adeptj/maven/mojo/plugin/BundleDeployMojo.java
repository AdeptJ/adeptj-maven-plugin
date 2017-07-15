/*
###############################################################################
#                                                                             #
#    Copyright 2016, AdeptJ (http://www.adeptj.com)                           #
#                                                                             #
#    Licensed under the Apache License, Version 2.0 (the "License");          #
#    you may not use this file except in compliance with the License.         #
#    You may obtain a copy of the License at                                  #
#                                                                             #
#        http://www.apache.org/licenses/LICENSE-2.0                           #
#                                                                             #
#    Unless required by applicable law or agreed to in writing, software      #
#    distributed under the License is distributed on an "AS IS" BASIS,        #
#    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. #
#    See the License for the specific language governing permissions and      #
#    limitations under the License.                                           #
#                                                                             #
###############################################################################
*/

package com.adeptj.maven.mojo.plugin;

import org.apache.commons.io.IOUtils;
import org.apache.http.Consts;
import org.apache.http.Header;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarFile;

import static com.adeptj.maven.mojo.plugin.BundleDeployMojo.DEPLOY_MOJO;
import static org.apache.http.HttpStatus.SC_MOVED_TEMPORARILY;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.maven.plugins.annotations.LifecyclePhase.INSTALL;

/**
 * BundleDeployMojo, for deploying the OSGi Bundles to running AdeptJ Runtime instance.
 *
 * @author Rakesh.Kumar, AdeptJ
 */
@Mojo(name = DEPLOY_MOJO, defaultPhase = INSTALL)
public class BundleDeployMojo extends AbstractMojo {

    static final String DEPLOY_MOJO = "deploy";

    private static final String J_USERNAME = "j_username";

    private static final String J_PASSWORD = "j_password";

    private static final String HEADER_JSESSIONID = "JSESSIONID";

    private static final String AUTH_URL = "http://localhost:9007/auth/j_security_check";

    @Parameter(property = "adeptj.file", defaultValue = "${project.build.directory}/${project.build.finalName}.jar", required = true)
    private String bundleFileName;

    @Parameter(property = "adeptj.console.url", defaultValue = "http://localhost:9007/system/console", required = true)
    private String consoleUrl;

    @Parameter(property = "adeptj.user", defaultValue = "admin", required = true)
    private String user;

    @Parameter(property = "adeptj.password", defaultValue = "admin", required = true)
    private String password;

    @Parameter(property = "adeptj.failOnError", defaultValue = "true", required = true)
    protected boolean failOnError;

    @Parameter(property = "adeptj.mimeType", defaultValue = "application/java-archive", required = true)
    protected String mimeType;

    @Parameter(property = "adeptj.bundle.startlevel", defaultValue = "20", required = true)
    private String bundleStartLevel;

    @Parameter(property = "adeptj.bundle.start", defaultValue = "true", required = true)
    private boolean bundleStart;

    @Parameter(property = "adeptj.refreshPackages", defaultValue = "true", required = true)
    private boolean refreshPackages;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Log log = getLog();
        File bundle = new File(this.bundleFileName);
        try {
            Attributes mainAttributes = new JarFile(bundle).getManifest().getMainAttributes();
            String bundleName = mainAttributes.getValue("Bundle-Name");
            String bsn = mainAttributes.getValue("Bundle-SymbolicName");
            String bundleVersion = mainAttributes.getValue("Bundle-Version");
            log.info("Deploying Bundle => " + bundleName + "    (" + bsn + "), version: " + bundleVersion);
        } catch (IOException ex) {
            log.error(ex);
        }
        try (CloseableHttpClient httpClient = getHttpClient();) {
            // First Authenticate
            List<NameValuePair> authForm = new ArrayList<>();
            authForm.add(new BasicNameValuePair(J_USERNAME, this.user));
            authForm.add(new BasicNameValuePair(J_PASSWORD, this.password));
            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(authForm, Consts.UTF_8);
            HttpUriRequest authRequest = RequestBuilder
                    .post(AUTH_URL)
                    .setEntity(entity)
                    .build();
            CloseableHttpResponse authResponse = httpClient.execute(authRequest);
            String sessionId = this.getSessionId(authResponse);
            IOUtils.closeQuietly(authResponse);

            // Now deploy bundle and pass the JSESSIONID received as a header in Auth call above.
            HttpUriRequest bundleInstallRequest = RequestBuilder
                    .post(this.consoleUrl + "/bundles")
                    .addHeader(HEADER_JSESSIONID, sessionId)
                    .setEntity(MultipartEntityBuilder.create()
                            .setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
                            .addBinaryBody("bundlefile", bundle)
                            .addTextBody("action", "install")
                            .addTextBody("_noredir_", "_noredir_")
                            .addTextBody("bundlestartlevel", this.bundleStartLevel)
                            .addTextBody("refreshPackages", "true")
                            .addTextBody("bundlestart", "true")
                            .build())
                    .build();
            CloseableHttpResponse bundleInstallResponse = httpClient.execute(bundleInstallRequest);
            int statusCode = bundleInstallResponse.getStatusLine().getStatusCode();
            if (statusCode == SC_OK || statusCode == SC_MOVED_TEMPORARILY) {
                log.info("Bundle deployed successfully, please check AdeptJ OSGi Web Console!!");
            } else {
                log.info("Seems a problem while deploying bundle, please check AdeptJ OSGi Web Console!!");
            }
            IOUtils.closeQuietly(bundleInstallResponse);
        } catch (Exception ex) {
            throw new MojoExecutionException("Installation on " + this.consoleUrl + " failed, cause: " + ex.getMessage(), ex);
        }
    }

    private String getSessionId(CloseableHttpResponse authResponse) {
        for (Header header : authResponse.getAllHeaders()) {
            //log.info("Header: " + header.getValue());
            if (HEADER_JSESSIONID.equals(header.getName())) {
                return header.getValue();
            }
        }
        return null;
    }

    private CloseableHttpClient getHttpClient() {
        return HttpClients.createDefault();
    }
}
