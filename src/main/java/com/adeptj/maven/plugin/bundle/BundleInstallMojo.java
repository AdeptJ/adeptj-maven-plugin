/*
###############################################################################
#                                                                             #
#    Copyright 2016-2024, AdeptJ (http://www.adeptj.com)                      #
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

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;

import java.io.IOException;
import java.net.URI;

import static com.adeptj.maven.plugin.bundle.BundleInstallMojo.MOJO_NAME;
import static com.adeptj.maven.plugin.bundle.Constants.PARAM_ACTION;
import static com.adeptj.maven.plugin.bundle.Constants.PARAM_ACTION_INSTALL_VALUE;
import static com.adeptj.maven.plugin.bundle.Constants.PARAM_BUNDLE_FILE;
import static com.adeptj.maven.plugin.bundle.Constants.PARAM_PARALLEL_VERSION;
import static com.adeptj.maven.plugin.bundle.Constants.PARAM_REFRESH_PACKAGES;
import static com.adeptj.maven.plugin.bundle.Constants.PARAM_START;
import static com.adeptj.maven.plugin.bundle.Constants.PARAM_START_LEVEL;
import static com.adeptj.maven.plugin.bundle.Constants.URL_BUNDLE_INSTALL;
import static com.adeptj.maven.plugin.bundle.Constants.VALUE_TRUE;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.maven.plugins.annotations.LifecyclePhase.INSTALL;

/**
 * Mojo for installing the OSGi Bundle to running AdeptJ Runtime instance.
 *
 * @author Rakesh.Kumar, AdeptJ
 */
@Mojo(name = MOJO_NAME, defaultPhase = INSTALL)
class BundleInstallMojo extends AbstractBundleMojo {

    static final String MOJO_NAME = "install";

    @Override
    void doExecute(BundleInfo info) throws IOException, MojoExecutionException {
        this.getLog().info("Installing " + info);
        URI uri = this.getFullUri(String.format(URL_BUNDLE_INSTALL, this.consoleUrl));
        HttpPost request = new HttpPost(uri);
        request.setEntity(this.newBundleInstallMultipartEntity(info));
        ClientResponse response = this.httpClient.execute(request, this.responseHandler);
        if (response.isOk()) {
            this.getLog().info("Bundle installed successfully, please check AdeptJ OSGi Web Console"
                    + " [" + this.consoleUrl + "/bundles" + "]");
            return;
        }
        if (this.failOnError) {
            throw new MojoExecutionException(
                    String.format("Couldn't install bundle, reason: [%s], status: [%s]",
                            response.getReasonPhrase(),
                            response.getCode()));
        }
        this.getLog().error("Problem installing bundle, please check AdeptJ OSGi Web Console!!");
    }

    private HttpEntity newBundleInstallMultipartEntity(BundleInfo info) {
        MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder.create()
                .setCharset(UTF_8)
                .addBinaryBody(PARAM_BUNDLE_FILE, info.getBundle())
                .addTextBody(PARAM_ACTION, PARAM_ACTION_INSTALL_VALUE)
                .addTextBody(PARAM_START_LEVEL, startLevel);
        if (this.startBundle) {
            multipartEntityBuilder.addTextBody(PARAM_START, VALUE_TRUE);
        }
        if (this.refreshPackages) {
            multipartEntityBuilder.addTextBody(PARAM_REFRESH_PACKAGES, VALUE_TRUE);
        }
        // Since web console v4.4.0
        if (this.parallelVersion) {
            multipartEntityBuilder.addTextBody(PARAM_PARALLEL_VERSION, VALUE_TRUE);
        }
        return multipartEntityBuilder.build();
    }

    @Override
    void handleException(Exception ex) throws MojoExecutionException {
        BundleMojoUtil.doHandleException(this.getLog(), ex, "install", this.consoleUrl);
    }
}
