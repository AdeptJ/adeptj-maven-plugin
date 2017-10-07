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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.util.List;

/**
 * Install an OSGi bundle to a running AdeptJ Runtime instance.
 */
@Mojo(name = "install-file", requiresProject = false)
public class BundleInstallFileMojo extends AbstractBundleMojo {

    @Parameter(property = "adeptj.file")
    private String bundleFileName;

    @Parameter(property = "adeptj.groupId")
    private String groupId;

    @Parameter(property = "adeptj.artifactId")
    private String artifactId;

    @Parameter(property = "adeptj.version")
    private String version;

    @Parameter(property = "adeptj.packaging", defaultValue = "jar")
    private String packaging = "jar";

    @Parameter(property = "adeptj.classifier")
    private String classifier;

    @Parameter(property = "project.remoteArtifactRepositories", required = true, readonly = true)
    private List<Object> pomRemoteRepositories;

    @Parameter(property = "adeptj.repoId", defaultValue = "temp")
    private String repositoryId = "temp";

    /**
     * A string of the form groupId:artifactId:version[:packaging[:classifier]].
     */
    @Parameter(property = "adeptj.artifact")
    private String artifact;

    /**
     * The url of the repository from which we'll download the artifact
     */
    @Parameter(property = "adeptj.repoUrl")
    private String repositoryUrl;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Log log = getLog();
        log.info("@@@@@@ In BundleInstallFileMojo @@@@@@");
    }
}