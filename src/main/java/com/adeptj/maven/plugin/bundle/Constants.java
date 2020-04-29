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
 * Constants
 *
 * @author Rakesh.Kumar, AdeptJ
 */
public final class Constants {

    private Constants() {
    }

    static final String J_USERNAME = "j_username";

    static final String J_PASSWORD = "j_password";

    static final String COOKIE_JSESSIONID = "JSESSIONID";

    static final String DEFAULT_AUTH_URL = "http://localhost:8080/admin/auth/j_security_check";

    static final String DEFAULT_LOGOUT_URL = "http://localhost:8080/admin/logout";

    static final String DEFAULT_CONSOLE_URL = "http://localhost:8080/system/console";

    static final String URL_INSTALL = "/install";

    static final String URL_UNINSTALL = "/bundles/%s";

    static final String PARAM_START_LEVEL = "bundlestartlevel";

    static final String PARAM_START = "bundlestart";

    static final String PARAM_BUNDLE_FILE = "bundlefile";

    static final String PARAM_REFRESH_PACKAGES = "refreshPackages";

    static final String PARAM_PARALLEL_VERSION = "parallelVersion";

    static final String PARAM_ACTION = "action";

    static final String PARAM_ACTION_INSTALL_VALUE = "install";

    static final String PARAM_ACTION_UNINSTALL_VALUE = "uninstall";

    static final String VALUE_TRUE = "true";

    static final String VALUE_FALSE = "false";

    static final String BUNDLE_NAME = "Bundle-Name";

    static final String BUNDLE_VERSION = "Bundle-Version";

    static final String BUNDLE_SYMBOLIC_NAME = "Bundle-SymbolicName";
}
