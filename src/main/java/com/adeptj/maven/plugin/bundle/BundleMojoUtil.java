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

import org.eclipse.jetty.client.util.MultiPartRequestContent;
import org.eclipse.jetty.client.util.PathRequestContent;
import org.eclipse.jetty.client.util.StringRequestContent;

import java.io.File;
import java.io.IOException;

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

    static MultiPartRequestContent newMultipartRequestContent(File bundle, String startLevel, boolean startBundle,
                                                              boolean refreshPackages,
                                                              boolean parallelVersion) throws IOException {
        MultiPartRequestContent content = new MultiPartRequestContent();
        content.addFieldPart(PARAM_ACTION, new StringRequestContent(PARAM_ACTION_INSTALL_VALUE), null);
        content.addFieldPart(PARAM_START_LEVEL, new StringRequestContent(startLevel), null);
        content.addFilePart(PARAM_BUNDLE_FILE, bundle.getName(), new PathRequestContent(bundle.toPath()), null);
        if (startBundle) {
            content.addFieldPart(PARAM_START, new StringRequestContent(VALUE_TRUE), null);
        }
        if (refreshPackages) {
            content.addFieldPart(PARAM_REFRESH_PACKAGES, new StringRequestContent(VALUE_TRUE), null);
        }
        // Since web console v4.4.0
        if (parallelVersion) {
            content.addFieldPart(PARAM_PARALLEL_VERSION, new StringRequestContent(VALUE_TRUE), null);
        }
        // MultiPartRequestContent must be closed before sending request.
        content.close();
        return content;
    }
}
