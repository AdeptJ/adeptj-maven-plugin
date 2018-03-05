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
import org.apache.http.Header;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.adeptj.maven.plugin.bundle.Constants.DEFAULT_AUTH_URL;
import static com.adeptj.maven.plugin.bundle.Constants.DEFAULT_CONSOLE_URL;
import static com.adeptj.maven.plugin.bundle.Constants.HEADER_JSESSIONID;
import static com.adeptj.maven.plugin.bundle.Constants.HEADER_SET_COOKIE;
import static com.adeptj.maven.plugin.bundle.Constants.J_PASSWORD;
import static com.adeptj.maven.plugin.bundle.Constants.J_USERNAME;
import static com.adeptj.maven.plugin.bundle.Constants.REGEX_EQ;
import static com.adeptj.maven.plugin.bundle.Constants.REGEX_SEMI_COLON;
import static com.adeptj.maven.plugin.bundle.Constants.UTF_8;
import static com.adeptj.maven.plugin.bundle.Constants.VALUE_TRUE;

/**
 * AbstractBundleMojo
 *
 * @author Rakesh.Kumar, AdeptJ
 */
abstract class AbstractBundleMojo extends AbstractMojo {

    @Parameter(property = "adeptj.console.url", defaultValue = DEFAULT_CONSOLE_URL, required = true)
    String adeptjConsoleURL;

    @Parameter(property = "adeptj.failOnError", defaultValue = VALUE_TRUE, required = true)
    boolean failOnError;

    @Parameter(property = "adeptj.auth.url", defaultValue = DEFAULT_AUTH_URL, required = true)
    private String authUrl;

    @Parameter(property = "adeptj.user", defaultValue = "admin", required = true)
    private String user;

    @Parameter(property = "adeptj.password", defaultValue = "admin", required = true)
    private String password;

    private CloseableHttpClient httpClient;

    AbstractBundleMojo() {
        this.httpClient = HttpClients.createDefault();
    }

    CloseableHttpClient getHttpClient() {
        return this.httpClient;
    }

    boolean authenticate() throws IOException {
        List<NameValuePair> authForm = new ArrayList<>();
        authForm.add(new BasicNameValuePair(J_USERNAME, this.user));
        authForm.add(new BasicNameValuePair(J_PASSWORD, this.password));
        try (CloseableHttpResponse authResponse = this.httpClient.execute(RequestBuilder
                .post(this.authUrl)
                .setEntity(new UrlEncodedFormEntity(authForm, UTF_8))
                .build())) {
            return this.parseResponse(authResponse);
        }
    }

    private boolean parseResponse(CloseableHttpResponse authResponse) {
        String sessionId = null;
        for (Header header : authResponse.getAllHeaders()) {
            String headerName = header.getName();
            if (HEADER_SET_COOKIE.equals(headerName)) {
                for (String part : header.getValue().split(REGEX_SEMI_COLON)) {
                    if (part.startsWith(HEADER_JSESSIONID)) {
                        sessionId = part.split(REGEX_EQ)[1];
                        break;
                    }
                }
            } else if (HEADER_JSESSIONID.equals(headerName)) {
                sessionId = header.getValue();
                break;
            }
        }
        return StringUtils.isNotEmpty(sessionId);
    }
}
