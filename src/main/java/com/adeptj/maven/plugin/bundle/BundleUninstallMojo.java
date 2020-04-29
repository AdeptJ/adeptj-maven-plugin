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
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.HttpEntities;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static com.adeptj.maven.plugin.bundle.BundleMojoOp.UNINSTALL;
import static com.adeptj.maven.plugin.bundle.BundleUninstallMojo.MOJO_NAME;
import static com.adeptj.maven.plugin.bundle.Constants.PARAM_ACTION;
import static com.adeptj.maven.plugin.bundle.Constants.PARAM_ACTION_UNINSTALL_VALUE;
import static com.adeptj.maven.plugin.bundle.Constants.URL_UNINSTALL;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.hc.core5.http.HttpStatus.SC_OK;

/**
 * Mojo for uninstall an OSGi bundle from a running AdeptJ Runtime instance.
 *
 * @author Rakesh.Kumar, AdeptJ
 */
@Mojo(name = MOJO_NAME)
public class BundleUninstallMojo extends AbstractBundleMojo {

    static final String MOJO_NAME = "uninstall";

    @Parameter(
            property = "adeptj.bundle.file",
            defaultValue = "${project.build.directory}/${project.build.finalName}.jar"
    )
    private String bundleFileName;


    @Override
    public void execute() throws MojoExecutionException {
        Log log = this.getLog();
        File bundle = new File(this.bundleFileName);
        try {
            BundleInfo info = this.getBundleInfo(bundle);
            this.logBundleDetails(info, UNINSTALL);
            // First login, then while installing bundle, HttpClient will pass the JSESSIONID received
            // in the Set-Cookie header in the auth call. if authentication fails, discontinue the further execution.
            if (this.login()) {
                URI uri = URI.create(this.consoleUrl + String.format(URL_UNINSTALL, info.getSymbolicName()));
                HttpPost request = new HttpPost(uri);
                List<NameValuePair> parameters = new ArrayList<>();
                parameters.add(new BasicNameValuePair(PARAM_ACTION, PARAM_ACTION_UNINSTALL_VALUE));
                request.setEntity(HttpEntities.createUrlEncoded(parameters, UTF_8));
                try (CloseableHttpResponse response = this.httpClient.execute(request)) {
                    if (response.getCode() == SC_OK) {
                        EntityUtils.consume(response.getEntity());
                        log.info("Bundle uninstalled successfully, please check AdeptJ OSGi Web Console"
                                + " [" + this.consoleUrl + "]");
                    } else {
                        if (this.failOnError) {
                            throw new MojoExecutionException(
                                    String.format("Couldn't uninstall bundle , reason: [%s], status: [%s]",
                                            response.getReasonPhrase(),
                                            response.getCode()));
                        }
                        log.error("Problem uninstalling bundle, please check AdeptJ OSGi Web Console!!");
                    }
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
            throw new MojoExecutionException("Bundle uninstall operation on [" + this.consoleUrl + "] failed, cause: "
                    + ex.getMessage(), ex);
        } finally {
            this.logout();
            this.closeHttpClient();
        }
    }
}