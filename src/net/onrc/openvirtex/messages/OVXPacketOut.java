/**
 * Copyright (c) 2013 Open Networking Laboratory
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of
 * the Software, and to permit persons to whom the Software is furnished to do
 * so,
 * subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS
 * OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 * 
 */

package net.onrc.openvirtex.messages;

import java.util.LinkedList;
import java.util.List;

import net.onrc.openvirtex.elements.Mappable;
import net.onrc.openvirtex.elements.OVXMap;
import net.onrc.openvirtex.elements.address.OVXIPAddress;
import net.onrc.openvirtex.elements.address.PhysicalIPAddress;
import net.onrc.openvirtex.elements.datapath.OVXSwitch;
import net.onrc.openvirtex.elements.port.OVXPort;
import net.onrc.openvirtex.exceptions.ActionVirtualizationDenied;
import net.onrc.openvirtex.messages.actions.OVXActionNetworkLayerDestination;
import net.onrc.openvirtex.messages.actions.OVXActionNetworkLayerSource;
import net.onrc.openvirtex.messages.actions.VirtualizableAction;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openflow.protocol.OFMatch;
import org.openflow.protocol.OFPacketOut;
import org.openflow.protocol.Wildcards.Flag;
import org.openflow.protocol.action.OFAction;

public class OVXPacketOut extends OFPacketOut implements Devirtualizable {

    private final Logger         log   = LogManager
	                                       .getLogger(OVXPacketOut.class
	                                               .getName());
    private OFMatch              match = null;
    private final List<OFAction> acts  = new LinkedList<OFAction>();

    @Override
    public void devirtualize(final OVXSwitch sw) {

	final OVXPort inport = sw.getPort(this.getInPort());

	this.setInPort(inport.getPhysicalPortNumber());

	if (this.getBufferId() == -1) {
	    if (this.getPacketData().length <= 14) {
		// TODO: send error to controller
		this.log.error("PacketOut has no buffer or data {}; dropping",
		        this);
		return;
	    }
	    this.match = new OFMatch().loadFromPacket(this.packetData,
		    this.inPort);
	} else {
	    final OVXPacketIn cause = sw.getFromBufferMap(this.bufferId);
	    if (cause == null) {
		this.log.error(
		        "Unknown buffer id {} for virtual switch {}; dropping",
		        this.bufferId, sw);
		return;
	    }

	    this.match = new OFMatch().loadFromPacket(cause.getPacketData(),
		    this.inPort);
	    this.setBufferId(cause.getBufferId());
	}

	for (final OFAction act : this.getActions()) {
	    try {
		((VirtualizableAction) act).virtualize(sw);
		this.acts.add(act);
	    } catch (final ActionVirtualizationDenied e) {
		this.log.warn("Action {} could not be virtualized; error: {}",
		        act, e.getMessage());
		// TODO: send error to controller
		return;
	    }
	}

	this.prependRewriteActions(sw);
	this.setActions(this.acts);
	this.setActionsLength((short) 0);
	this.setLengthU(OVXPacketOut.MINIMUM_LENGTH + this.packetData.length);
	for (final OFAction act : this.acts) {
	    this.setLengthU(this.getLengthU() + act.getLengthU());
	    this.setActionsLength((short) (this.getActionsLength() + act
		    .getLength()));
	}
	// prependRewriteActions(sw);
	sw.sendSouth(this);

    }

    private void prependRewriteActions(final OVXSwitch sw) {
	final Mappable map = OVXMap.getInstance();

	if (!this.match.getWildcardObj().isWildcarded(Flag.NW_SRC)) {
	    final OVXIPAddress vip = new OVXIPAddress(sw.getTenantId(),
		    this.match.getNetworkSource());
	    PhysicalIPAddress pip = map.getPhysicalIP(vip, sw.getTenantId());
	    if (pip == null) {
		pip = new PhysicalIPAddress(map.getVirtualNetwork(
		        sw.getTenantId()).nextIP());
		this.log.debug(
		        "Adding IP mapping {} -> {} for tenant {} at switch {}",
		        vip, pip, sw.getTenantId(), sw.getName());
		map.addIP(pip, vip);
	    }
	    final OVXActionNetworkLayerSource srcAct = new OVXActionNetworkLayerSource();
	    srcAct.setNetworkAddress(pip.getIp());
	    this.acts.add(0, srcAct);

	}

	if (!this.match.getWildcardObj().isWildcarded(Flag.NW_DST)) {
	    final OVXIPAddress vip = new OVXIPAddress(sw.getTenantId(),
		    this.match.getNetworkDestination());
	    PhysicalIPAddress pip = map.getPhysicalIP(vip, sw.getTenantId());
	    if (pip == null) {
		pip = new PhysicalIPAddress(map.getVirtualNetwork(
		        sw.getTenantId()).nextIP());
		this.log.debug(
		        "Adding IP mapping {} -> {} for tenant {} at switch {}",
		        vip, pip, sw.getTenantId(), sw.getName());
		map.addIP(pip, vip);
	    }
	    final OVXActionNetworkLayerDestination dstAct = new OVXActionNetworkLayerDestination();
	    dstAct.setNetworkAddress(pip.getIp());
	    this.acts.add(0, dstAct);

	}
    }

}
