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
import org.apache.commons.lang3.Validate;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.IOException;
import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarFile;

import static com.adeptj.maven.plugin.bundle.Constants.BUNDLE_NAME;
import static com.adeptj.maven.plugin.bundle.Constants.BUNDLE_SYMBOLIC_NAME;
import static com.adeptj.maven.plugin.bundle.Constants.BUNDLE_VERSION;
import static com.adeptj.maven.plugin.bundle.Constants.DEFAULT_AUTH_URL;
import static com.adeptj.maven.plugin.bundle.Constants.DEFAULT_CONSOLE_URL;
import static com.adeptj.maven.plugin.bundle.Constants.DEFAULT_LOGOUT_URL;
import static com.adeptj.maven.plugin.bundle.Constants.HEADER_JSESSIONID;
import static com.adeptj.maven.plugin.bundle.Constants.HEADER_SET_COOKIE;
import static com.adeptj.maven.plugin.bundle.Constants.J_PASSWORD;
import static com.adeptj.maven.plugin.bundle.Constants.J_USERNAME;
import static com.adeptj.maven.plugin.bundle.Constants.REGEX_EQ;
import static com.adeptj.maven.plugin.bundle.Constants.REGEX_SEMI_COLON;
import static com.adeptj.maven.plugin.bundle.Constants.VALUE_TRUE;

/**
 * Base for various bundle mojo implementations.
 *
 * @author Rakesh.Kumar, AdeptJ
 */
abstract class AbstractBundleMojo extends AbstractMojo {

    @Parameter(property = "adeptj.console.url", defaultValue = DEFAULT_CONSOLE_URL, required = true)
    String adeptjConsoleURL;

    @Parameter(property = "adeptj.auth.url", defaultValue = DEFAULT_AUTH_URL, required = true)
    private String authUrl;

    @Parameter(property = "adeptj.logout.url", defaultValue = DEFAULT_LOGOUT_URL)
    private String logoutUrl;

    @Parameter(property = "adeptj.user", defaultValue = "admin", required = true)
    private String user;

    @Parameter(property = "adeptj.password", defaultValue = "admin", required = true)
    private String password;

    @Parameter(property = "adeptj.failOnError", defaultValue = VALUE_TRUE, required = true)
    boolean failOnError;

    private boolean loginSucceeded;

    final HttpClient httpClient;

    public AbstractBundleMojo() {
        this.httpClient = HttpClient.newBuilder()
                .cookieHandler(new CookieManager())
                .followRedirects(HttpClient.Redirect.NORMAL)
                .version(HttpClient.Version.HTTP_2)
                .build();
        this.getLog().debug("JDK HttpClient initialized!!");
    }

    boolean login() {
        Map<String, Object> data = new HashMap<>();
        data.put(J_USERNAME, this.user);
        data.put(J_PASSWORD, this.password);
        getLog().info(data.toString());
        HttpRequest loginRequest = HttpRequest.newBuilder()
                .POST(BundleMojoUtil.ofFormData(data))
                .uri(URI.create(this.authUrl))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .build();
        try {
            HttpResponse<String> httpResponse = this.httpClient.send(loginRequest, HttpResponse.BodyHandlers.ofString());
            System.out.println(httpResponse.headers().allValues(HEADER_SET_COOKIE));
            getLog().info(httpResponse.body());
            String sessionId = null;
            for (String header : httpResponse.headers().allValues(HEADER_SET_COOKIE)) {
                for (String token : header.split(REGEX_SEMI_COLON)) {
                    if (StringUtils.startsWith(token, HEADER_JSESSIONID)) {
                        sessionId = token.split(REGEX_EQ)[1];
                        this.getLog().info("Retrieved AdeptJ SessionId from [SET-COOKIE] header: " + sessionId);
                        break;
                    }
                }
                if (StringUtils.isNotEmpty(sessionId)) {
                    this.loginSucceeded = true;
                    this.getLog().info("Login succeeded!!");
                    break;
                }
            }
        } catch (Exception ex) {
            this.getLog().error(ex);
            throw new BundleMojoException(ex);
        }
        return this.loginSucceeded;
    }

    void logout() {
        if (this.loginSucceeded) {
            this.getLog().info("Invoking Logout!!");
            HttpRequest logoutRequest = HttpRequest.newBuilder()
                    .GET()
                    .uri(URI.create(this.logoutUrl))
                    .build();
            try {
                HttpResponse<String> response = this.httpClient.send(logoutRequest, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    this.getLog().info("Logout successful!!");
                } else {
                    this.getLog().info("Logout failed!!");
                }
            } catch (IOException | InterruptedException ex) {
                this.getLog().error(ex);
            }
        }
    }

    void logBundleDetails(BundleDTO dto, BundleMojoOp op) {
        switch (op) {
            case INSTALL:
                this.getLog().info("Installing " + dto);
                break;
            case UNINSTALL:
                this.getLog().info("Uninstalling " + dto);
                break;
        }
    }

    BundleDTO getBundleDTO(File bundle) throws IOException {
        try (JarFile bundleArchive = new JarFile(bundle)) {
            Attributes mainAttributes = bundleArchive.getManifest().getMainAttributes();
            String bundleName = mainAttributes.getValue(BUNDLE_NAME);
            Validate.isTrue(StringUtils.isNotEmpty(bundleName), "Artifact is not a Bundle!!");
            String symbolicName = mainAttributes.getValue(BUNDLE_SYMBOLIC_NAME);
            Validate.isTrue(StringUtils.isNotEmpty(symbolicName), "Bundle symbolic name is blank!!");
            String bundleVersion = mainAttributes.getValue(BUNDLE_VERSION);
            return new BundleDTO(bundleName, symbolicName, bundleVersion);
        }
    }
}
