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

package net.onrc.openvirtex.messages.actions;



import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.onrc.openvirtex.elements.address.OVXIPAddress;
import net.onrc.openvirtex.elements.datapath.OVXBigSwitch;
import net.onrc.openvirtex.elements.datapath.OVXSwitch;
import net.onrc.openvirtex.elements.datapath.PhysicalSwitch;
import net.onrc.openvirtex.elements.port.OVXPort;
import net.onrc.openvirtex.elements.port.PhysicalPort;
import net.onrc.openvirtex.exceptions.ActionVirtualizationDenied;
import net.onrc.openvirtex.exceptions.DroppedMessageException;
import net.onrc.openvirtex.messages.OVXFlowMod;
import net.onrc.openvirtex.messages.OVXPacketIn;
import net.onrc.openvirtex.messages.OVXPacketOut;
import net.onrc.openvirtex.packet.ARP;
import net.onrc.openvirtex.packet.Ethernet;
import net.onrc.openvirtex.routing.SwitchRoute;

import org.openflow.protocol.OFError.OFBadActionCode;
import org.openflow.protocol.OFFlowMod;
import org.openflow.protocol.OFMatch;
import org.openflow.protocol.OFPacketIn;
import org.openflow.protocol.OFPacketOut;
import org.openflow.protocol.OFPort;
import org.openflow.protocol.Wildcards;
import org.openflow.protocol.Wildcards.Flag;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionOutput;
import org.openflow.protocol.action.OFActionStripVirtualLan;
import org.openflow.protocol.action.OFActionVirtualLanIdentifier;
import org.openflow.util.U16;

public class OVXActionOutput extends OFActionOutput implements VirtualizableAction {

	@Override
	public void virtualize(OVXSwitch sw, List<OFAction> approvedActions, OFMatch match)
			throws ActionVirtualizationDenied, DroppedMessageException {

		int outport = U16.f(this.getPort());

		//Set the vlanId field if the packet is coming from a link port

		if (!sw.getPort(match.getInputPort()).isEdge() && match.getDataLayerType() != net.onrc.openvirtex.packet.Ethernet.TYPE_ARP)
			match.setDataLayerVirtualLan(sw.getPort(match.getInputPort()).getLinkId().shortValue());

		boolean exceptionThrown = false;

		/*
		 * If we have a flood or all action then expand the 
		 * action list to include all the ports on the virtual 
		 * switch (minus the inport if it's a flood)
		 */
		if (outport == U16.f(OFPort.OFPP_ALL.getValue()) || 
				outport == U16.f(OFPort.OFPP_FLOOD.getValue())) {
			Map<Short, OVXPort> ports = sw.getPorts();
			for (OVXPort port : ports.values()) {
				if (port.getPortNumber() != match.getInputPort()) {
					if (port.isEdge()) {
						if (sw instanceof OVXBigSwitch) {
							if (match.getDataLayerType() == Ethernet.TYPE_ARP) {
								PhysicalPort phyPort = port.getPhysicalPort();
								//phyPort.getParentSwitch().sendMsg(createARPPacketOut(match,(short) 2, phyPort.getPortNumber(), match.getNetworkProtocol()), null);
								OVXBigSwitch bigSwitch = (OVXBigSwitch) port.getParentSwitch();
								SwitchRoute route = bigSwitch.getRoute(sw.getPort(match.getInputPort()), port);
								PhysicalPort srcPort = route.getRoute().get(route.getRoute().size()-1).getDstPort();
								//maybe I can put CONTROLLER as input port for packetOut
								phyPort.getParentSwitch().sendMsg(createARPPacketOut(match, srcPort.getPortNumber(), phyPort.getPortNumber(), 
										match.getNetworkProtocol()), null);

								exceptionThrown = true;
							}
							else {
								OVXBigSwitch bigSwitch = (OVXBigSwitch) port.getParentSwitch();
								SwitchRoute route = bigSwitch.getRoute(sw.getPort(match.getInputPort()), port);
								PhysicalPort srcPort = route.getRoute().get(0).getSrcPort();
								approvedActions.add(new OFActionVirtualLanIdentifier((short) route.getRouteId()));
								this.setPort(srcPort.getPortNumber());
								approvedActions.add(new OFActionOutput(srcPort.getPortNumber()));

								//Generate the fm for the end point of the link
								PhysicalPort dstPort = route.getRoute().get(route.getRoute().size()-1).getDstPort();
								sendFlowMod(match.clone(), dstPort.getParentSwitch(), dstPort, port, (short) route.getRouteId(), bigSwitch.getTenantId());
							}
						} else {
							prependUnRewriteActions(approvedActions, match);
							//check if inPort is a vLink port. If yes, remove VLAN tag
							if (!sw.getPort(match.getInputPort()).isEdge())
								approvedActions.add(new OFActionStripVirtualLan());
							approvedActions.add(new OFActionOutput(port.getPhysicalPortNumber()));
						}
					} else if (port.getLinkId() != 0) {
						if (match.getDataLayerType() == Ethernet.TYPE_ARP) {	
							OVXPort dstPort = sw.getMap().getVirtualNetwork(sw.getTenantId()).getNeighborPort(port);
							dstPort.getParentSwitch().sendMsg(createARPPacket(match, dstPort.getPortNumber(), match.getNetworkProtocol()), null);
							//throw new DroppedMessageException();
						}
						else {
							if (sw instanceof OVXBigSwitch) {
								OVXBigSwitch bigSwitch = (OVXBigSwitch) port.getParentSwitch();
								SwitchRoute route = bigSwitch.getRoute(sw.getPort(match.getInputPort()), port);
								PhysicalPort srcPort = route.getRoute().get(0).getSrcPort();
								approvedActions.add(new OFActionVirtualLanIdentifier((short) route.getRouteId()));
								this.setPort(srcPort.getPortNumber());
								approvedActions.add(new OFActionOutput(srcPort.getPortNumber()));

								//Generate the fm for the end point of the link
								PhysicalPort dstPort = route.getRoute().get(route.getRoute().size()-1).getDstPort();
								sendFlowMod(match.clone(), dstPort.getParentSwitch(), dstPort, port, (short) route.getRouteId(), bigSwitch.getTenantId());
							}
							else {

								approvedActions.add(new OFActionVirtualLanIdentifier(port.getLinkId().shortValue()));
								if (sw.getPort(match.getInputPort()).getPhysicalPortNumber() != port.getPhysicalPortNumber())
									approvedActions.add(new OFActionOutput(port.getPhysicalPortNumber()));
								else
									approvedActions.add(new OFActionOutput(OFPort.OFPP_IN_PORT.getValue()));
							}
						}

					}

				}
			}

			if (outport == U16.f(OFPort.OFPP_ALL.getValue())) 
				approvedActions.add(new OFActionOutput(OFPort.OFPP_IN_PORT.getValue()));


		} else if (outport < U16.f(OFPort.OFPP_MAX.getValue())) {
			OVXPort ovxPort = sw.getPort(this.getPort());
			if (ovxPort != null) {
				if (ovxPort.isEdge()) {
					if (sw instanceof OVXBigSwitch) {
						if (match.getDataLayerType() == Ethernet.TYPE_ARP) {
							PhysicalPort phyPort = ovxPort.getPhysicalPort();
							//phyPort.getParentSwitch().sendMsg(createARPPacketOut(match,(short) 2, phyPort.getPortNumber(), match.getNetworkProtocol()), sw);
							OVXBigSwitch bigSwitch = (OVXBigSwitch) ovxPort.getParentSwitch();
							SwitchRoute route = bigSwitch.getRoute(sw.getPort(match.getInputPort()), ovxPort);
							PhysicalPort srcPort = route.getRoute().get(route.getRoute().size()-1).getDstPort();
							//maybe I can put CONTROLLER as input port for packetOut
							phyPort.getParentSwitch().sendMsg(createARPPacketOut(match, srcPort.getPortNumber(), phyPort.getPortNumber(), 
									match.getNetworkProtocol()), null);
							exceptionThrown = true;
						}
						else {
							OVXBigSwitch bigSwitch = (OVXBigSwitch) ovxPort.getParentSwitch();
							SwitchRoute route = bigSwitch.getRoute(sw.getPort(match.getInputPort()), ovxPort);
							PhysicalPort srcPort = route.getRoute().get(0).getSrcPort();
							approvedActions.add(new OFActionVirtualLanIdentifier((short) route.getRouteId()));
							this.setPort(srcPort.getPortNumber());
							approvedActions.add(new OFActionOutput(srcPort.getPortNumber()));

							//Generate the fm for the end point of the link
							PhysicalPort dstPort = route.getRoute().get(route.getRoute().size()-1).getDstPort();
							sendFlowMod(match.clone(), dstPort.getParentSwitch(), dstPort, ovxPort, (short) route.getRouteId(), bigSwitch.getTenantId());

						}
					}
					else {
						prependUnRewriteActions(approvedActions, match);
						//check if inPort is a vLink port. If yes, remove VLAN tag
						if (!sw.getPort(match.getInputPort()).isEdge())
							approvedActions.add(new OFActionStripVirtualLan());

						this.setPort(ovxPort.getPhysicalPortNumber());
						approvedActions.add(this);
					}
				}
				else if (ovxPort.getLinkId() != 0) {
					if (match.getDataLayerType() == Ethernet.TYPE_ARP) {
						OVXPort dstPort = sw.getMap().getVirtualNetwork(sw.getTenantId()).getNeighborPort(ovxPort);
						dstPort.getParentSwitch().sendMsg(createARPPacket(match, dstPort.getPortNumber(), match.getNetworkProtocol()), null);
						//throw new DroppedMessageException();
					}

					if (sw instanceof OVXBigSwitch) {
						OVXBigSwitch bigSwitch = (OVXBigSwitch) ovxPort.getParentSwitch();
						SwitchRoute route = bigSwitch.getRoute(sw.getPort(match.getInputPort()), ovxPort);
						PhysicalPort srcPort = route.getRoute().get(0).getSrcPort();
						approvedActions.add(new OFActionVirtualLanIdentifier((short) route.getRouteId()));
						this.setPort(srcPort.getPortNumber());
						approvedActions.add(new OFActionOutput(srcPort.getPortNumber()));

						//Generate the fm for the end point of the link
						PhysicalPort dstPort = route.getRoute().get(route.getRoute().size()-1).getDstPort();
						sendFlowMod(match.clone(), dstPort.getParentSwitch(), dstPort, ovxPort, (short) route.getRouteId(), bigSwitch.getTenantId());

					}
					else {
						approvedActions.add(new OFActionVirtualLanIdentifier(ovxPort.getLinkId().shortValue()));
						if (sw.getPort(match.getInputPort()).getPhysicalPortNumber() != ovxPort.getPhysicalPortNumber()) {
							this.setPort(ovxPort.getPhysicalPortNumber());
							approvedActions.add(this);
						}
						else {
							this.setPort(OFPort.OFPP_IN_PORT.getValue());
							approvedActions.add(new OFActionOutput(OFPort.OFPP_IN_PORT.getValue()));
						}
					}

				}
			} else
				throw new ActionVirtualizationDenied("Virtual Port " + this.getPort() + 
						" does not exist in virtual switch " + sw.getName(), OFBadActionCode.OFPBAC_BAD_OUT_PORT);
		} else
			approvedActions.add(this);

		if (exceptionThrown)
			throw new DroppedMessageException();

	}

    private void prependUnRewriteActions(List<OFAction> approvedActions, final OFMatch match) {
	if (!match.getWildcardObj().isWildcarded(Flag.NW_SRC)) {
	    final OVXActionNetworkLayerSource srcAct = new OVXActionNetworkLayerSource();
	    srcAct.setNetworkAddress(match.getNetworkSource());
	    approvedActions.add(srcAct);
	}
	if (!match.getWildcardObj().isWildcarded(Flag.NW_DST)) {
	    final OVXActionNetworkLayerDestination dstAct = new OVXActionNetworkLayerDestination();
	    dstAct.setNetworkAddress(match.getNetworkDestination());
	    approvedActions.add(dstAct);
	}
    }
    
    private OVXPacketIn createARPPacket(OFMatch match, short portNumber, short opCode) {
	ARP arp = new ARP();
	arp.setHardwareType(ARP.HW_TYPE_ETHERNET);
	arp.setProtocolType(ARP.PROTO_TYPE_IP);
	//TODO: Verify if this is always valid (if broadcast request, if unicast reply)
	arp.setOpCode(opCode);
	arp.setHardwareAddressLength((byte) 6);
	arp.setProtocolAddressLength((byte) 4);
	arp.setSenderHardwareAddress(match.getDataLayerSource());
	arp.setTargetHardwareAddress(match.getDataLayerDestination());
	arp.setSenderProtocolAddress(match.getNetworkSource());
	arp.setTargetProtocolAddress(match.getNetworkDestination());
	Ethernet eth = new Ethernet();
	eth.setEtherType(net.onrc.openvirtex.packet.Ethernet.TYPE_ARP);
	eth.setSourceMACAddress(match.getDataLayerSource());
	eth.setDestinationMACAddress(match.getDataLayerDestination());
	eth.setPayload(arp);
	//TODO: Implement getLenght for Ethernet
	OVXPacketIn msg = new OVXPacketIn();
	msg.setInPort(portNumber);
	msg.setBufferId(OFPacketOut.BUFFER_ID_NONE);
	msg.setReason(OFPacketIn.OFPacketInReason.NO_MATCH);
	msg.setTotalLength((short) (OFPacketIn.MINIMUM_LENGTH + eth.serialize().length));
	msg.setPacketData(eth.serialize());
	return msg;
    }

    
    private OVXPacketOut createARPPacketOut(OFMatch match, short inPort, short outPort, short opCode) {
	ARP arp = new ARP();
	arp.setHardwareType(ARP.HW_TYPE_ETHERNET);
	arp.setProtocolType(ARP.PROTO_TYPE_IP);
	//TODO: Verify if this is always valid (if broadcast request, if unicast reply)
	arp.setOpCode(opCode);
	arp.setHardwareAddressLength((byte) 6);
	arp.setProtocolAddressLength((byte) 4);
	arp.setSenderHardwareAddress(match.getDataLayerSource());
	arp.setTargetHardwareAddress(match.getDataLayerDestination());
	arp.setSenderProtocolAddress(match.getNetworkSource());
	arp.setTargetProtocolAddress(match.getNetworkDestination());
	Ethernet eth = new Ethernet();
	eth.setEtherType(net.onrc.openvirtex.packet.Ethernet.TYPE_ARP);
	eth.setSourceMACAddress(match.getDataLayerSource());
	eth.setDestinationMACAddress(match.getDataLayerDestination());
	eth.setPayload(arp);
	//TODO: Implement getLenght for Ethernet
	OVXPacketOut msg = new OVXPacketOut();
	msg.setInPort(inPort);
	msg.setBufferId(OFPacketOut.BUFFER_ID_NONE);
	OFActionOutput outAction = new OFActionOutput(outPort);
	ArrayList<OFAction> actions = new ArrayList<OFAction>();
	actions.add(outAction);
	msg.setActions(actions);
	msg.setActionsLength(outAction.getLength());
	msg.setPacketData(eth.serialize());
	msg.setLengthU((short) (OFPacketOut.MINIMUM_LENGTH + msg.getPacketData().length + OFActionOutput.MINIMUM_LENGTH));

	return msg;
    }
    
    private void sendFlowMod(OFMatch match, PhysicalSwitch sw, PhysicalPort inPort, OVXPort outPort, Short linkId, Integer tenantId) {
	OVXFlowMod fm = new OVXFlowMod();
	fm.setCommand(OFFlowMod.OFPFC_MODIFY);
	fm.setHardTimeout((short) 0);
	fm.setIdleTimeout((short) 5);
	fm.setBufferId(OFPacketOut.BUFFER_ID_NONE);
	fm.setOutPort(OFPort.OFPP_NONE.getValue());
	LinkedList<OFAction> actionList = new LinkedList<OFAction>();
	
	if (outPort.isEdge()) {
	    final OVXActionNetworkLayerSource srcAct = new OVXActionNetworkLayerSource();
	    srcAct.setNetworkAddress(match.getNetworkSource());
	    actionList.add(srcAct);
	    final OVXActionNetworkLayerDestination dstAct = new OVXActionNetworkLayerDestination();
	    dstAct.setNetworkAddress(match.getNetworkDestination());
	    actionList.add(dstAct);
	    OVXActionStripVirtualLan stripAction = new OVXActionStripVirtualLan();
	    actionList.add(stripAction);
	}
	else {
	    OFActionVirtualLanIdentifier vlanAction = new OFActionVirtualLanIdentifier(outPort.getLinkId().shortValue());
	    actionList.add(vlanAction);
	}
	    
	OFActionOutput outAction = new OFActionOutput(outPort.getPhysicalPortNumber());
	actionList.add(outAction);
	fm.setActions(actionList);
	Wildcards wild = match.getWildcardObj();
	wild = wild.matchOn(Flag.DL_SRC).matchOn(Flag.DL_DST).matchOn(Flag.DL_VLAN).matchOn(Flag.IN_PORT);
	match.setWildcards(wild.getInt());
	match.setInputPort(inPort.getPortNumber());
	match.setDataLayerVirtualLan(linkId);
	match.setNetworkSource(sw.getMap().getPhysicalIP(new OVXIPAddress(tenantId, match.getNetworkSource()), tenantId).getIp());
	match.setNetworkDestination(sw.getMap().getPhysicalIP(new OVXIPAddress(tenantId, match.getNetworkDestination()), tenantId).getIp());
	fm.setMatch(match);
	fm.setLengthU(OVXFlowMod.MINIMUM_LENGTH);
	for (OFAction act : actionList) {
	    fm.setLengthU(fm.getLengthU() + act.getLengthU());
	}
	sw.sendMsg(fm, sw);
	try {
	    Thread.sleep(5);
        } catch (InterruptedException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
        }
    }
    
}