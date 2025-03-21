/*
###############################################################################
#                                                                             #
#    Copyright 2016-2024, AdeptJ (http://www.adeptj.com)                      #
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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

/**
 * Utility methods.
 *
 * @author Rakesh.Kumar, AdeptJ
 */
final class BundleMojoUtil {

    private BundleMojoUtil() {
    }

    static void doHandleException(Log log, Exception ex, String op, String consoleUrl) throws MojoExecutionException {
        log.error(ex);
        if (ex instanceof MojoExecutionException) {
            throw (MojoExecutionException) ex;
        }
        String message = String.format("Bundle %s operation on [%s] failed, cause: %s", op, consoleUrl, ex.getMessage());
        throw new MojoExecutionException(message, ex);
    }
}
