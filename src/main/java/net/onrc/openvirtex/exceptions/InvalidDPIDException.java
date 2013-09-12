package net.onrc.openvirtex.exceptions;

/**
 * The physical dpid specified by the admin is not a valid dpid.
 */
public class InvalidDPIDException extends IllegalArgumentException {

    private static final long serialVersionUID = 6957434977838246116L;

    public InvalidDPIDException() {
	super();
    }

    public InvalidDPIDException(String msg) {
	super(msg);
    }

    public InvalidDPIDException(Throwable msg) {
	super(msg);
    }
}
