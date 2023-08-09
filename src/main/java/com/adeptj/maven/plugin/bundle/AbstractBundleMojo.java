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
import org.eclipse.jetty.client.ContentResponse;
import org.eclipse.jetty.client.FormRequestContent;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.Request;
import org.eclipse.jetty.http.HttpCookieStore;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.util.Fields;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.jar.JarFile;

import static com.adeptj.maven.plugin.bundle.Constants.COOKIE_JSESSIONID;
import static com.adeptj.maven.plugin.bundle.Constants.DEFAULT_BASE_URL;
import static com.adeptj.maven.plugin.bundle.Constants.DEFAULT_CONSOLE_URL;
import static com.adeptj.maven.plugin.bundle.Constants.DEFAULT_LOGIN_URL;
import static com.adeptj.maven.plugin.bundle.Constants.DEFAULT_LOGOUT_URL;
import static com.adeptj.maven.plugin.bundle.Constants.J_PASSWORD;
import static com.adeptj.maven.plugin.bundle.Constants.J_USERNAME;
import static com.adeptj.maven.plugin.bundle.Constants.RT_ADAPTER_TOMCAT;
import static com.adeptj.maven.plugin.bundle.Constants.VALUE_FALSE;
import static com.adeptj.maven.plugin.bundle.Constants.VALUE_TRUE;
import static org.eclipse.jetty.http.HttpMethod.GET;
import static org.eclipse.jetty.http.HttpMethod.POST;

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

    @Parameter(property = "adeptj.base.url", defaultValue = DEFAULT_BASE_URL, required = true)
    String baseUrl;

    @Parameter(property = "adeptj.console.url", defaultValue = DEFAULT_CONSOLE_URL, required = true)
    String consoleUrl;

    @Parameter(property = "adeptj.login.url", defaultValue = DEFAULT_LOGIN_URL, required = true)
    private String loginUrl;

    @Parameter(property = "adeptj.logout.url", defaultValue = DEFAULT_LOGOUT_URL)
    private String logoutUrl;

    @Parameter(property = "adeptj.user", defaultValue = "admin", required = true)
    private String user;

    @Parameter(property = "adeptj.password", defaultValue = "admin", required = true)
    private String password;

    @Parameter(property = "adeptj.server.adapter")
    private String serverAdapter;

    private boolean loginSucceeded;

    protected final HttpClient httpClient;

    public AbstractBundleMojo() {
        this.httpClient = new HttpClient();
        this.httpClient.setFollowRedirects(false);
        this.httpClient.setHttpCookieStore(new HttpCookieStore.Default());
        try {
            this.httpClient.start();
            this.getLog().info("Using Jetty HttpClient!!");
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    public abstract void doExecute(File bundle, BundleInfo info) throws IOException, MojoExecutionException;

    public abstract void handleException(Exception ex) throws MojoExecutionException;

    @Override
    public void execute() throws MojoExecutionException {
        File bundle = new File(this.bundleFileName);
        try {
            BundleInfo info = this.getBundleInfo(bundle);
            // First login, then while installing bundle, HttpClient will pass the JSESSIONID received
            // in the Set-Cookie header in the auth call. if authentication fails, discontinue the further execution.
            this.initServerHttpSession();
            if (this.login()) {
                this.doExecute(bundle, info);
            } else {
                this.handleLoginFailure();
            }
        } catch (ExecutionException | InterruptedException | TimeoutException | IOException | IllegalArgumentException ex) {
            this.handleException(ex);
        } finally {
            this.logout();
            this.closeHttpClient();
        }
    }

    URI getFullUri(String url) {
        if (!StringUtils.startsWith(url, "/")) {
            url = "/" + url;
        }
        URI uri = URI.create(this.baseUrl + url);
        this.getLog().debug("URI to hit: " + uri);
        return uri;
    }

    private void initServerHttpSession() throws ExecutionException, InterruptedException, TimeoutException {
        if (StringUtils.equalsIgnoreCase(this.serverAdapter, RT_ADAPTER_TOMCAT)) {
            ContentResponse response = this.httpClient.newRequest(this.getFullUri(this.consoleUrl))
                    .method(GET)
                    .send();
            if (HttpStatus.isSuccess(response.getStatus())) {
                this.getLog().debug("Invoked /system/console so that server HttpSession is initialized!");
            }
        }
    }

    boolean login() throws ExecutionException, InterruptedException, TimeoutException {
        Request request = this.httpClient.newRequest(this.getFullUri(this.loginUrl)).method(POST);
        Fields fields = new Fields();
        fields.put(J_USERNAME, this.user);
        fields.put(J_PASSWORD, this.password);
        request.body(new FormRequestContent(fields));
        ContentResponse response = request.send();
        int status = response.getStatus();
        if (HttpStatus.isSuccess(status) || HttpStatus.isRedirection(status)) {
            this.loginSucceeded = this.httpClient.getHttpCookieStore()
                    .all()
                    .stream()
                    .anyMatch(cookie -> StringUtils.startsWith(cookie.getName(), COOKIE_JSESSIONID));
            if (this.loginSucceeded) {
                this.getLog().info("Login Successful!!");
            }
        }
        return this.loginSucceeded;
    }

    void logout() {
        if (this.loginSucceeded) {
            this.getLog().info("Invoking Logout!!");
            try {
                Request request = this.httpClient.newRequest(this.getFullUri(this.logoutUrl)).method(GET);
                ContentResponse response = request.send();
                this.getLog().info("Logout status code: " + response.getStatus());
                this.getLog().info("Logout successful!!");
                this.httpClient.getHttpCookieStore().clear();
            } catch (Exception ex) {
                this.getLog().error(ex);
            }
        }
    }

    void closeHttpClient() {
        try {
            this.httpClient.getHttpCookieStore().clear();
            this.httpClient.stop();
            this.getLog().debug("HttpClient closed!!");
        } catch (Exception ex) {
            this.getLog().error(ex);
        }
    }

    void handleLoginFailure() throws MojoExecutionException {
        if (this.failOnError) {
            throw new MojoExecutionException("[Authentication failed, please check credentials!!]");
        }
        this.getLog().error("Authentication failed, please check credentials!!");
    }

    BundleInfo getBundleInfo(File bundle) throws IOException {
        try (JarFile bundleArchive = new JarFile(bundle)) {
            return new BundleInfo(bundleArchive.getManifest());
        }
    }
}
