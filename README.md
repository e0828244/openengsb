====
    Licensed to the Austrian Association for Software Tool Integration (AASTI)
    under one or more contributor license agreements. See the NOTICE file
    distributed with this work for additional information regarding copyright
    ownership. The AASTI licenses this file to you under the Apache License,
    Version 2.0 (the "License"); you may not use this file except in compliance
    with the License. You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
====

Setup Project and Kick Off
==========================
Execute "mvn install"

Now run "mvn eclipse:eclipse" and import the projects into eclipse. If you simply want to run the openengsb execute "mvn install openengsb:provision"

Scripts
-------
Scripts which help you to create projects, run the workspace and so on are stored in a seperated script folder (etc/scripts). Its not required to use them but they may help you with your effort.

Full Test
---------
Before creating a pull request, run the following command:

etc/scripts/pre-push.sh (or "mvn clean install -Plicense-check,integration-test,checkstyle")

Further Information
-------------------
This readme gives only the most important information for developer. General information about this project is located at http://openengsb.org. The detailed developer and user documentation is located at http://openengsb.org/manual/index.html.

