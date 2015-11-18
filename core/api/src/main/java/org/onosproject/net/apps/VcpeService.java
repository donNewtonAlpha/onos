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



public interface VcpeService {

    public void enableInternet(int clientId);

    public void disableInternet(int clientId);

    public void generalDNS(int clientId);

    public void personalDNS(int clientId);

    public void noDNS(int clientId);

    public void enableUverse(int clientId);

    public void disableUverse(int clientId);

    public void initiateCustomer(int clientID);

    public void discoverLan(int clientId);

}
