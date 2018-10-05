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

/**
 * A simple Bundle information holder.
 *
 * @author Rakesh.Kumar, AdeptJ
 */
class BundleDTO {

    private final String bundleName;

    private final String bsn;

    private final String bundleVersion;

    BundleDTO(String bundleName, String bsn, String bundleVersion) {
        this.bundleName = bundleName;
        this.bsn = bsn;
        this.bundleVersion = bundleVersion;
    }

    String getBundleName() {
        return this.bundleName;
    }

    String getBsn() {
        return this.bsn;
    }

    String getBundleVersion() {
        return this.bundleVersion;
    }

    @Override
    public String toString() {
        return "Bundle [" + this.bundleName + " (" + this.bsn + ")," + " version: " + this.bundleVersion + "]";
    }
}
