/**
 *  Copyright (c) 2013 Open Networking Laboratory
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE. 
 * 
 */

package net.onrc.openvirtex.messages;

import java.util.LinkedList;
import java.util.List;

import net.onrc.openvirtex.elements.datapath.PhysicalSwitch;
import net.onrc.openvirtex.messages.actions.OVXActionOutput;

import org.openflow.protocol.OFMatch;
import org.openflow.protocol.OFPacketIn;
import org.openflow.protocol.OFPort;
import org.openflow.protocol.action.OFAction;

public class OVXPacketIn extends OFPacketIn implements Virtualizable {

    @Override
    public void virtualize(PhysicalSwitch sw) {
	OVXFlowMod fm = new OVXFlowMod();
	OFMatch match = new OFMatch();
	match.loadFromPacket(this.getPacketData(), this.getInPort());
	fm.setMatch(match);
	List<OFAction> actions = new LinkedList<OFAction>();
	OVXActionOutput out = new OVXActionOutput();
	out.setPort(OFPort.OFPP_FLOOD.getValue());
	actions.add(out);
	fm.setActions(actions);
	fm.setLengthU(fm.getLengthU() + out.getLengthU());
	
	sw.sendMsg(fm, sw);

    }

}
