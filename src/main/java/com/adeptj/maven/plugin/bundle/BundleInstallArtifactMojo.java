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
import org.jboss.shrinkwrap.resolver.api.maven.Maven;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static com.adeptj.maven.plugin.bundle.BundleInstallArtifactMojo.MOJO_NAME;
import static com.adeptj.maven.plugin.bundle.BundleMojoOp.INSTALL;
import static com.adeptj.maven.plugin.bundle.Constants.SC_OK;
import static com.adeptj.maven.plugin.bundle.Constants.URL_INSTALL;
import static com.adeptj.maven.plugin.bundle.Constants.VALUE_FALSE;
import static com.adeptj.maven.plugin.bundle.Constants.VALUE_TRUE;

/**
 * Install an OSGi bundle from maven repository to a running AdeptJ Runtime instance.
 */
@Mojo(name = MOJO_NAME, requiresProject = false)
public class BundleInstallArtifactMojo extends AbstractBundleMojo {

    static final String MOJO_NAME = "install-artifact";

    private static final String GAV_FMT = "%s:%s:%s";

    @Parameter(property = "adeptj.artifact.groupId")
    private String groupId;

    @Parameter(property = "adeptj.artifact.artifactId")
    private String artifactId;

    @Parameter(property = "adeptj.artifact.version")
    private String version;

    @Parameter(property = "adeptj.bundle.startlevel", defaultValue = "20", required = true)
    private String startLevel;

    @Parameter(property = "adeptj.bundle.start", defaultValue = VALUE_TRUE, required = true)
    private boolean startBundle;

    @Parameter(property = "adeptj.bundle.refreshPackages", defaultValue = VALUE_TRUE, required = true)
    private boolean refreshPackages;

    @Parameter(property = "adeptj.bundle.parallelVersion", defaultValue = VALUE_FALSE)
    private boolean parallelVersion;

    @Override
    public void execute() throws MojoExecutionException {
        Log log = this.getLog();
        File bundle = Maven.resolver()
                .resolve(String.format(GAV_FMT, this.groupId, this.artifactId, this.version))
                .withoutTransitivity()
                .asSingleFile();
        try {
            BundleDTO dto = this.getBundleDTO(bundle);
            this.logBundleDetails(dto, INSTALL);
            // First login, then while installing bundle, HttpClient will pass the JSESSIONID received
            // in the Set-Cookie header in the auth call. if authentication fails, discontinue the further execution.
            if (this.login()) {
                HttpRequest request = BundleMojoUtil.bundleInstallRequest(bundle, URI.create(this.consoleUrl + URL_INSTALL),
                        this.startLevel,
                        this.startBundle, this.refreshPackages, this.parallelVersion);
                int status = this.httpClient.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
                if (status == SC_OK) {
                    log.info("Bundle installed successfully, please check AdeptJ OSGi Web Console"
                            + " [" + this.consoleUrl + "]");
                } else {
                    if (this.failOnError) {
                        throw new MojoExecutionException(String.format("Bundle installation failed, status: [%s]", status));
                    }
                    log.error("Problem installing bundle, please check AdeptJ OSGi Web Console!!");
                }
            } else {
                // means authentication was failed.
                if (this.failOnError) {
                    throw new MojoExecutionException("[Authentication failed, please check credentials!!]");
                }
                log.error("Authentication failed, please check credentials!!");
            }
        } catch (IOException | BundleMojoException ex) {
            this.getLog().error(ex);
            throw new MojoExecutionException("Installation on [" + this.consoleUrl + "] failed, cause: " + ex.getMessage(), ex);
        } catch (InterruptedException ex) {
            this.getLog().error(ex);
            if (!Thread.currentThread().isInterrupted()) {
                Thread.currentThread().interrupt();
            }
            throw new MojoExecutionException("Installation on [" + this.consoleUrl + "] failed, cause: " + ex.getMessage(), ex);
        } finally {
            this.logout();
        }
    }
}