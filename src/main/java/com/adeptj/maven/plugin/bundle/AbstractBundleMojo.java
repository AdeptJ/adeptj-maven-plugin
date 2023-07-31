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
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.cookie.BasicCookieStore;
import org.apache.hc.client5.http.cookie.CookieStore;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.entity.HttpEntities;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;

import static com.adeptj.maven.plugin.bundle.Constants.COOKIE_JSESSIONID;
import static com.adeptj.maven.plugin.bundle.Constants.DEFAULT_AUTH_URL;
import static com.adeptj.maven.plugin.bundle.Constants.DEFAULT_BASE_URL;
import static com.adeptj.maven.plugin.bundle.Constants.DEFAULT_CONSOLE_URL;
import static com.adeptj.maven.plugin.bundle.Constants.DEFAULT_LOGOUT_URL;
import static com.adeptj.maven.plugin.bundle.Constants.J_PASSWORD;
import static com.adeptj.maven.plugin.bundle.Constants.J_USERNAME;
import static com.adeptj.maven.plugin.bundle.Constants.RT_ADAPTER_TOMCAT;
import static com.adeptj.maven.plugin.bundle.Constants.VALUE_FALSE;
import static com.adeptj.maven.plugin.bundle.Constants.VALUE_TRUE;
import static java.nio.charset.StandardCharsets.UTF_8;

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

    @Parameter(property = "adeptj.auth.url", defaultValue = DEFAULT_AUTH_URL, required = true)
    private String authUrl;

    @Parameter(property = "adeptj.logout.url", defaultValue = DEFAULT_LOGOUT_URL)
    private String logoutUrl;

    @Parameter(property = "adeptj.user", defaultValue = "admin", required = true)
    private String user;

    @Parameter(property = "adeptj.password", defaultValue = "admin", required = true)
    private String password;

    @Parameter(property = "adeptj.server.adapter")
    private String serverAdapter;

    private final CookieStore cookieStore;

    final CloseableHttpClient httpClient;

    final HttpClientResponseHandler<ClientResponse> responseHandler;

    private boolean loginSucceeded;

    public AbstractBundleMojo() {
        this.cookieStore = new BasicCookieStore();
        this.httpClient = HttpClients.custom()
                .disableRedirectHandling()
                .setDefaultCookieStore(this.cookieStore)
                .build();
        this.responseHandler = new ResponseHandler();
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
        } catch (IOException | IllegalArgumentException ex) {
            this.handleException(ex);
        } finally {
            this.logout();
            this.closeHttpClient();
        }
    }

    URI getUri(String url) {
        if (!StringUtils.startsWith(url, "/")) {
            url = "/" + url;
        }
        URI uri = URI.create(this.baseUrl + url);
        this.getLog().debug("URI to hit: " + uri);
        return uri;
    }

    private void initServerHttpSession() throws IOException {
        if (StringUtils.equalsIgnoreCase(this.serverAdapter, RT_ADAPTER_TOMCAT)) {
            HttpGet request = new HttpGet(this.getUri(this.consoleUrl));
            ClientResponse response = this.httpClient.execute(request, this.responseHandler);
            if (response.isOk()) {
                this.getLog().debug("Invoked /system/console so that server HttpSession is initialized!");
            }
        }
    }

    boolean login() throws IOException {
        HttpPost request = new HttpPost(this.getUri(this.authUrl));
        List<NameValuePair> form = new ArrayList<>();
        form.add(new BasicNameValuePair(J_USERNAME, this.user));
        form.add(new BasicNameValuePair(J_PASSWORD, this.password));
        request.setEntity(HttpEntities.createUrlEncoded(form, UTF_8));
        ClientResponse response = this.httpClient.execute(request, this.responseHandler);
        this.getLog().debug("Login status code: " + response.getCode());
        this.loginSucceeded = this.cookieStore.getCookies()
                .stream()
                .anyMatch(cookie -> StringUtils.startsWith(cookie.getName(), COOKIE_JSESSIONID));
        return this.loginSucceeded;
    }

    void logout() {
        if (this.loginSucceeded) {
            this.getLog().debug("Invoking Logout!!");
            try {
                HttpGet request = new HttpGet(this.getUri(this.logoutUrl));
                ClientResponse response = this.httpClient.execute(request, this.responseHandler);
                this.getLog().debug("Logout status code: " + response.getCode());
                this.getLog().debug("Logout successful!!");
                this.cookieStore.clear();
            } catch (IOException ex) {
                this.getLog().error(ex);
            }
        }
    }

    void closeHttpClient() {
        try {
            this.cookieStore.clear();
            this.httpClient.close();
            this.getLog().debug("HttpClient closed!!");
        } catch (IOException ex) {
            this.getLog().error(ex);
        }
    }

    BundleInfo getBundleInfo(File bundle) throws IOException {
        try (JarFile bundleArchive = new JarFile(bundle)) {
            return new BundleInfo(bundleArchive.getManifest());
        }
    }

    void handleLoginFailure() throws MojoExecutionException {
        if (this.failOnError) {
            throw new MojoExecutionException("[Authentication failed, please check credentials!!]");
        }
        this.getLog().error("Authentication failed, please check credentials!!");
    }
}
