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

import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.core5.http.HttpEntity;

import java.io.File;

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

    static HttpEntity newMultipartEntity(File bundle, String startLevel, boolean startBundle, boolean refreshPackages,
                                         boolean parallelVersion) {
        MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder.create()
                .setCharset(UTF_8)
                .addBinaryBody(PARAM_BUNDLE_FILE, bundle)
                .addTextBody(PARAM_ACTION, PARAM_ACTION_INSTALL_VALUE)
                .addTextBody(PARAM_START_LEVEL, startLevel);
        if (startBundle) {
            multipartEntityBuilder.addTextBody(PARAM_START, VALUE_TRUE);
        }
        if (refreshPackages) {
            multipartEntityBuilder.addTextBody(PARAM_REFRESH_PACKAGES, VALUE_TRUE);
        }
        // Since web console v4.4.0
        if (parallelVersion) {
            multipartEntityBuilder.addTextBody(PARAM_PARALLEL_VERSION, VALUE_TRUE);
        }
        return multipartEntityBuilder.build();
    }
}
