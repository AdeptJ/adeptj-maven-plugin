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
public class Constants {

    static final String J_USERNAME = "j_username";

    static final String J_PASSWORD = "j_password";

    static final String HEADER_JSESSIONID = "JSESSIONID";

    static final String DEFAULT_AUTH_URL = "http://localhost:9007/auth/j_security_check";

    static final String DEFAULT_CONSOLE_URL = "http://localhost:9007/system/console";

    static final String URL_INSTALL = "/install";

    static final String URL_UNINSTALL = "/bundles/%s";

    static final String HEADER_SET_COOKIE = "Set-Cookie";

    static final String REGEX_SEMI_COLON = ";";

    static final String REGEX_EQ = "=";

    static final String UTF_8 = "UTF-8";

    static final String PARAM_STARTLEVEL = "bundlestartlevel";

    static final String PARAM_START = "bundlestart";

    static final String PARAM_BUNDLEFILE = "bundlefile";

    static final String PARAM_REFRESH_PACKAGES = "refreshPackages";

    static final String PARAM_ACTION = "action";

    static final String PARAM_ACTION_VALUE = "install";

    static final String VALUE_TRUE = "true";

    static final String BUNDLE_NAME = "Bundle-Name";

    static final String BUNDLE_VERSION = "Bundle-Version";

    static final String BUNDLE_SYMBOLICNAME = "Bundle-SymbolicName";
}
