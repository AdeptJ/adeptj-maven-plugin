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

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.io.entity.HttpEntities;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static com.adeptj.maven.plugin.bundle.BundleUninstallMojo.MOJO_NAME;
import static com.adeptj.maven.plugin.bundle.Constants.PARAM_ACTION;
import static com.adeptj.maven.plugin.bundle.Constants.PARAM_ACTION_UNINSTALL_VALUE;
import static com.adeptj.maven.plugin.bundle.Constants.URL_BUNDLE_UNINSTALL;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Mojo for uninstall an OSGi bundle from a running AdeptJ Runtime instance.
 *
 * @author Rakesh.Kumar, AdeptJ
 */
@Mojo(name = MOJO_NAME)
public class BundleUninstallMojo extends AbstractBundleMojo {

    static final String MOJO_NAME = "uninstall";

    @Override
    public void doExecute(File bundle, BundleInfo info) throws IOException, MojoExecutionException {
        this.getLog().info("Uninstalling " + info);
        URI uri = this.getUri(String.format(URL_BUNDLE_UNINSTALL, this.consoleUrl, info.getSymbolicName()));
        HttpPost request = new HttpPost(uri);
        List<NameValuePair> form = new ArrayList<>();
        form.add(new BasicNameValuePair(PARAM_ACTION, PARAM_ACTION_UNINSTALL_VALUE));
        request.setEntity(HttpEntities.createUrlEncoded(form, UTF_8));
        ClientResponse response = this.httpClient.execute(request, this.responseHandler);
        if (response.isOk()) {
            this.getLog().info("Bundle uninstalled successfully, please check AdeptJ OSGi Web Console"
                    + " [" + this.consoleUrl + "/bundles" + "]");
        } else {
            if (this.failOnError) {
                throw new MojoExecutionException(
                        String.format("Couldn't uninstall bundle, reason: [%s], status: [%s]",
                                response.getReasonPhrase(),
                                response.getCode()));
            }
            this.getLog().error("Problem uninstalling bundle, please check AdeptJ OSGi Web Console!!");
        }
    }

    @Override
    public void handleException(Exception ex) throws MojoExecutionException {
        this.getLog().error(ex);
        if (ex instanceof MojoExecutionException) {
            throw (MojoExecutionException) ex;
        }
        throw new MojoExecutionException("Bundle uninstall operation on [" + this.consoleUrl + "] failed, cause: "
                + ex.getMessage(), ex);
    }
}