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

/**
 * A simple Bundle information holder.
 *
 * @author Rakesh.Kumar, AdeptJ
 */
class BundleInfo {

    private final String bundleName;

    private final String symbolicName;

    private final String bundleVersion;

    BundleInfo(String bundleName, String symbolicName, String bundleVersion) {
        Validate.isTrue(StringUtils.isNotEmpty(symbolicName), "Bundle symbolic name is null!!");
        Validate.isTrue(StringUtils.isNotEmpty(bundleName), "Artifact is not a Bundle!!");
        this.bundleName = bundleName;
        this.symbolicName = symbolicName;
        this.bundleVersion = bundleVersion;
    }

    String getSymbolicName() {
        return this.symbolicName;
    }

    @Override
    public String toString() {
        return "Bundle [" + this.bundleName + " (" + this.symbolicName + ")," + " version: " + this.bundleVersion + "]";
    }
}
