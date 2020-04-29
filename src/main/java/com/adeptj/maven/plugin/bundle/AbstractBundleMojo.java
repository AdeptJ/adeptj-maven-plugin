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
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.cookie.BasicCookieStore;
import org.apache.hc.client5.http.cookie.CookieStore;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.io.entity.EntityUtils;
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
import java.util.jar.Attributes;
import java.util.jar.JarFile;

import static com.adeptj.maven.plugin.bundle.Constants.BUNDLE_NAME;
import static com.adeptj.maven.plugin.bundle.Constants.BUNDLE_SYMBOLIC_NAME;
import static com.adeptj.maven.plugin.bundle.Constants.BUNDLE_VERSION;
import static com.adeptj.maven.plugin.bundle.Constants.COOKIE_JSESSIONID;
import static com.adeptj.maven.plugin.bundle.Constants.DEFAULT_AUTH_URL;
import static com.adeptj.maven.plugin.bundle.Constants.DEFAULT_CONSOLE_URL;
import static com.adeptj.maven.plugin.bundle.Constants.DEFAULT_LOGOUT_URL;
import static com.adeptj.maven.plugin.bundle.Constants.J_PASSWORD;
import static com.adeptj.maven.plugin.bundle.Constants.J_USERNAME;
import static com.adeptj.maven.plugin.bundle.Constants.URL_INSTALL;
import static com.adeptj.maven.plugin.bundle.Constants.VALUE_TRUE;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.hc.core5.http.HttpStatus.SC_OK;

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

    private final CookieStore cookieStore;

    final CloseableHttpClient httpClient;

    private boolean loginSucceeded;

    public AbstractBundleMojo() {
        this.cookieStore = new BasicCookieStore();
        this.httpClient = HttpClients.custom()
                .disableRedirectHandling()
                .setDefaultCookieStore(this.cookieStore)
                .build();
    }

    boolean login() {
        try {
            HttpPost request = new HttpPost(this.authUrl);
            List<NameValuePair> parameters = new ArrayList<>();
            parameters.add(new BasicNameValuePair(J_USERNAME, this.user));
            parameters.add(new BasicNameValuePair(J_PASSWORD, this.password));
            request.setEntity(HttpEntities.createUrlEncoded(parameters, UTF_8));
            try (CloseableHttpResponse response = this.httpClient.execute(request)) {
                EntityUtils.consumeQuietly(response.getEntity());
                this.loginSucceeded = this.cookieStore.getCookies()
                        .stream()
                        .anyMatch(cookie -> StringUtils.startsWith(cookie.getName(), COOKIE_JSESSIONID));
            }
        } catch (IOException ex) {
            throw new BundleMojoException(ex);
        }
        return this.loginSucceeded;
    }

    void logout() {
        if (this.loginSucceeded) {
            this.getLog().debug("Invoking Logout!!");
            try (CloseableHttpResponse response = this.httpClient.execute(new HttpGet(this.logoutUrl))) {
                this.getLog().debug("Logout status code: " + response.getCode());
                this.getLog().debug("Logout successful!!");
                EntityUtils.consumeQuietly(response.getEntity());
                this.cookieStore.clear();
            } catch (IOException ex) {
                this.getLog().error(ex);
            }
        }
    }

    void installBundle(HttpEntity multipartEntity) throws IOException, MojoExecutionException {
        HttpPost request = new HttpPost(URI.create(this.consoleUrl + URL_INSTALL));
        request.setEntity(multipartEntity);
        try (CloseableHttpResponse response = this.httpClient.execute(request)) {
            if (response.getCode() == SC_OK) {
                EntityUtils.consume(response.getEntity());
                this.getLog().info("Bundle installed successfully, please check AdeptJ OSGi Web Console"
                        + " [" + this.consoleUrl + "]");
            } else {
                if (this.failOnError) {
                    throw new MojoExecutionException(
                            String.format("Bundle installation failed, reason: [%s], status: [%s]",
                                    response.getReasonPhrase(),
                                    response.getCode()));
                }
                this.getLog().warn("Problem installing bundle, please check AdeptJ OSGi Web Console!!");
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
            Attributes mainAttributes = bundleArchive.getManifest().getMainAttributes();
            String bundleName = mainAttributes.getValue(BUNDLE_NAME);
            Validate.isTrue(StringUtils.isNotEmpty(bundleName), "Artifact is not a Bundle!!");
            String symbolicName = mainAttributes.getValue(BUNDLE_SYMBOLIC_NAME);
            Validate.isTrue(StringUtils.isNotEmpty(symbolicName), "Bundle symbolic name is blank!!");
            String bundleVersion = mainAttributes.getValue(BUNDLE_VERSION);
            return new BundleInfo(bundleName, symbolicName, bundleVersion);
        }
    }
}
