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
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.util.Fields;
import org.eclipse.jetty.util.HttpCookieStore;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.jar.JarFile;

import static com.adeptj.maven.plugin.bundle.Constants.COOKIE_JSESSIONID;
import static com.adeptj.maven.plugin.bundle.Constants.DEFAULT_AUTH_URL;
import static com.adeptj.maven.plugin.bundle.Constants.DEFAULT_CONSOLE_URL;
import static com.adeptj.maven.plugin.bundle.Constants.DEFAULT_LOGOUT_URL;
import static com.adeptj.maven.plugin.bundle.Constants.J_PASSWORD;
import static com.adeptj.maven.plugin.bundle.Constants.J_USERNAME;
import static com.adeptj.maven.plugin.bundle.Constants.RT_ADAPTER_TOMCAT;
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

    @Parameter(property = "adeptj.runtime.adapter")
    private String adapter;

    private boolean loginSucceeded;

    protected final HttpClient httpClient;

    public AbstractBundleMojo() {
        this.httpClient = new HttpClient();
        this.httpClient.setFollowRedirects(false);
        this.httpClient.setCookieStore(new HttpCookieStore());
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

    private void initServerHttpSession() throws ExecutionException, InterruptedException, TimeoutException {
        if (StringUtils.equalsIgnoreCase(this.adapter, RT_ADAPTER_TOMCAT)) {
            ContentResponse response = this.httpClient.newRequest(this.consoleUrl)
                    .method(HttpMethod.GET)
                    .send();
            if (HttpStatus.isSuccess(response.getStatus())) {
                this.getLog().debug("Invoked /system/console so that server HttpSession is initialized!");
            }
        }
    }

    boolean login() throws ExecutionException, InterruptedException, TimeoutException {
        Request request = this.httpClient.newRequest(this.authUrl).method(HttpMethod.POST);
        Fields fields = new Fields();
        fields.put(J_USERNAME, this.user);
        fields.put(J_PASSWORD, this.password);
        request.body(new FormRequestContent(fields));
        ContentResponse response = request.send();
        int status = response.getStatus();
        if (HttpStatus.isSuccess(status) || HttpStatus.isRedirection(status)) {
            this.loginSucceeded = this.httpClient.getCookieStore()
                    .getCookies()
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

    void closeHttpClient() {
        try {
            this.httpClient.getCookieStore().removeAll();
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
