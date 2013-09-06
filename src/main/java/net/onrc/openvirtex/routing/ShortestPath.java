/**
 * 
 */
package net.onrc.openvirtex.routing;

import java.util.LinkedList;

import net.onrc.openvirtex.elements.datapath.OVXBigSwitch;
import net.onrc.openvirtex.elements.link.PhysicalLink;
import net.onrc.openvirtex.elements.port.OVXPort;

/**
 * @author gerola
 * 
 */
public class ShortestPath implements Routable {

	@Override
	public LinkedList<PhysicalLink> computePath(final OVXPort srcPort,
			final OVXPort dstPort) {
		return null;
	}

	@Override
        public SwitchRoute getRoute(OVXBigSwitch vSwitch,
                OVXPort srcPort, OVXPort dstPort) {
	    return null;
        }
	
	public String getName() {
		return "shortest path";
	}
}
