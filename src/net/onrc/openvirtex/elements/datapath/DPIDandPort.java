package net.onrc.openvirtex.elements.datapath;

public class DPIDandPort {
    long  dpid;
    short port;

    public DPIDandPort(final long dpid, final short port) {
	super();
	this.dpid = dpid;
	this.port = port;
    }

    /**
     * @return the dpid
     */
    public long getDpid() {
	return this.dpid;
    }

    /**
     * @param dpid
     *            the dpid to set
     */
    public void setDpid(final long dpid) {
	this.dpid = dpid;
    }

    /**
     * @return the port
     */
    public short getPort() {
	return this.port;
    }

    /**
     * @param port
     *            the port to set
     */
    public void setPort(final short port) {
	this.port = port;
    }
}
