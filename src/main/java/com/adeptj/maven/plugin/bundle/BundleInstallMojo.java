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

package com.adeptj.maven.plugin.bundle;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.IOException;
import java.util.jar.Attributes;
import java.util.jar.JarFile;

import static com.adeptj.maven.plugin.bundle.BundleInstallMojo.MOJO_NAME;
import static com.adeptj.maven.plugin.bundle.Constants.BUNDLE_NAME;
import static com.adeptj.maven.plugin.bundle.Constants.BUNDLE_SYMBOLICNAME;
import static com.adeptj.maven.plugin.bundle.Constants.BUNDLE_VERSION;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.maven.plugins.annotations.LifecyclePhase.INSTALL;

/**
 * Mojo for installing the OSGi Bundle to running AdeptJ Runtime instance.
 *
 * @author Rakesh.Kumar, AdeptJ
 */
@Mojo(name = MOJO_NAME, defaultPhase = INSTALL)
public class BundleInstallMojo extends AbstractBundleMojo {

    static final String MOJO_NAME = "install";

    private static final String DEFAULT_CONSOLE_URL = "http://localhost:9007/system/console";

    private static final String URL_INSTALL = "/install";

    private static final String PARAM_STARTLEVEL = "bundlestartlevel";

    private static final String PARAM_START = "bundlestart";

    private static final String PARAM_BUNDLEFILE = "bundlefile";

    private static final String PARAM_REFRESH_PACKAGES = "refreshPackages";

    private static final String PARAM_ACTION = "action";

    private static final String PARAM_ACTION_VALUE = "install";

    private static final String VALUE_TRUE = "true";

    @Parameter(property = "adeptj.file", defaultValue = "${project.build.directory}/${project.build.finalName}.jar", required = true)
    private String bundleFileName;

    @Parameter(property = "adeptj.console.url", defaultValue = DEFAULT_CONSOLE_URL, required = true)
    private String adeptjConsoleURL;

    @Parameter(property = "adeptj.failOnError", defaultValue = VALUE_TRUE, required = true)
    private boolean failOnError;

    @Parameter(property = "adeptj.bundle.startlevel", defaultValue = "20", required = true)
    private String bundleStartLevel;

    @Parameter(property = "adeptj.bundle.start", defaultValue = VALUE_TRUE, required = true)
    private boolean bundleStart;

    @Parameter(property = "adeptj.refreshPackages", defaultValue = VALUE_TRUE, required = true)
    private boolean refreshPackages;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Log log = getLog();
        File bundle = new File(this.bundleFileName);
        this.logBundleInfo(log, bundle);
        try (CloseableHttpClient httpClient = this.getHttpClient()) {
            // First authenticate, then while installing bundle, HttpClient will pass the JSESSIONID received
            // in the Set-Cookie header in the auth call. if authentication fails, discontinue the further execution.
            if (this.authenticate()) {
                CloseableHttpResponse installResponse = httpClient.execute(RequestBuilder
                        .post(this.adeptjConsoleURL + URL_INSTALL)
                        .setEntity(this.multipartEntity(bundle))
                        .build());
                int status = installResponse.getStatusLine().getStatusCode();
                if (status == SC_OK) {
                    log.info("Bundle installed successfully, please check AdeptJ OSGi Web Console"
                            + " [" + this.adeptjConsoleURL + "]");
                } else {
                    if (this.failOnError) {
                        throw new MojoExecutionException("Installation failed, cause: " + status);
                    }
                    log.warn("Seems a problem while installing bundle, please check AdeptJ OSGi Web Console!!");
                }
                IOUtils.closeQuietly(installResponse);
            } else {
                // means authentication was failed.
                if (this.failOnError) {
                    throw new MojoExecutionException("[Authentication failed, please check credentials!!]");
                }
                log.error("Authentication failed, please check credentials!!");
            }
        } catch (Exception ex) {
            throw new MojoExecutionException("Installation on [" + this.adeptjConsoleURL + "] failed, cause: "
                    + ex.getMessage(), ex);
        }
    }

    private HttpEntity multipartEntity(File bundle) {
        MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder.create();
        multipartEntityBuilder.addBinaryBody(PARAM_BUNDLEFILE, bundle);
        multipartEntityBuilder.addTextBody(PARAM_ACTION, PARAM_ACTION_VALUE);
        multipartEntityBuilder.addTextBody(PARAM_STARTLEVEL, this.bundleStartLevel);
        if (this.bundleStart) {
            multipartEntityBuilder.addTextBody(PARAM_REFRESH_PACKAGES, VALUE_TRUE);
        }
        if (this.refreshPackages) {
            multipartEntityBuilder.addTextBody(PARAM_START, VALUE_TRUE);
        }
        return multipartEntityBuilder.build();
    }

    private void logBundleInfo(Log log, File bundle) {
        try (JarFile jarFile = new JarFile(bundle)) {
            Attributes mainAttributes = jarFile.getManifest().getMainAttributes();
            String bundleName = mainAttributes.getValue(BUNDLE_NAME);
            if (bundleName == null || bundleName.isEmpty()) {
                throw new IllegalStateException("Artifact is not a Bundle!!");
            }
            String bsn = mainAttributes.getValue(BUNDLE_SYMBOLICNAME);
            String bundleVersion = mainAttributes.getValue(BUNDLE_VERSION);
            log.info("Installing Bundle [" + bundleName + " (" + bsn + "), version: " + bundleVersion + "]");
        } catch (IOException ex) {
            log.error(ex);
        }
    }
}
