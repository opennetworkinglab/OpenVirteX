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

package net.onrc.openvirtex.elements;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.onrc.openvirtex.elements.address.OVXIPAddress;
import net.onrc.openvirtex.elements.address.PhysicalIPAddress;
import net.onrc.openvirtex.elements.datapath.OVXSwitch;
import net.onrc.openvirtex.elements.datapath.PhysicalSwitch;
import net.onrc.openvirtex.elements.link.OVXLink;
import net.onrc.openvirtex.elements.link.PhysicalLink;
import net.onrc.openvirtex.elements.network.OVXNetwork;
import net.onrc.openvirtex.elements.port.OVXPort;
import net.onrc.openvirtex.util.MACAddress;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.googlecode.concurrenttrees.radix.ConcurrentRadixTree;
import com.googlecode.concurrenttrees.radix.RadixTree;
import com.googlecode.concurrenttrees.radix.node.concrete.DefaultCharArrayNodeFactory;

public class OVXMap implements Mappable {

    Logger                                                                   log = LogManager
	                                                                                 .getLogger(OVXMap.class
	                                                                                         .getName());
    private static OVXMap                                                    mapInstance;

    ConcurrentHashMap<OVXSwitch, ArrayList<PhysicalSwitch>>                  virtualSwitchMap;
    ConcurrentHashMap<PhysicalSwitch, ConcurrentHashMap<Integer, OVXSwitch>> physicalSwitchMap;
    ConcurrentHashMap<OVXLink, ArrayList<PhysicalLink>>                      virtualLinkMap;
    ConcurrentHashMap<PhysicalLink, ConcurrentHashMap<Integer, OVXLink>>     physicalLinkMap;
    ConcurrentHashMap<Integer, OVXNetwork>                                   networkMap;
    RadixTree<OVXIPAddress>                                                  physicalIPMap;

    RadixTree<ConcurrentHashMap<Integer, PhysicalIPAddress>>                 virtualIPMap;
    RadixTree<Integer>                                                       macMap;

    /**
     * constructor for OVXMap will be an empty constructor
     */
    private OVXMap() {
	this.virtualSwitchMap = new ConcurrentHashMap<OVXSwitch, ArrayList<PhysicalSwitch>>();
	this.physicalSwitchMap = new ConcurrentHashMap<PhysicalSwitch, ConcurrentHashMap<Integer, OVXSwitch>>();
	this.virtualLinkMap = new ConcurrentHashMap<OVXLink, ArrayList<PhysicalLink>>();
	this.physicalLinkMap = new ConcurrentHashMap<PhysicalLink, ConcurrentHashMap<Integer, OVXLink>>();
	this.networkMap = new ConcurrentHashMap<Integer, OVXNetwork>();
	this.physicalIPMap = new ConcurrentRadixTree<OVXIPAddress>(
	        new DefaultCharArrayNodeFactory());
	this.virtualIPMap = new ConcurrentRadixTree<ConcurrentHashMap<Integer, PhysicalIPAddress>>(
	        new DefaultCharArrayNodeFactory());
	this.macMap = new ConcurrentRadixTree<Integer>(
	        new DefaultCharArrayNodeFactory());
    }

    /**
     * getInstance will get the instance of the class and if this already exists
     * then the existing object will be returned. This is a singleton class.
     * 
     * @return mapInstance Return the OVXMap object instance
     */
    public static OVXMap getInstance() {
	if (OVXMap.mapInstance == null) {
	    OVXMap.mapInstance = new OVXMap();
	}
	return OVXMap.mapInstance;
    }

    // ADD objects to dictionary

    /**
     * Create the mapping between PhysicalSwithes and a VirtualSwitch. This
     * function takes
     * in a list of physicalSwitches and adds to the OVXMap indexed by the
     * virtualSwitch.
     * 
     * @param physicalSwitches
     * @param virtualSwitch
     */
    @Override
    public void addSwitches(final List<PhysicalSwitch> physicalSwitches,
	    final OVXSwitch virtualSwitch) {

	for (final PhysicalSwitch physicalSwitch : physicalSwitches) {
	    this.addSwitch(physicalSwitch, virtualSwitch);
	}
    }

    /**
     * create the mapping between OVX switch to physical switch and from
     * physical switch to virtual switch
     * 
     * @param physicalSwitch
     *            Refers to the PhysicalSwitch from the PhysicalNetwork
     * @param virtualSwitch
     *            Has type OVXSwitch and this switch is specific to a tenantId
     * 
     */
    private void addSwitch(final PhysicalSwitch physicalSwitch,
	    final OVXSwitch virtualSwitch) {
	this.addPhysicalSwitch(physicalSwitch, virtualSwitch);
	this.addVirtualSwitch(virtualSwitch, physicalSwitch);
    }

    /**
     * Create the mapping between PhysicalLinks and a VirtualLink. This function
     * takes in a
     * list of physicalLinks rather than an individual physicalLink and adds the
     * list
     * to the OVXmap.
     * 
     * @param physicalLinks
     * @param virtualLink
     */
    @Override
    public void addLinks(final List<PhysicalLink> physicalLinks,
	    final OVXLink virtualLink) {
	for (final PhysicalLink physicalLink : physicalLinks) {
	    this.addLink(physicalLink, virtualLink);
	}
    }

    /**
     * create the mapping between the virtual link to physical link and physical
     * link to virtual link
     * 
     * @param physicalLink
     *            Refers to the PhysicalLink in the PhysicalNetwork
     * @param virtualLink
     *            Refers to the OVXLink which consists of many PhysicalLinks and
     *            specific to a tenantId
     * 
     */
    private void addLink(final PhysicalLink physicalLink,
	    final OVXLink virtualLink) {
	this.addPhysicalLink(physicalLink, virtualLink);
	this.addVirtualLink(virtualLink, physicalLink);
    }

    /**
     * This is the generic function which takes as arguments the
     * PhysicalIPAddress
     * and the OVXIPAddress. This will add the value into both the physical
     * to virtual map and in the other direction.
     * 
     * @param physicalIP
     *            Refers to the PhysicalIPAddress which is created using the
     *            tenant id and virtualIP
     * @param virtualIP
     *            The IP address used within the VirtualNetwork
     */
    @Override
    public void addIP(final PhysicalIPAddress physicalIP,
	    final OVXIPAddress virtualIP) {
	this.addPhysicalIP(physicalIP, virtualIP);
	this.addVirtualIP(virtualIP, physicalIP);
    }

    /**
     * This function will create a map indexed on the key PhysicalIPAddress with
     * value OVXIPAddress.
     * 
     * @param physicalIP
     *            refers to the PhysicalIPAddress which is created using the
     *            tenant id and virtualIP
     * @param virtualIP
     *            the IP address used within the VirtualNetwork
     */
    private void addPhysicalIP(final PhysicalIPAddress physicalIP,
	    final OVXIPAddress virtualIP) {
	this.physicalIPMap.put(physicalIP.toString(), virtualIP);
    }

    /**
     * This function will create a map indexed on the key OVXIPAddress with
     * value
     * a ConcurrentHashMap mapping the tenant id to the PhysicalIPAddress
     * 
     * @param virtualIP
     *            the IP address used within the VirtualNetwork
     * @param physicalIP
     *            refers to the PhysicalIPAddress which is created using the
     *            tenant id and virtualIP
     */
    private void addVirtualIP(final OVXIPAddress virtualIP,
	    final PhysicalIPAddress physicalIP) {
	ConcurrentHashMap<Integer, PhysicalIPAddress> ipMap = this.virtualIPMap
	        .getValueForExactKey(virtualIP.toString());
	if (ipMap == null) {
	    ipMap = new ConcurrentHashMap<Integer, PhysicalIPAddress>();
	    this.virtualIPMap.put(virtualIP.toString(), ipMap);
	}
	ipMap.put(virtualIP.getTenantId(), physicalIP);
    }

    /**
     * This function sets up the mapping from the physicalSwitch to the
     * tenant id and virtualSwitch which has been specified
     * 
     * @param physicalSwitch
     *            A PhysicalSwitch object which is found in the PhysicalNetwork
     * @param virtualSwitch
     *            An OVXSwitch object which is found in the OVXNetwork
     * 
     */
    private void addPhysicalSwitch(final PhysicalSwitch physicalSwitch,
	    final OVXSwitch virtualSwitch) {
	System.out.println("physicalSwitchMap:" + this.physicalSwitchMap);
	ConcurrentHashMap<Integer, OVXSwitch> switchMap = this.physicalSwitchMap
	        .get(physicalSwitch);
	if (switchMap == null) {
	    switchMap = new ConcurrentHashMap<Integer, OVXSwitch>();
	    this.physicalSwitchMap.put(physicalSwitch, switchMap);
	}
	switchMap.put(virtualSwitch.getTenantId(), virtualSwitch);
    }

    /**
     * sets up the mapping from the physical link to the OVXLinks which
     * contain the given physical link
     * 
     * @param physicalLink
     *            A Link consisting of the PhysicalPort and PhysicalSwitch
     *            objects for both the source and destinations
     * @param virtualLink
     *            OVXLink contains the OVXPort and OVXSwitch for source and
     *            destination in the OVXNetwork
     */
    private void addPhysicalLink(final PhysicalLink physicalLink,
	    final OVXLink virtualLink) {
	ConcurrentHashMap<Integer, OVXLink> linkMap = this.physicalLinkMap
	        .get(physicalLink);
	if (linkMap == null) {
	    linkMap = new ConcurrentHashMap<Integer, OVXLink>();
	    this.physicalLinkMap.put(physicalLink, linkMap);
	}
	linkMap.put(virtualLink.getTenantId(), virtualLink);
    }

    /**
     * sets up the mapping from the OVXSwitch to the physicalSwitch
     * which has been specified
     * 
     * @param virtualSwitch
     *            A OVXSwitch object which represents a single switch in the
     *            OVXNetwork
     * @param physicalSwitch
     *            A PhysicalSwitch object is a single switch in the
     *            PhysicalNetwork
     * 
     */
    private void addVirtualSwitch(final OVXSwitch virtualSwitch,
	    final PhysicalSwitch physicalSwitch) {
	ArrayList<PhysicalSwitch> switchList = this.virtualSwitchMap
	        .get(virtualSwitch);
	if (switchList == null) {
	    switchList = new ArrayList<PhysicalSwitch>();
	    this.virtualSwitchMap.put(virtualSwitch, switchList);
	}
	switchList.add(physicalSwitch);
    }

    /**
     * maps the virtual link to the physical links that it contains
     * 
     * @param virtualLink
     *            A OVXLink object which represents a single link in the
     *            OVXNetwork
     * @param physicalLink
     *            A PhysicalLink object which represent a single source and
     *            destination PhysicalPort and PhysicalSwitch
     */
    private void addVirtualLink(final OVXLink virtualLink,
	    final PhysicalLink physicalLink) {
	ArrayList<PhysicalLink> linkList = this.virtualLinkMap.get(virtualLink);
	if (linkList == null) {
	    linkList = new ArrayList<PhysicalLink>();
	    this.virtualLinkMap.put(virtualLink, linkList);
	}
	linkList.add(physicalLink);
    }

    /**
     * Maintain a list of all the virtualNetworks in the system
     * indexed by the tenant id mapping to OVXNetworks
     * 
     * @param virtualNetwork
     *            An OVXNetwork object which keeps track of all the elements in
     *            the Virtual network
     * 
     */
    @Override
    public void addNetwork(final OVXNetwork virtualNetwork) {
	this.networkMap.put(virtualNetwork.getTenantId(), virtualNetwork);
    }

    @Override
    public void addMAC(final MACAddress mac, final Integer tenantId) {
	this.macMap.put(mac.toStringNoColon(), tenantId);
    }

    // Access objects from dictionary given the key

    @Override
    public PhysicalIPAddress getPhysicalIP(final OVXIPAddress ip,
	    final Integer tenantId) {
	final ConcurrentHashMap<Integer, PhysicalIPAddress> ips = this.virtualIPMap
	        .getValueForExactKey(ip.toString());
	if (ips == null) {
	    return null;
	}
	return ips.get(tenantId);
    }

    @Override
    public OVXIPAddress getVirtualIP(final PhysicalIPAddress ip) {
	return this.physicalIPMap.getValueForExactKey(ip.toString());
    }

    /**
     * get the OVXSwitch which has been specified by the physicalSwitch
     * and tenantId
     * 
     * @param physicalSwitch
     *            A PhysicalSwitch object is a single switch in the
     *            PhysicalNetwork
     * 
     * @return virtualSwitch A OVXSwitch object which represents a single switch
     *         in the OVXNetwork
     */
    @Override
    public OVXSwitch getVirtualSwitch(final PhysicalSwitch physicalSwitch,
	    final Integer tenantId) {
	final ConcurrentHashMap<Integer, OVXSwitch> sws = this.physicalSwitchMap
	        .get(physicalSwitch);
	if (sws == null) {
	    this.log.error("No virtual switches for physical switch {}",
		    physicalSwitch);
	}
	this.log.info("looking for tid {} in {}", tenantId, sws);
	return this.physicalSwitchMap.get(physicalSwitch).get(tenantId);
    }

    /**
     * get the OVXLink which has been specified by the physicalLink and
     * the tenantId. This function will return a list of OVXLinks all of
     * which contain the specified physicalLink in the tenantId.
     * 
     * @param physicalLink
     *            A PhysicalLink object which represent a single source and
     *            destination PhysicalPort and PhysicalSwitch
     * 
     * @return virtualLink A OVXLink object which represents a single link in
     *         the OVXNetwork
     */
    @Override
    public OVXLink getVirtualLink(final PhysicalLink physicalLink,
	    final Integer tenantId) {
	return this.physicalLinkMap.get(physicalLink).get(tenantId);
    }

    /**
     * get the physicalLinks that all make up a specified OVXLink.
     * Return a list of all the physicalLinks that make up the OVXLink
     * 
     * @param virtualLink
     *            An OVXLink object which represents a single link in the
     *            OVXNetwork
     * 
     * @return physicalLinks A List of PhysicalLink objects which represent a
     *         single source and destination PhysicalPort and PhysicalSwitch
     */
    @Override
    public List<PhysicalLink> getPhysicalLinks(final OVXLink virtualLink) {

	return this.virtualLinkMap.get(virtualLink);
    }

    /**
     * get the physicalSwitches that are contained in the OVXSwitch. for
     * a big switch this will be multiple physicalSwitches
     * 
     * @param virtualSwitch
     *            A OVXSwitch object representing a single switch in the virtual
     *            network
     * 
     * @return physicalSwitches A List of PhysicalSwitch objects that are each
     *         part of the OVXSwitch specified
     */
    @Override
    public List<PhysicalSwitch> getPhysicalSwitches(
	    final OVXSwitch virtualSwitch) {
	return this.virtualSwitchMap.get(virtualSwitch);
    }

    /**
     * use the tenantId to return the OVXNetwork object
     * 
     * @param tenantId
     *            This is an Integer that represents a unique number for each
     *            virtualNetwork
     * 
     * @return virtualNetwork A OVXNetwork object that represents all the
     *         information related to a virtual network
     */
    @Override
    public OVXNetwork getVirtualNetwork(final Integer tenantId) {
	return this.networkMap.get(tenantId);
    }

    @Override
    public Integer getMAC(final MACAddress mac) {
	return this.macMap.getValueForExactKey(mac.toStringNoColon());
    }

    // Remove objects from dictionary

    public Collection<OVXNetwork> getVirtualNetworks() {
	final Collection<OVXNetwork> networks = this.networkMap.values();
	return networks;
    }

    public Collection<OVXSwitch> getVirtualSwitches() {
	final Collection<OVXSwitch> switches = this.virtualSwitchMap.keySet();
	return switches;
    }

    public Set<OVXLink> getVirtualLinks() {
	final Collection<OVXLink> links = this.virtualLinkMap.keySet();
	System.out.println("VIRTUALLINKS:" + links);
	final Set<OVXLink> linkSet = new HashSet<OVXLink>();
	final HashMap<Integer, ArrayList<Integer>> tMap = new HashMap<Integer, ArrayList<Integer>>();
	for (final OVXLink link : links) {
	    System.out.println("LINK:" + link.getLinkId());
	    System.out.println("TENANT:" + link.getTenantId());
	    ArrayList<Integer> tList = new ArrayList<Integer>();
	    tList = tMap.get(link.getTenantId());
	    if (tList != null) {
		if (tList.contains(link.getLinkId())) {
		    continue;
		} else {
		    linkSet.add(link);
		    tList.add(link.getLinkId());
		    tMap.put(link.getTenantId(), tList);
		}
	    } else {
		tList = new ArrayList<Integer>();
		linkSet.add(link);
		tList.add(link.getLinkId());
		tMap.put(link.getTenantId(), tList);
	    }

	    System.out.println(tMap.toString());
	}
	System.out.println("linkSet: " + linkSet.toString());
	return linkSet;
    }

    @Override
    public HashMap<String, Object> toJson() {
	final HashMap<String, Object> output = new HashMap<String, Object>();
	final LinkedList<Object> list = new LinkedList<Object>();

	final Collection<Integer> tenants = this.getTenantIds();
	for (final Integer tenant : tenants) {
	    final HashMap<String, Object> ovxMap = new HashMap<String, Object>();
	    final ArrayList<HashMap<String, Object>> switchList = new ArrayList<HashMap<String, Object>>();

	    final ArrayList<HashMap<String, Object>> linkList = new ArrayList<HashMap<String, Object>>();

	    ovxMap.put(Mappable.TID, tenant);

	    final ArrayList<OVXSwitch> virSw = this
		    .getVirtualSwitchesPerTenant(tenant);
	    for (final OVXSwitch vSwitch : virSw) {
		final HashMap<String, Object> switchMap = new HashMap<String, Object>();
		switchMap.put(Mappable.VSWID,
		        String.valueOf(vSwitch.getSwitchId()));
		final ArrayList<PhysicalSwitch> phySwitches = this.virtualSwitchMap
		        .get(vSwitch);
		final ArrayList<Long> phySwitchIds = new ArrayList<Long>();
		for (final PhysicalSwitch phySwitch : phySwitches) {
		    phySwitchIds.add(phySwitch.getSwitchId());
		}
		switchMap.put(Mappable.PSWID, phySwitchIds);
		final Collection<OVXPort> virPorts = vSwitch.getPorts();
		final ArrayList<HashMap<String, Object>> portList = new ArrayList<HashMap<String, Object>>();
		for (final OVXPort vPort : virPorts) {
		    final HashMap<String, Object> portMap = new HashMap<String, Object>();
		    portMap.put(Mappable.VPORTNUM, vPort.getPortNumber());
		    HashMap<String, Object> phyPortMap = new HashMap<String, Object>();
		    phyPortMap = vPort.getPhysicalPort().toJson();
		    portMap.put(Mappable.PHYPORT, phyPortMap);
		    portList.add(portMap);
		}
		switchMap.put(Mappable.PORT, portList);
		switchList.add(switchMap);
	    }
	    ovxMap.put(Mappable.SWMAP, switchList);

	    final ArrayList<OVXLink> virLinks = this
		    .getVirtualLinksPerTenant(tenant);
	    for (final OVXLink vLink : virLinks) {
		final HashMap<String, Object> linkMap = new HashMap<String, Object>();
		linkMap.put(Mappable.LINKID, String.valueOf(vLink.getLinkId()));
		final ArrayList<HashMap<String, Object>> pLinks = new ArrayList<HashMap<String, Object>>();
		final ArrayList<PhysicalLink> phyLinks = this.virtualLinkMap
		        .get(vLink);
		for (final PhysicalLink phyLink : phyLinks) {
		    final HashMap<String, Object> phyLinkMap = phyLink.toJson();
		    pLinks.add(phyLinkMap);

		}
		linkMap.put(Mappable.PHYLINK, pLinks);
		linkList.add(linkMap);
	    }
	    ovxMap.put(Mappable.LINKMAP, linkList);
	    list.add(ovxMap);
	}

	output.put(Mappable.MAP, list);
	return output;
    }

    private ArrayList<OVXLink> getVirtualLinksPerTenant(final Integer tenant) {
	final ArrayList<OVXLink> vLinks = new ArrayList<OVXLink>();
	final Collection<ConcurrentHashMap<Integer, OVXLink>> virLinkMap = this.physicalLinkMap
	        .values();
	for (final ConcurrentHashMap<Integer, OVXLink> vLink : virLinkMap) {
	    if (vLink.containsKey(tenant)) {
		vLinks.add(vLink.get(tenant));
	    }
	}
	return vLinks;
    }

    private ArrayList<OVXSwitch> getVirtualSwitchesPerTenant(
	    final Integer tenant) {
	final ArrayList<OVXSwitch> vSwitches = new ArrayList<OVXSwitch>();
	final Collection<ConcurrentHashMap<Integer, OVXSwitch>> virSwitchMap = this.physicalSwitchMap
	        .values();
	System.out.println("virSwitchMap:" + virSwitchMap);
	for (final ConcurrentHashMap<Integer, OVXSwitch> vSwitch : virSwitchMap) {
	    System.out.println("vSwitch:" + vSwitch);
	    if (vSwitch.containsKey(tenant)
		    && vSwitches.contains(vSwitch.get(tenant)) == false) {
		vSwitches.add(vSwitch.get(tenant));
	    }
	}
	System.out.println("vSwitches:" + vSwitches);
	return vSwitches;
    }

    private Collection<Integer> getTenantIds() {
	final Collection<Integer> tenants = this.networkMap.keySet();
	return tenants;
    }
}
