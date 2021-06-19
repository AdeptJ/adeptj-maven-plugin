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

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.FormRequestContent;
import org.eclipse.jetty.client.util.MultiPartRequestContent;
import org.eclipse.jetty.client.util.PathRequestContent;
import org.eclipse.jetty.client.util.StringRequestContent;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.util.Fields;
import org.eclipse.jetty.util.HttpCookieStore;

import java.io.File;
import java.io.IOException;
import java.util.jar.JarFile;

import static com.adeptj.maven.plugin.bundle.Constants.COOKIE_JSESSIONID;
import static com.adeptj.maven.plugin.bundle.Constants.DEFAULT_AUTH_URL;
import static com.adeptj.maven.plugin.bundle.Constants.DEFAULT_CONSOLE_URL;
import static com.adeptj.maven.plugin.bundle.Constants.DEFAULT_LOGOUT_URL;
import static com.adeptj.maven.plugin.bundle.Constants.J_PASSWORD;
import static com.adeptj.maven.plugin.bundle.Constants.J_USERNAME;
import static com.adeptj.maven.plugin.bundle.Constants.PARAM_ACTION;
import static com.adeptj.maven.plugin.bundle.Constants.PARAM_ACTION_INSTALL_VALUE;
import static com.adeptj.maven.plugin.bundle.Constants.PARAM_BUNDLE_FILE;
import static com.adeptj.maven.plugin.bundle.Constants.PARAM_PARALLEL_VERSION;
import static com.adeptj.maven.plugin.bundle.Constants.PARAM_REFRESH_PACKAGES;
import static com.adeptj.maven.plugin.bundle.Constants.PARAM_START;
import static com.adeptj.maven.plugin.bundle.Constants.PARAM_START_LEVEL;
import static com.adeptj.maven.plugin.bundle.Constants.URL_BUNDLE_INSTALL;
import static com.adeptj.maven.plugin.bundle.Constants.VALUE_FALSE;
import static com.adeptj.maven.plugin.bundle.Constants.VALUE_TRUE;

/**
 * Base for various bundle mojo implementations.
 *
 * @author Rakesh.Kumar, AdeptJ
 */
abstract class AbstractBundleMojo extends AbstractMojo {

    @Parameter(
            property = "adeptj.bundle.file",
            defaultValue = "${project.build.directory}/${project.build.finalName}.jar",
            required = true
    )
    String bundleFileName;

    @Parameter(property = "adeptj.failOnError", defaultValue = VALUE_TRUE, required = true)
    boolean failOnError;

    @Parameter(property = "adeptj.bundle.startlevel", defaultValue = "20", required = true)
    String startLevel;

    @Parameter(property = "adeptj.bundle.start", defaultValue = VALUE_TRUE, required = true)
    boolean startBundle;

    @Parameter(property = "adeptj.bundle.refreshPackages", defaultValue = VALUE_TRUE, required = true)
    boolean refreshPackages;

    @Parameter(property = "adeptj.bundle.parallelVersion", defaultValue = VALUE_FALSE)
    boolean parallelVersion;

    @Parameter(property = "adeptj.console.url", defaultValue = DEFAULT_CONSOLE_URL, required = true)
    String consoleUrl;

    @Parameter(property = "adeptj.auth.url", defaultValue = DEFAULT_AUTH_URL, required = true)
    private String authUrl;

    @Parameter(property = "adeptj.logout.url", defaultValue = DEFAULT_LOGOUT_URL)
    private String logoutUrl;

    @Parameter(property = "adeptj.user", defaultValue = "admin", required = true)
    private String user;

    @Parameter(property = "adeptj.password", defaultValue = "admin", required = true)
    private String password;

    private boolean loginSucceeded;

    protected final HttpClient httpClient;

    public AbstractBundleMojo() {
        this.httpClient = new HttpClient();
        this.httpClient.setCookieStore(new HttpCookieStore());
        try {
            this.httpClient.start();
            this.getLog().info("Using Jetty HttpClient!!");
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    boolean login() {
        try {
            Request request = this.httpClient.newRequest(this.authUrl).method(HttpMethod.POST);
            Fields fields = new Fields();
            fields.put(J_USERNAME, this.user);
            fields.put(J_PASSWORD, this.password);
            request.body(new FormRequestContent(fields));
            ContentResponse response = request.send();
            if (response.getStatus() == HttpStatus.OK_200) {
                this.loginSucceeded = this.httpClient.getCookieStore()
                        .getCookies()
                        .stream()
                        .anyMatch(cookie -> StringUtils.startsWith(cookie.getName(), COOKIE_JSESSIONID));
                if (this.loginSucceeded) {
                    this.getLog().info("Login Successful!!");
                }
            }
        } catch (Exception ex) {
            throw new BundleMojoException(ex);
        }
        return this.loginSucceeded;
    }

    void logout() {
        if (this.loginSucceeded) {
            this.getLog().info("Invoking Logout!!");
            try {
                Request request = this.httpClient.newRequest(this.logoutUrl).method(HttpMethod.GET);
                ContentResponse response = request.send();
                this.getLog().info("Logout status code: " + response.getStatus());
                this.getLog().info("Logout successful!!");
                this.httpClient.getCookieStore().removeAll();
            } catch (Exception ex) {
                this.getLog().error(ex);
            }
        }
    }

    void installBundle(File bundle) {
        try {
            Request request = this.httpClient.newRequest(String.format(URL_BUNDLE_INSTALL, this.consoleUrl))
                    .method(HttpMethod.POST);
            MultiPartRequestContent content = new MultiPartRequestContent();
            content.addFieldPart(PARAM_ACTION, new StringRequestContent(PARAM_ACTION_INSTALL_VALUE), null);
            content.addFieldPart(PARAM_START_LEVEL, new StringRequestContent(this.startLevel), null);
            content.addFilePart(PARAM_BUNDLE_FILE, bundle.getName(), new PathRequestContent(bundle.toPath()), null);
            if (this.startBundle) {
                content.addFieldPart(PARAM_START, new StringRequestContent(VALUE_TRUE), null);
            }
            if (this.refreshPackages) {
                content.addFieldPart(PARAM_REFRESH_PACKAGES, new StringRequestContent(VALUE_TRUE), null);
            }
            // Since web console v4.4.0
            if (this.parallelVersion) {
                content.addFieldPart(PARAM_PARALLEL_VERSION, new StringRequestContent(VALUE_TRUE), null);
            }
            // MultiPartRequestContent must be closed before sending request.
            content.close();
            ContentResponse response = request.body(content).send();
            if (response.getStatus() == HttpStatus.OK_200) {
                this.getLog().info("Bundle installed successfully, please check AdeptJ OSGi Web Console"
                        + " [" + this.consoleUrl + "/bundles" + "]");
                return;
            }
            if (this.failOnError) {
                throw new MojoExecutionException(
                        String.format("Bundle installation failed, reason: [%s], status: [%s]",
                                response.getReason(),
                                response.getStatus()));
            }
            this.getLog().warn("Problem installing bundle, please check AdeptJ OSGi Web Console!!");
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    void closeHttpClient() {
        try {
            this.httpClient.getCookieStore().removeAll();
            this.httpClient.stop();
            this.getLog().debug("HttpClient closed!!");
        } catch (Exception ex) {
            this.getLog().error(ex);
        }
    }

    void logBundleInfo(BundleInfo info, BundleMojoOp op) {
        switch (op) {
            case INSTALL:
                this.getLog().info("Installing " + info);
                break;
            case UNINSTALL:
                this.getLog().info("Uninstalling " + info);
                break;
        }
    }

    BundleInfo getBundleInfo(File bundle) throws IOException {
        try (JarFile bundleArchive = new JarFile(bundle)) {
            return new BundleInfo(bundleArchive.getManifest().getMainAttributes());
        }
    }
}
