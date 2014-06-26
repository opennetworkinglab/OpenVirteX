/*******************************************************************************
 * Copyright 2014 Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package net.onrc.openvirtex.elements.address;


public class PhysicalIPAddress extends IPAddress {
    final public static String IP_FOR_SOURCE = "source";
    final public static String IP_FOR_DESTINATION = "destination";

    private int tid;

    public PhysicalIPAddress(final Integer ip) {
        this.ip = ip;
    }

    public PhysicalIPAddress(final String ipAddress) {
        super(ipAddress);
    }
    public void setTenantId(Integer tid){
        this.tid = tid;
    }

    public Integer getTenantId() {
        return this.tid;
    }

}
