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
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.MultiPartRequestContent;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static com.adeptj.maven.plugin.bundle.BundleInstallMojo.MOJO_NAME;
import static com.adeptj.maven.plugin.bundle.Constants.URL_BUNDLE_INSTALL;
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
    public void doExecute(File bundle, BundleInfo info) throws IOException, MojoExecutionException {
        this.getLog().info("Installing " + info);
        URI uri = URI.create(String.format(URL_BUNDLE_INSTALL, this.consoleUrl));
        Request request = this.httpClient.newRequest(uri).method(HttpMethod.POST);
        MultiPartRequestContent content = BundleMojoUtil.newBundleInstallMultiPartRequestContent(bundle, this.startLevel,
                this.startBundle,
                this.refreshPackages,
                this.parallelVersion);
        try {
            ContentResponse response = request.body(content).send();
            int status = response.getStatus();
            if (HttpStatus.isSuccess(status)) {
                this.getLog().info("Bundle installed successfully, please check AdeptJ OSGi Web Console"
                        + " [" + this.consoleUrl + "/bundles" + "]");
                return;
            }
            if (this.failOnError) {
                throw new MojoExecutionException(
                        String.format("Bundle installation failed, reason: [%s], status: [%s]",
                                HttpStatus.getMessage(status), status));
            }
            this.getLog().error("Problem installing bundle, please check AdeptJ OSGi Web Console!!");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IOException(ex);
        } catch (TimeoutException | ExecutionException ex) {
            throw new IOException(ex);
        }
    }

    @Override
    public void handleException(Exception ex) throws MojoExecutionException {
        this.getLog().error(ex);
        if (ex instanceof MojoExecutionException) {
            throw (MojoExecutionException) ex;
        }
        throw new MojoExecutionException("Bundle install operation on [" + this.consoleUrl + "] failed, cause: "
                + ex.getMessage(), ex);
    }
}
