package org.onosproject.net.apps;

/*
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

public interface AbstractSwitchService {

    // @return a string representation of the knowledge
    public String getKnowledgeData();

    // @return true if the child is added successfully, false otherwise
    public boolean addChild(String childIp);

    // @return true if the NRP is initiated, false otherwise
    public boolean initiateNameResolutionProtocol();

    // @return true if the TDP is initiated, false otherwise
    public boolean initiateTopologyDiscoveryProtocol();

    // @return true if the intent is created, false if the system is unable to do so
    public boolean createHostToHostIntent(String host1Mac, String host2Mac);

    // @return true if the expanded topology can be computed, false if the system is unable to do so
    public boolean computeExpandedTopology();

    // @return true if the knowledge is reset, false if the system is unable to do so
    public boolean resetKnowledge();

    // @return true if the test case exist and is loaded successfully, false otherwise
    public boolean fillDb(int testCase);

}
