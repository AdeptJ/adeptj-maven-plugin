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

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.IOException;
import java.util.jar.Attributes;
import java.util.jar.JarFile;

import static com.adeptj.maven.plugin.bundle.BundleUninstallMojo.MOJO_NAME;
import static com.adeptj.maven.plugin.bundle.Constants.BUNDLE_NAME;
import static com.adeptj.maven.plugin.bundle.Constants.BUNDLE_SYMBOLICNAME;
import static com.adeptj.maven.plugin.bundle.Constants.BUNDLE_VERSION;
import static com.adeptj.maven.plugin.bundle.Constants.URL_UNINSTALL;
import static org.apache.http.HttpStatus.SC_OK;

/**
 * Uninstall an OSGi bundle from a running AdeptJ Runtime instance.
 *
 * @author Rakesh.Kumar, AdeptJ
 */
@Mojo(name = MOJO_NAME)
public class BundleUninstallMojo extends AbstractBundleMojo {

    static final String MOJO_NAME = "uninstall";

    @Parameter(property = "adeptj.file", defaultValue = "${project.build.directory}/${project.build.finalName}.jar")
    private String bundleFileName;


    @Override
    public void execute() throws MojoExecutionException {
        Log log = getLog();
        File bundle = new File(this.bundleFileName);
        String bsn = this.getBsn(log, bundle);
        try (CloseableHttpClient httpClient = this.getHttpClient()) {
            // First authenticate, then while installing bundle, HttpClient will pass the JSESSIONID received
            // in the Set-Cookie header in the auth call. if authentication fails, discontinue the further execution.
            if (this.authenticate()) {
                try (CloseableHttpResponse uninstallResponse = httpClient.execute(RequestBuilder
                        .post(this.adeptjConsoleURL + String.format(URL_UNINSTALL, bsn))
                        .addParameter("action", "uninstall")
                        .build())) {
                    int status = uninstallResponse.getStatusLine().getStatusCode();
                    if (status == SC_OK) {
                        log.info("Bundle uninstalled successfully, please check AdeptJ OSGi Web Console"
                                + " [" + this.adeptjConsoleURL + "]");
                    } else {
                        if (this.failOnError) {
                            throw new MojoExecutionException("Bundle Uninstall failed, cause: " + status);
                        }
                        log.warn("Seems a problem while uninstalling bundle, please check AdeptJ OSGi Web Console!!");
                    }
                }
            } else {
                // means authentication was failed.
                if (this.failOnError) {
                    throw new MojoExecutionException("[Authentication failed, please check credentials!!]");
                }
                log.error("Authentication failed, please check credentials!!");
            }
        } catch (Exception ex) {
            throw new MojoExecutionException("Uninstall on [" + this.adeptjConsoleURL + "] failed, cause: " + ex.getMessage(), ex);
        }
    }

    private String getBsn(Log log, File bundle) {
        String bsn = null;
        try (JarFile jarFile = new JarFile(bundle)) {
            Attributes mainAttributes = jarFile.getManifest().getMainAttributes();
            String bundleName = mainAttributes.getValue(BUNDLE_NAME);
            if (bundleName == null || bundleName.isEmpty()) {
                throw new IllegalStateException("Artifact is not a Bundle!!");
            }
            bsn = mainAttributes.getValue(BUNDLE_SYMBOLICNAME);
            String bundleVersion = mainAttributes.getValue(BUNDLE_VERSION);
            log.info("Uninstalling Bundle [" + bundleName + " (" + bsn + "), version: " + bundleVersion + "]");
        } catch (IOException ex) {
            log.error(ex);
        }
        return bsn;
    }

}