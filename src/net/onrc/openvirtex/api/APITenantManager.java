package net.onrc.openvirtex.api;

import java.util.ArrayList;
import java.util.List;

import net.onrc.openvirtex.elements.OVXMap;
import net.onrc.openvirtex.elements.datapath.OVXBigSwitch;
import net.onrc.openvirtex.elements.datapath.OVXSingleSwitch;
import net.onrc.openvirtex.elements.datapath.OVXSwitch;
import net.onrc.openvirtex.elements.network.OVXNetwork;

public class APITenantManager {

    /**
     * createOVXNetwork will create a new OVXNetwork object and initialize the
     * network with all the values specified like the tenantId, protocol,
     * controllerIP, networkIP, mask
     * 
     * @param tenantId
     *            The tenantId that the new virtualNetwork should be assigned
     * @param protocol
     * @param controllerAddress
     *            The IPAddress for the controller which controls this
     *            virtualNetowkr
     * @param controllerPort
     *            The Port which the controller and OVX will communicate on
     * @param networkAddress
     *            The IP address that OVX runs in
     * @param mask
     *            The IP range is defined using the mask
     * 
     * @return success
     */
    public Integer createOVXNetwork(final String protocol,
	    final String controllerAddress, final int controllerPort,
	    final String networkAddress, final short mask) {
	final OVXNetwork virtualNetwork = new OVXNetwork(controllerAddress,
	        controllerPort);
	// virtualNetwork.register();
	return virtualNetwork.getTenantId();
    }

    /**
     * createOVXSwitch create a new switch object given a set of
     * physical dpid. This switch object will either be an OVXSwitch or
     * a OVXBigSwitch.
     * 
     * @param tenantId
     *            The tenantId will specify which virtual network the switch
     *            belongs to
     * @param dpids
     *            The list of physicalSwitch dpids to specify what the
     *            virtualSwitch is composed of
     * @return dpid Return the DPID of the virtualSwitch which we have just
     *         created
     */
    public long createOVXSwitch(final int tenantId, final List<String> dpids) {
	final OVXMap map = OVXMap.getInstance();
	final OVXNetwork virtualNetwork = map.getVirtualNetwork(tenantId);
	OVXSwitch virtualSwitch;
	if (dpids.size() == 1) {
	    // TODO: change OXVSwitch constructor to take a physicalDPID and
	    // generate the next available VirtualDPID from OVXNetwork
	    virtualSwitch = new OVXSingleSwitch(Long.parseLong(dpids.get(0)),
		    tenantId);
	} else {
	    final List<Long> longDpids = new ArrayList<Long>();
	    for (final String dpid : dpids) {
		final long longDpid = Long.parseLong(dpid);
		longDpids.add(longDpid);
	    }
	    virtualSwitch = new OVXBigSwitch();
	}
	return virtualNetwork.addSwitch(dpids);
	// virtualNetwork.registerSwitch(virtualSwitch);
	// return virtualSwitch.getSwitchId();
    }

    /**
     * To add a Host we have to create an edgePort which the host can connect
     * to.
     * So we create a new Port object and set the edge attribute to be True.
     * 
     * @param tenantId
     *            The tenantId is the integer to specify which virtualNetwork
     *            this host should be added to
     * @param dpid
     *            specify the virtual dpid for which switch to attach the host
     *            to
     * @param port
     *            Specify which port on the virtualSwitch the host should be
     *            connected to
     * @return portNumber The portNumber is a short that represents the port of
     *         the edge switch which this edgePort is using
     */
    public short createEdgePort(final int tenantId, final String mac) {
	final OVXMap map = OVXMap.getInstance();
	final OVXNetwork virtualNetwork = map.getVirtualNetwork(tenantId);
	final short edgePort = virtualNetwork.addHost(mac);
	/*
	 * OVXSwitch virtualSwitch = virtualNetwork.getSwitch(longDpid);
	 * OVXPort edgePort = new OVXPort();
	 * edgePort.setIsEdge(true);
	 * virtualSwitch.addPort(edgePort);
	 * virtualSwitch.registerPort(edgePort.getPortNumber(),
	 * edgePort.getParentSwitch().getSwitchId(),
	 * edgePort.getPhysicalPort().getPortNumber());
	 */
	return edgePort;
    }

    /**
     * Takes a path of physicalLinks in a string and creates the virtualLink
     * based on this data. Each virtualLink consists of a set of PhysicalLinks
     * that are all continuous in the PHysicalNetwork topology.
     * 
     * @param tenantId
     *            Specify which virtualNetwork that the link is being created in
     * @param pathString
     *            The list of physicalLinks that make up the virtualLink
     * @return virtualLink the OVXLink object that is created using the
     *         PhysicalLinks
     */
    public Integer createOVXLink(final int tenantId, final String pathString) {
	// TODO: actual link objects passed in
	final OVXMap map = OVXMap.getInstance();
	final OVXNetwork virtualNetwork = map.getVirtualNetwork(tenantId);
	return virtualNetwork.addLink(pathString);
	// virtualLink needs to have a constructor that takes a set of
	// physicalLinks
	/*
	 * OVXPort sourcePort = new OVXPort();
	 * OVXPort destPort = new OVXPort();
	 * OVXLink virtualLink = new OVXLink (sourcePort, destPort, tenantId);
	 * virtualNetwork.registerLink(virtualLink);
	 * return virtualLink.getLinkId();
	 */
    }

    /**
     * Creates and starts the network which is specified by the given
     * tenant id.
     * 
     * @param tenantId
     *            A unique Integer which identifies each virtual network
     */
    public boolean bootNetwork(final int tenantId) {
	// initialize the virtualNetwork using the given tenantId
	final OVXMap map = OVXMap.getInstance();
	final OVXNetwork virtualNetwork = map.getVirtualNetwork(tenantId);
	// adds the virtualNetwork to the OVXMap
	virtualNetwork.initialize();
	return true;
    }
}
