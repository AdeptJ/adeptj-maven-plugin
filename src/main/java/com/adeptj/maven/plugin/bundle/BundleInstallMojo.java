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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static com.adeptj.maven.plugin.bundle.BundleInstallMojo.MOJO_NAME;
import static com.adeptj.maven.plugin.bundle.Constants.PARAM_ACTION;
import static com.adeptj.maven.plugin.bundle.Constants.PARAM_ACTION_INSTALL_VALUE;
import static com.adeptj.maven.plugin.bundle.Constants.PARAM_BUNDLE_FILE;
import static com.adeptj.maven.plugin.bundle.Constants.PARAM_PARALLEL_VERSION;
import static com.adeptj.maven.plugin.bundle.Constants.PARAM_REFRESH_PACKAGES;
import static com.adeptj.maven.plugin.bundle.Constants.PARAM_START;
import static com.adeptj.maven.plugin.bundle.Constants.PARAM_START_LEVEL;
import static com.adeptj.maven.plugin.bundle.Constants.URL_INSTALL;
import static com.adeptj.maven.plugin.bundle.Constants.VALUE_FALSE;
import static com.adeptj.maven.plugin.bundle.Constants.VALUE_TRUE;
import static org.apache.maven.plugins.annotations.LifecyclePhase.INSTALL;

/**
 * Mojo for installing the OSGi Bundle to running AdeptJ Runtime instance.
 *
 * @author Rakesh.Kumar, AdeptJ
 */
@Mojo(name = MOJO_NAME, defaultPhase = INSTALL)
public class BundleInstallMojo extends AbstractBundleMojo {

    static final String MOJO_NAME = "install";

    @Parameter(
            property = "adeptj.bundle.file",
            defaultValue = "${project.build.directory}/${project.build.finalName}.jar",
            required = true
    )
    private String bundleFileName;

    @Parameter(property = "adeptj.bundle.startlevel", defaultValue = "20", required = true)
    private String bundleStartLevel;

    @Parameter(property = "adeptj.bundle.start", defaultValue = VALUE_TRUE, required = true)
    private boolean bundleStart;

    @Parameter(property = "adeptj.bundle.refreshPackages", defaultValue = VALUE_TRUE, required = true)
    private boolean refreshPackages;

    @Parameter(property = "adeptj.bundle.parallelVersion", defaultValue = VALUE_FALSE)
    private boolean parallelVersion;

    @Override
    public void execute() throws MojoExecutionException {
        Log log = this.getLog();
        File bundle = new File(this.bundleFileName);
        try {
            BundleDTO dto = this.getBundleDTO(bundle);
            this.logBundleDetails(dto, BundleMojoOp.INSTALL);
            // First login, then while installing bundle, HttpClient will pass the JSESSIONID received
            // in the Set-Cookie header in the auth call. if authentication fails, discontinue the further execution.
            if (this.login()) {
                Map<String, Object> data = new LinkedHashMap<>();
                data.put(PARAM_ACTION, PARAM_ACTION_INSTALL_VALUE);
                data.put(PARAM_START_LEVEL, bundleStartLevel);
                data.put(PARAM_BUNDLE_FILE, bundle.toPath());
                if (this.bundleStart) {
                    data.put(PARAM_START, VALUE_TRUE);
                }
                if (this.refreshPackages) {
                    data.put(PARAM_REFRESH_PACKAGES, VALUE_TRUE);
                }
                // Since web console v4.4.0
                if (this.parallelVersion) {
                    data.put(PARAM_PARALLEL_VERSION, VALUE_TRUE);
                }
                String boundary = UUID.randomUUID().toString();
                HttpRequest request = HttpRequest.newBuilder()
                        .header("Content-Type", "multipart/form-data;boundary=" + boundary)
                        .POST(BundleMojoUtil.ofMimeMultipartData(data, boundary))
                        .uri(URI.create(this.adeptjConsoleURL + URL_INSTALL))
                        .build();
                HttpResponse<String> response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                int status = response.statusCode();
                if (status == 200) {
                    log.info("Bundle installed successfully, please check AdeptJ OSGi Web Console"
                            + " [" + this.adeptjConsoleURL + "]");
                } else {
                    if (this.failOnError) {
                        throw new MojoExecutionException(
                                String.format("Bundle installation failed, reason: [%s], status: [%s]",
                                        response.statusCode(),
                                        status));
                    }
                    log.warn("Problem installing bundle, please check AdeptJ OSGi Web Console!!");
                }
            } else {
                // means authentication was failed.
                if (this.failOnError) {
                    throw new MojoExecutionException("[Authentication failed, please check credentials!!]");
                }
                log.error("Authentication failed, please check credentials!!");
            }
        } catch (Exception ex) {
            throw new MojoExecutionException("Installation on [" + this.adeptjConsoleURL + "] failed, cause: " + ex.getMessage(), ex);
        } finally {
            this.logout();
        }
    }
}
