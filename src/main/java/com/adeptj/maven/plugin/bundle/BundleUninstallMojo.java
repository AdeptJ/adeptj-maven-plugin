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
import java.util.HashMap;
import java.util.Map;

import static com.adeptj.maven.plugin.bundle.BundleMojoOp.UNINSTALL;
import static com.adeptj.maven.plugin.bundle.BundleUninstallMojo.MOJO_NAME;
import static com.adeptj.maven.plugin.bundle.Constants.PARAM_ACTION;
import static com.adeptj.maven.plugin.bundle.Constants.PARAM_ACTION_UNINSTALL_VALUE;
import static com.adeptj.maven.plugin.bundle.Constants.URL_UNINSTALL;

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
            BundleDTO dto = this.getBundleDTO(bundle);
            this.logBundleDetails(dto, UNINSTALL);
            // First login, then while installing bundle, HttpClient will pass the JSESSIONID received
            // in the Set-Cookie header in the auth call. if authentication fails, discontinue the further execution.
            if (this.login()) {
                Map<String, Object> data = new HashMap<>();
                data.put(PARAM_ACTION, PARAM_ACTION_UNINSTALL_VALUE);
                HttpRequest request = HttpRequest.newBuilder()
                        .POST(BundleMojoUtil.ofFormData(data))
                        .uri(URI.create(this.adeptjConsoleURL + String.format(URL_UNINSTALL, dto.getSymbolicName())))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .build();
                getLog().info(request.toString());
                HttpResponse<String> response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                int status = response.statusCode();
                if (status == 200) {
                    log.info("Bundle uninstalled successfully, please check AdeptJ OSGi Web Console"
                            + " [" + this.adeptjConsoleURL + "]");
                } else {
                    if (this.failOnError) {
                        throw new MojoExecutionException(
                                String.format("Couldn't uninstall bundle , reason: [%s], status: [%s]",
                                        status,
                                        status));
                    }
                    log.error("Problem uninstalling bundle, please check AdeptJ OSGi Web Console!!");
                }
            } else {
                // means authentication was failed.
                if (this.failOnError) {
                    throw new MojoExecutionException("[Authentication failed, please check credentials!!]");
                }
                log.error("Authentication failed, please check credentials!!");
            }
        } catch (Exception ex) {
            throw new MojoExecutionException("Bundle uninstall operation on [" + this.adeptjConsoleURL + "] failed, cause: "
                    + ex.getMessage(), ex);
        } finally {
            this.logout();
        }
    }
}