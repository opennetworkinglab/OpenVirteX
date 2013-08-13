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

package net.onrc.openvirtex.elements.link;

import net.onrc.openvirtex.elements.datapath.Switch;
import net.onrc.openvirtex.elements.port.Port;

/**
 * The Class Link.
 * 
 * @param <T1>
 *            the generic type (Port)
 * @param <T2>
 *            the generic type (Switch)
 */
public abstract class Link<T1, T2> {

	/** The source port. */
	protected T1 srcPort = null;

	/** The destination port. */
	protected T1 dstPort = null;

	/**
	 * Instantiates a new link.
	 */
	protected Link() {
	}

	/**
	 * Instantiates a new link.
	 * 
	 * @param srcPort
	 *            the source port instance
	 * @param dstPort
	 *            the destination port instance
	 */
	protected Link(final T1 srcPort, final T1 dstPort) {
		super();
		this.srcPort = srcPort;
		this.dstPort = dstPort;
	}

	/**
	 * Gets the source port instance.
	 * 
	 * @return the source port
	 */
	public T1 getSrcPort() {
		return this.srcPort;
	}

	/**
	 * Sets the source port instance.
	 * 
	 * @param srcPort
	 *            the new source port
	 */
	public void setSrcPort(final T1 srcPort) {
		this.srcPort = srcPort;
	}

	/**
	 * Gets the destination port instance.
	 * 
	 * @return the destination port
	 */
	public T1 getDstPort() {
		return this.dstPort;
	}

	/**
	 * Sets the destination port instance.
	 * 
	 * @param dstPort
	 *            the new destination port
	 */
	public void setDstPort(final T1 dstPort) {
		this.dstPort = dstPort;
	}
	
	@SuppressWarnings("unchecked")
        public T2 getSrcSwitch() {
	    return (T2) ((Port) this.srcPort).getParentSwitch();
	}

	@SuppressWarnings("unchecked")
        public T2 getDstSwitch() {
	    return (T2) ((Port) this.dstPort).getParentSwitch();
	}

	public String toString() {
	    String src = ((Switch) this.getSrcSwitch()).getSwitchId().toString(); 
	    String dst = ((Switch) this.getDstSwitch()).getSwitchId().toString();
	    return src + "<>" + dst;
	}
}
