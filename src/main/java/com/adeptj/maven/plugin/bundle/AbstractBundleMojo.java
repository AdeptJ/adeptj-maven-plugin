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
import static com.adeptj.maven.plugin.bundle.Constants.SC_OK;
import static com.adeptj.maven.plugin.bundle.Constants.VALUE_TRUE;

/**
 * Base for various bundle mojo implementations.
 *
 * @author Rakesh.Kumar, AdeptJ
 */
abstract class AbstractBundleMojo extends AbstractMojo {

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
    }

    boolean login() {
        Map<String, Object> data = Map.of(J_USERNAME, this.user, J_PASSWORD, this.password);
        HttpRequest request = BundleMojoUtil.formUrlEncodedRequest(URI.create(this.authUrl), data);
        try {
            this.loginSucceeded = this.httpClient.send(request, HttpResponse.BodyHandlers.discarding())
                    .headers()
                    .allValues(HEADER_SET_COOKIE)
                    .stream()
                    .map(value -> value.split(REGEX_SEMI_COLON))
                    .filter(mapping -> StringUtils.startsWith(mapping[0], HEADER_JSESSIONID))
                    .map(mapping -> mapping[0].split(REGEX_EQ)[1])
                    .anyMatch(StringUtils::isNotEmpty);
        } catch (IOException ex) {
            throw new BundleMojoException(ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BundleMojoException(ex);
        }
        return this.loginSucceeded;
    }

    void logout() {
        if (this.loginSucceeded) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(this.logoutUrl))
                    .GET()
                    .build();
            try {
                if (this.httpClient.send(request, HttpResponse.BodyHandlers.discarding()).statusCode() == SC_OK) {
                    this.getLog().debug("Logout successful!!");
                }
            } catch (IOException ex) {
                this.getLog().error(ex);
            } catch (InterruptedException ex) {
                this.getLog().error(ex);
                Thread.currentThread().interrupt();
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
