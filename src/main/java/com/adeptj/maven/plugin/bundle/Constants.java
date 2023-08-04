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

    static final String DEFAULT_BASE_URL = "http://localhost:8080";

    static final String DEFAULT_LOGIN_URL = "/admin/login/j_security_check";

    static final String DEFAULT_LOGOUT_URL = "/admin/logout";

    static final String DEFAULT_CONSOLE_URL = "/system/console";

    static final String URL_BUNDLE_INSTALL = "%s/install";

    static final String URL_BUNDLE_UNINSTALL = "%s/bundles/%s";

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

    static final String RT_ADAPTER_TOMCAT = "tomcat";
}
