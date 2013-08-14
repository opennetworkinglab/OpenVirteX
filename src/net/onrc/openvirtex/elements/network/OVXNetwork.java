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

package net.onrc.openvirtex.elements.network;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import net.onrc.openvirtex.core.io.OVXSendMsg;
import net.onrc.openvirtex.elements.OVXMap;
import net.onrc.openvirtex.elements.address.IPAddress;
import net.onrc.openvirtex.elements.datapath.OVXBigSwitch;
import net.onrc.openvirtex.elements.datapath.OVXSingleSwitch;
import net.onrc.openvirtex.elements.datapath.OVXSwitch;
import net.onrc.openvirtex.elements.datapath.PhysicalSwitch;
import net.onrc.openvirtex.elements.datapath.Switch;
import net.onrc.openvirtex.elements.link.OVXLink;
import net.onrc.openvirtex.elements.link.PhysicalLink;
import net.onrc.openvirtex.elements.port.OVXPort;
import net.onrc.openvirtex.elements.port.PhysicalPort;
import net.onrc.openvirtex.messages.OVXPacketIn;
import net.onrc.openvirtex.messages.OVXPacketOut;
import net.onrc.openvirtex.messages.lldp.LLDPUtil;
import net.onrc.openvirtex.util.MACAddress;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionOutput;

/**
 * Virtual networks contain tenantId, controller info, subnet and gateway
 * information. Handles registration of virtual switches and links. Responds to
 * LLDP discovery probes from the controller.
 * 
 */
public class OVXNetwork extends Network<OVXSwitch, OVXPort, OVXLink> {

    private final Integer                  tenantId;
    private final String                   protocol;
    private final String                   controllerHost;
    private final Integer                  controllerPort;
    private final IPAddress                network;
    private final short                    mask;
    private HashMap<IPAddress, MACAddress> gwsMap;
    private boolean                        bootState;
    private static AtomicInteger           tenantIdCounter = new AtomicInteger(
            1);
    private final AtomicLong		dpidCounter;
    private final AtomicInteger            linkCounter;
    // TODO: implement vlink flow pusher
    // public VLinkManager vLinkMgmt;

    Logger                                 log = LogManager
	                                               .getLogger(OVXNetwork.class
	                                                       .getName());

    public OVXNetwork(final String protocol,
	    final String controllerHost, final Integer controllerPort,
	    final IPAddress network, final short mask) {
	super();
	this.tenantId = tenantIdCounter.getAndIncrement();
	this.protocol = protocol;
	this.controllerHost = controllerHost;
	this.controllerPort = controllerPort;
	this.network = network;
	this.mask = mask;
	this.bootState = false;
	this.dpidCounter = new AtomicLong(0);
	// TODO: decide which value to start linkId's
	this.linkCounter = new AtomicInteger(2);
    }

    public String getProtocol() {
	return this.protocol;
    }

    public String getControllerHost() {
	return this.controllerHost;
    }

    public Integer getControllerPort() {
	return this.controllerPort;
    }

    public Integer getTenantId() {
	return this.tenantId;
    }

    public IPAddress getNetwork() {
	return this.network;
    }

    public MACAddress getGateway(final IPAddress ip) {
	return this.gwsMap.get(ip);
    }

    public short getMask() {
	return this.mask;
    }

    public void register() {
	OVXMap.getInstance().addNetwork(this);
    }

    public OVXSwitch createSwitch(final int tenantId, final List<Long> dpids) {
	OVXSwitch virtualSwitch;
	// TODO: generate ON.Lab dpid's
	final long switchId = this.dpidCounter.getAndIncrement();
	List<PhysicalSwitch> switches = new ArrayList<PhysicalSwitch>();
	// TODO: check if dpids are present in physical network
	for (long dpid: dpids) {
	    switches.add(PhysicalNetwork.getInstance().getSwitch(dpid));
	}
	if (dpids.size() == 1) {
	    virtualSwitch = new OVXSingleSwitch(switchId, tenantId, switches.get(0));
	} else {
	    virtualSwitch = new OVXBigSwitch(switchId, this.tenantId, switches);
	}
	this.addSwitch(virtualSwitch);
	virtualSwitch.register();
	return virtualSwitch;
    }

    /**
     * Create link and add it to the topology. Returns linkId when successful,
     * -1 if source port is already used.
     * 
     * @param srcPort
     * @param dstPort
     * @return
     */
    public synchronized OVXLink createLink(List<PhysicalLink> physicalLinks) {
	// Get virtual source port
	OVXPort srcPort = physicalLinks.get(0).getSrcPort().getOVXPort(this.tenantId);
	if (!this.neighbourPortMap.containsKey(srcPort)) {
	    int linkId = this.linkCounter.getAndIncrement();
	    final OVXLink link = new OVXLink(linkId, this.tenantId, physicalLinks);
	    super.addLink(link);
	    link.register();
	    return link;
	}
	return null;	
    }

    public OVXPort createHost(final long physicalDpid, final short portNumber, final MACAddress mac) {
	// TODO: check if dpid & port exist
	PhysicalSwitch physicalSwitch = PhysicalNetwork.getInstance().getSwitch(physicalDpid);
	OVXSwitch virtualSwitch = OVXMap.getInstance().getVirtualSwitch(physicalSwitch, this.tenantId);
	PhysicalPort physicalPort = physicalSwitch.getPort(portNumber);
	
	OVXPort edgePort = new OVXPort(this.tenantId, physicalPort, true);
	edgePort.register();
	OVXMap.getInstance().addMAC(mac,  this.tenantId);

	return edgePort;
    }

    // TODO
    public void createGateway(final IPAddress ip) {

    }

    // TOOD: should probably do some other stuff
    public boolean boot() {
	this.bootState = true;
	return this.bootState;
    }

    /**
     * Handle LLDP received from controller.
     * 
     * Receive LLDP from controller. Switch to which it is destined is passed in
     * by the ControllerHandler, port is extracted from the packet_out.
     * Packet_in is created based on topology info.
     */
    @Override
    public void handleLLDP(final OFMessage msg, final Switch sw) {
	final OVXPacketOut po = (OVXPacketOut) msg;
	final byte[] pkt = po.getPacketData();
	if (LLDPUtil.checkLLDP(pkt)) {
	    // Create LLDP response for each output action port
	    for (final OFAction action : po.getActions()) {
		try {
		    final short portNumber = ((OFActionOutput) action)
			    .getPort();
		    final OVXPort srcPort = (OVXPort) sw.getPort(portNumber);
		    final OVXPort dstPort = this.neighbourPortMap.get(srcPort);
		    final OVXPacketIn pi = new OVXPacketIn();
		    // Get input port from pkt_out
		    pi.setInPort(dstPort.getPortNumber());
		    pi.setPacketData(pkt);
		    dstPort.getParentSwitch().sendMsg(pi, this);
		} catch (final ClassCastException c) {
		    // ignore non-ActionOutput pkt_out's
		}
	    }
	} else {
	    this.log.debug("Invalid LLDP");
	}
    }

    @Override
    public void sendMsg(final OFMessage msg, final OVXSendMsg from) {
	// Do nothing
    }

    @Override
    public String getName() {
	return "Virtual network:" + this.tenantId.toString();
    }
}
