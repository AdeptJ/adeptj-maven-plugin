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

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.adeptj.maven.plugin.bundle.Constants.AMPERSAND;
import static com.adeptj.maven.plugin.bundle.Constants.EQ;
import static com.adeptj.maven.plugin.bundle.Constants.FORM_URL_ENCODED;
import static com.adeptj.maven.plugin.bundle.Constants.HEADER_CONTENT_TYPE;
import static com.adeptj.maven.plugin.bundle.Constants.MULTIPART_FORM_DATA;
import static com.adeptj.maven.plugin.bundle.Constants.PARAM_ACTION;
import static com.adeptj.maven.plugin.bundle.Constants.PARAM_ACTION_INSTALL_VALUE;
import static com.adeptj.maven.plugin.bundle.Constants.PARAM_BUNDLE_FILE;
import static com.adeptj.maven.plugin.bundle.Constants.PARAM_PARALLEL_VERSION;
import static com.adeptj.maven.plugin.bundle.Constants.PARAM_REFRESH_PACKAGES;
import static com.adeptj.maven.plugin.bundle.Constants.PARAM_START;
import static com.adeptj.maven.plugin.bundle.Constants.PARAM_START_LEVEL;
import static com.adeptj.maven.plugin.bundle.Constants.VALUE_TRUE;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Utility methods.
 *
 * @author Rakesh.Kumar, AdeptJ
 */
final class BundleMojoUtil {

    private BundleMojoUtil() {
    }

    static HttpRequest formUrlEncodedRequest(URI uri, Map<String, Object> data) {
        String body = data.entrySet()
                .stream()
                .map(entry -> URLEncoder.encode(entry.getKey(), UTF_8)
                        + EQ
                        + URLEncoder.encode(entry.getValue().toString(), UTF_8))
                .collect(Collectors.joining(AMPERSAND));
        return HttpRequest.newBuilder()
                .uri(uri)
                .header(HEADER_CONTENT_TYPE, FORM_URL_ENCODED)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
    }

    static HttpRequest bundleInstallRequest(File bundle, URI uri, String bundleStartLevel,
                                            boolean startBundle,
                                            boolean refreshPackages, boolean parallelVersion) throws IOException {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put(PARAM_ACTION, PARAM_ACTION_INSTALL_VALUE);
        data.put(PARAM_START_LEVEL, bundleStartLevel);
        data.put(PARAM_BUNDLE_FILE, bundle.toPath());
        if (startBundle) {
            data.put(PARAM_START, VALUE_TRUE);
        }
        if (refreshPackages) {
            data.put(PARAM_REFRESH_PACKAGES, VALUE_TRUE);
        }
        // Since web console v4.4.0
        if (parallelVersion) {
            data.put(PARAM_PARALLEL_VERSION, VALUE_TRUE);
        }
        String boundary = UUID.randomUUID().toString();
        List<byte[]> byteArrays = new ArrayList<>();
        byte[] separator = ("--" + boundary + "\r\nContent-Disposition: form-data; name=").getBytes(UTF_8);
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            byteArrays.add(separator);
            if (entry.getValue() instanceof Path) {
                Path path = (Path) entry.getValue();
                byteArrays.add(("\"" + entry.getKey() + "\"; filename=\"" + path.getFileName()
                        + "\"\r\nContent-Type: " + Files.probeContentType(path) + "\r\n\r\n").getBytes(UTF_8));
                byteArrays.add(Files.readAllBytes(path));
                byteArrays.add("\r\n".getBytes(UTF_8));
            } else {
                byteArrays.add(("\"" + entry.getKey() + "\"\r\n\r\n" + entry.getValue() + "\r\n").getBytes(UTF_8));
            }
        }
        byteArrays.add(("--" + boundary + "--").getBytes(UTF_8));
        return HttpRequest.newBuilder()
                .uri(uri)
                .header(HEADER_CONTENT_TYPE, MULTIPART_FORM_DATA + ";boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArrays(byteArrays))
                .build();
    }
}
