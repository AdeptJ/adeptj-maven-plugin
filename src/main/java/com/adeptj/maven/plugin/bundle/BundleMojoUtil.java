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

import org.eclipse.jetty.client.MultiPartRequestContent;
import org.eclipse.jetty.client.StringRequestContent;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.MultiPart;

import java.io.File;

import static com.adeptj.maven.plugin.bundle.Constants.PARAM_ACTION;
import static com.adeptj.maven.plugin.bundle.Constants.PARAM_ACTION_INSTALL_VALUE;
import static com.adeptj.maven.plugin.bundle.Constants.PARAM_BUNDLE_FILE;
import static com.adeptj.maven.plugin.bundle.Constants.PARAM_PARALLEL_VERSION;
import static com.adeptj.maven.plugin.bundle.Constants.PARAM_REFRESH_PACKAGES;
import static com.adeptj.maven.plugin.bundle.Constants.PARAM_START;
import static com.adeptj.maven.plugin.bundle.Constants.PARAM_START_LEVEL;
import static com.adeptj.maven.plugin.bundle.Constants.VALUE_TRUE;

/**
 * Utility methods.
 *
 * @author Rakesh.Kumar, AdeptJ
 */
final class BundleMojoUtil {

    private BundleMojoUtil() {
    }

    static MultiPartRequestContent newBundleInstallMultiPartRequestContent(File bundle, String startLevel,
                                                                           boolean startBundle,
                                                                           boolean refreshPackages,
                                                                           boolean parallelVersion) {
        MultiPartRequestContent content = new MultiPartRequestContent();
        content.addPart(new MultiPart.ContentSourcePart(PARAM_ACTION, null, HttpFields.EMPTY,
                new StringRequestContent(PARAM_ACTION_INSTALL_VALUE)));
        content.addPart(new MultiPart.ContentSourcePart(PARAM_START_LEVEL, null, HttpFields.EMPTY,
                new StringRequestContent(startLevel)));
        content.addPart(new MultiPart.PathPart(PARAM_BUNDLE_FILE, bundle.getName(), HttpFields.EMPTY, bundle.toPath()));
        if (startBundle) {
            content.addPart(new MultiPart.ContentSourcePart(PARAM_START, null, HttpFields.EMPTY,
                    new StringRequestContent(VALUE_TRUE)));
        }
        if (refreshPackages) {
            content.addPart(new MultiPart.ContentSourcePart(PARAM_REFRESH_PACKAGES, null, HttpFields.EMPTY,
                    new StringRequestContent(VALUE_TRUE)));
        }
        // Since web console v4.4.0
        if (parallelVersion) {
            content.addPart(new MultiPart.ContentSourcePart(PARAM_PARALLEL_VERSION, null, HttpFields.EMPTY,
                    new StringRequestContent(VALUE_TRUE)));
        }
        // MultiPartRequestContent must be closed before sending request.
        content.close();
        return content;
    }
}
