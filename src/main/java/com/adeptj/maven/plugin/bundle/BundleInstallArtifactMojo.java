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
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;

import java.io.File;
import java.io.IOException;

import static com.adeptj.maven.plugin.bundle.BundleInstallArtifactMojo.MOJO_NAME;
import static com.adeptj.maven.plugin.bundle.BundleMojoOp.INSTALL;

/**
 * Install an OSGi bundle from maven repository to a running AdeptJ Runtime instance.
 */
@Mojo(name = MOJO_NAME, requiresProject = false)
public class BundleInstallArtifactMojo extends AbstractBundleMojo {

    static final String MOJO_NAME = "install-artifact";

    private static final String GAV_FMT = "%s:%s:%s";

    @Parameter(property = "adeptj.artifact.groupId", required = true)
    private String groupId;

    @Parameter(property = "adeptj.artifact.artifactId", required = true)
    private String artifactId;

    @Parameter(property = "adeptj.artifact.version", required = true)
    private String version;

    @Override
    public void execute() throws MojoExecutionException {
        String gavFmt = String.format(GAV_FMT, this.groupId, this.artifactId, this.version);
        File bundle = Maven.resolver()
                .resolve(gavFmt)
                .withoutTransitivity()
                .asSingleFile();
        if (bundle == null) {
            throw new MojoExecutionException(String.format("Maven artifact with GAV[%s] not found!", gavFmt));
        }
        try {
            this.logBundleInfo(this.getBundleInfo(bundle), INSTALL);
            // First login, then while installing bundle, HttpClient will pass the JSESSIONID received
            // in the Set-Cookie header in the auth call. if authentication fails, discontinue the further execution.
            if (this.login()) {
                this.installBundle(bundle);
                return;
            }
            // means authentication was failed.
            if (this.failOnError) {
                throw new MojoExecutionException("[Authentication failed, please check credentials!!]");
            }
            this.getLog().error("Authentication failed, please check credentials!!");
        } catch (IOException | BundleMojoException | IllegalArgumentException ex) {
            this.getLog().error(ex);
            throw new MojoExecutionException("Installation on [" + this.consoleUrl + "] failed, cause: " + ex.getMessage(), ex);
        } finally {
            this.logout();
            this.closeHttpClient();
        }
    }
}