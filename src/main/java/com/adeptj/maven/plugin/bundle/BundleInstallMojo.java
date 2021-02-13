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

import java.io.File;
import java.io.IOException;

import static com.adeptj.maven.plugin.bundle.BundleInstallMojo.MOJO_NAME;
import static org.apache.maven.plugins.annotations.LifecyclePhase.INSTALL;

/**
 * Mojo for installing the OSGi Bundle to running AdeptJ Runtime instance.
 *
 * @author Rakesh.Kumar, AdeptJ
 */
@Mojo(name = MOJO_NAME, defaultPhase = INSTALL)
public class BundleInstallMojo extends AbstractBundleMojo {

    static final String MOJO_NAME = "install";

    @Override
    public void execute() throws MojoExecutionException {
        File bundle = new File(this.bundleFileName);
        try {
            this.logBundleInfo(this.getBundleInfo(bundle), BundleMojoOp.INSTALL);
            // First login, then while installing bundle, HttpClient will pass the JSESSIONID received
            // in the Set-Cookie header in the auth call. if authentication fails, discontinue the further execution.
            if (this.login()) {
                this.installBundle(bundle);
            } else {
                // means authentication was failed.
                if (this.failOnError) {
                    throw new MojoExecutionException("[Authentication failed, please check credentials!!]");
                }
                this.getLog().error("Authentication failed, please check credentials!!");
            }
        } catch (IOException | BundleMojoException ex) {
            this.getLog().error(ex);
            throw new MojoExecutionException("Installation on [" + this.consoleUrl + "] failed, cause: " + ex.getMessage(), ex);
        } finally {
            this.logout();
            this.closeHttpClient();
        }
    }
}
