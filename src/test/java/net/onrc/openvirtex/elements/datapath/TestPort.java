/******************************************************************************
 * Copyright 2014 Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package net.onrc.openvirtex.elements.datapath;

import net.onrc.openvirtex.elements.port.PhysicalPort;

import org.openflow.protocol.OFPhysicalPort;

public class TestPort extends PhysicalPort {

    public TestPort(final PhysicalSwitch psw, final boolean IsEdge,
            final byte[] hw, final short PortNumber) {
        super(new OFPhysicalPort(), psw, IsEdge);
        this.hardwareAddress = hw;
        this.portNumber = PortNumber;
    }
}