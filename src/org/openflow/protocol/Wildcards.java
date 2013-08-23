/**
 * Copyright 2013, Big Switch Networks, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 **/

package org.openflow.protocol;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;

import com.google.common.base.Joiner;

/**
 * a more user friendly representation of the wildcards bits in an OpenFlow
 * match. The Wildcards object is
 * <ul>
 * <li>immutable (i.e., threadsafe)</li>
 * <li>instance managed (don't instantiate it yourself), instead call "of"</li>
 * <ul>
 * <p>
 * You can construct a Wildcard object from either its integer representation
 * </p>
 * <code>
 *    Wildcard.of(0x3820e0);
 *  </code>
 * <p>
 * Or start with either an empty or full wildcard, and select/unselect foo.
 * </p>
 * <code>
 *  Wildcard w = Wildcards.NONE
 *                .set(Flag.DL_SRC, Flag. DL_DST, Flag.DL_VLAN_PCP)
 *                .setNwDstMask(8)
 *                .setNwSrcMask(8);
 *  </code>
 * <p>
 * <b>Remember:</b> Wildcards objects are immutable. set... operations have
 * <b>NO EFFECT</b> on the current wildcard object. You HAVE to use the returned
 * changed object.
 * </p>
 * 
 * @author Andreas Wundsam <andreas.wundsam@bigswitch.com>
 */
public class Wildcards {

    public final static Wildcards FULL                = new Wildcards(
	                                                      OFMatch.OFPFW_ALL_SANITIZED);
    private static final int      FULL_INT            = Wildcards.FULL.getInt();

    public final static Wildcards EXACT               = new Wildcards(0);

    // floodlight common case: matches on inport + l2
    public final static int       INT_INPORT_L2_MATCH = 0x3820e0;
    public final static Wildcards INPORT_L2_MATCH     = new Wildcards(
	                                                      Wildcards.INT_INPORT_L2_MATCH);

    /**
     * enum type for the binary flags that can be set in the wildcards field of
     * an OFMatch. Replaces the unwieldy c-ish int constants in OFMatch.
     */
    public static enum Flag {
	IN_PORT(OFMatch.OFPFW_IN_PORT), /* Switch input port. */
	DL_VLAN(OFMatch.OFPFW_DL_VLAN), /* VLAN id. */
	DL_SRC(OFMatch.OFPFW_DL_SRC), /* Ethernet source address. */
	DL_DST(OFMatch.OFPFW_DL_DST), /* Ethernet destination addr */
	DL_TYPE(OFMatch.OFPFW_DL_TYPE), /* Ethernet frame type. */
	NW_PROTO(OFMatch.OFPFW_NW_PROTO), /* IP protocol. */
	TP_SRC(OFMatch.OFPFW_TP_SRC), /* TCP/UDP source port. */
	TP_DST(OFMatch.OFPFW_TP_DST), /* TCP/UDP destination port. */
	DL_VLAN_PCP(OFMatch.OFPFW_DL_VLAN_PCP), /* VLAN priority. */
	NW_SRC(-1) { /*
	              * virtual NW_SRC flag => translates to the strange 6 bits
	              * in the header
	              */
	    @Override
	    boolean isBolean() {
		return false;
	    }

	    @Override
	    int getInt(final int flags) {
		return (flags & OFMatch.OFPFW_NW_SRC_MASK) >> OFMatch.OFPFW_NW_SRC_SHIFT;
	    }

	    @Override
	    int setInt(final int flags, final int srcMask) {
		return flags & ~OFMatch.OFPFW_NW_SRC_MASK
		        | srcMask << OFMatch.OFPFW_NW_SRC_SHIFT;
	    }

	    @Override
	    int wildcard(final int flags) {
		return flags & ~OFMatch.OFPFW_NW_SRC_MASK;
	    }

	    @Override
	    int matchOn(final int flags) {
		return flags | OFMatch.OFPFW_NW_SRC_ALL;
	    }

	    @Override
	    boolean isPartiallyOn(final int flags) {
		final int intValue = this.getInt(flags);
		return intValue > 0 && intValue < 32;
	    }

	    @Override
	    boolean isFullyOn(final int flags) {
		return this.getInt(flags) >= 32;
	    }

	},
	NW_DST(-1) { /*
	              * virtual NW_SRC flag => translates to the strange 6 bits
	              * in the header
	              */
	    @Override
	    boolean isBolean() {
		return false;
	    }

	    @Override
	    int getInt(final int flags) {
		return (flags & OFMatch.OFPFW_NW_DST_MASK) >> OFMatch.OFPFW_NW_DST_SHIFT;
	    }

	    @Override
	    int setInt(final int flags, final int srcMask) {
		return flags & ~OFMatch.OFPFW_NW_DST_MASK
		        | srcMask << OFMatch.OFPFW_NW_DST_SHIFT;
	    }

	    @Override
	    int wildcard(final int flags) {
		return flags & ~OFMatch.OFPFW_NW_DST_MASK;
	    }

	    @Override
	    int matchOn(final int flags) {
		return flags | OFMatch.OFPFW_NW_DST_ALL;
	    }

	    @Override
	    boolean isFullyOn(final int flags) {
		return this.getInt(flags) >= 32;
	    }
	},
	NW_TOS(OFMatch.OFPFW_NW_TOS); /* IP ToS (DSCP field, 6 bits). */

	final int bitPosition;

	Flag(final int bitPosition) {
	    this.bitPosition = bitPosition;
	}

	/**
	 * @return a modified OF-1.0 flags field with this flag cleared (match
	 *         on this field)
	 */
	int matchOn(final int flags) {
	    return flags & ~this.bitPosition;
	}

	/**
	 * @return a modified OF-1.0 flags field with this flag set (wildcard
	 *         this field)
	 */
	int wildcard(final int flags) {
	    return flags | this.bitPosition;
	}

	/**
	 * @return true iff this is a true boolean flag that can either be off
	 *         or on.True in OF-1.0 for all fields except NW_SRC and NW_DST
	 */
	boolean isBolean() {
	    return false;
	}

	/**
	 * @return true iff this wildcard field is currently 'partially on'.
	 *         Always false for true Boolean Flags. Can be true in OF-1.0
	 *         for NW_SRC, NW_DST.
	 */
	boolean isPartiallyOn(final int flags) {
	    return false;
	}

	/**
	 * @return true iff this wildcard field currently fully on (fully
	 *         wildcarded). Equivalent to the boolean flag being set in the
	 *         bitmask for bit flags, and to the wildcarded bit length set
	 *         to >=32 for NW_SRC and NW_DST
	 * @param flags
	 * @return
	 */
	boolean isFullyOn(final int flags) {
	    return (flags & this.bitPosition) != 0;
	}

	/**
	 * set the integer representation of this flag. only for NW_SRC and
	 * NW_DST
	 */
	int setInt(final int flags, final int srcMask) {
	    throw new UnsupportedOperationException();
	}

	/**
	 * set the integer representation of this flag. only for NW_SRC and
	 * NW_DST
	 */
	int getInt(final int flags) {
	    throw new UnsupportedOperationException();
	}

    }

    private final int flags;

    /** private constructor. use Wildcard.of() instead */
    private Wildcards(final int flags) {
	this.flags = flags;
    }

    /**
     * return a wildcard object matching the given int flags. May reuse / cache
     * frequently used wildcard instances. Don't rely on it though (use equals
     * not ==).
     * 
     * @param flags
     * @return
     */
    public static Wildcards of(final int paramFlags) {
	final int flags = Wildcards.sanitizeInt(paramFlags);
	switch (flags) {
	    case 0x0000:
		return Wildcards.EXACT;
	    case OFMatch.OFPFW_ALL_SANITIZED:
		return Wildcards.FULL;
	    case INT_INPORT_L2_MATCH:
		return Wildcards.INPORT_L2_MATCH;
	    default:
		return new Wildcards(flags);
	}
    }

    /** convience method return a wildcard for exactly one set flag */
    public static Wildcards of(final Wildcards.Flag setFlag) {
	return Wildcards.of(setFlag.wildcard(0));
    }

    /** convience method return a wildcard for exactly two set flags */
    public static Wildcards of(final Wildcards.Flag setFlag,
	    final Wildcards.Flag setFlag2) {
	return Wildcards.of(setFlag.wildcard(setFlag2.wildcard(0)));
    }

    /** convience method return a wildcard for an arbitrary number of set flags */
    public static Wildcards of(final Wildcards.Flag... setFlags) {
	int flags = 0;
	for (final Wildcards.Flag flag : setFlags) {
	    flags = flag.wildcard(0);
	}
	return Wildcards.of(flags);
    }

    /** convience method return a wildcards for ofmatches that match on one flag */
    public static Wildcards ofMatches(final Wildcards.Flag setFlag) {
	return Wildcards.of(setFlag.matchOn(Wildcards.FULL_INT));
    }

    /**
     * convience method return a wildcard for for an ofmatch that match on two
     * flags
     */
    public static Wildcards ofMatches(final Wildcards.Flag setFlag,
	    final Wildcards.Flag setFlag2) {
	return Wildcards.of(setFlag.matchOn(setFlag2
	        .matchOn(Wildcards.FULL_INT)));
    }

    /**
     * convience method return a wildcard for an ofmatch that amtch on an
     * arbitrary number of set flags
     */
    public static Wildcards ofMatches(final Wildcards.Flag... setFlags) {
	int flags = Wildcards.FULL_INT;
	for (final Wildcards.Flag flag : setFlags) {
	    flags = flag.matchOn(flags);
	}
	return Wildcards.of(flags);
    }

    /**
     * return a Wildcards object that has the given flags set
     * <p>
     * <b>NOTE:</b> NOT a mutator function. 'this' wildcard object stays
     * unmodified. </b>
     */
    public Wildcards wildcard(final Wildcards.Flag flag) {
	final int flags = flag.wildcard(this.flags);
	if (flags == this.flags) {
	    return this;
	} else {
	    return new Wildcards(flags);
	}
    }

    /**
     * return a Wildcards object that has the given flags set
     * <p>
     * <b>NOTE:</b> NOT a mutator function. 'this' wildcard object stays
     * unmodified. </b>
     */
    public Wildcards wildcard(final Wildcards.Flag flag,
	    final Wildcards.Flag flag2) {
	final int flags = flag.wildcard(flag2.wildcard(this.flags));
	if (flags == this.flags) {
	    return this;
	} else {
	    return new Wildcards(flags);
	}
    }

    /**
     * return a Wildcards object that has the given flags wildcarded
     * <p>
     * <b>NOTE:</b> NOT a mutator function. 'this' wildcard object stays
     * unmodified. </b>
     */
    public Wildcards wildcard(final Wildcards.Flag... setFlags) {
	int flags = this.flags;
	for (final Wildcards.Flag flag : setFlags) {
	    flags = flag.wildcard(flags);
	}
	if (flags == this.flags) {
	    return this;
	} else {
	    return new Wildcards(flags);
	}
    }

    /**
     * return a Wildcards object that matches on exactly the given flag
     * <p>
     * <b>NOTE:</b> NOT a mutator function. 'this' wildcard object stays
     * unmodified. </b>
     */
    public Wildcards matchOn(final Wildcards.Flag flag) {
	final int flags = flag.matchOn(this.flags);
	if (flags == this.flags) {
	    return this;
	} else {
	    return new Wildcards(flags);
	}
    }

    /**
     * return a Wildcards object that matches on exactly the given flags
     * <p>
     * <b>NOTE:</b> NOT a mutator function. 'this' wildcard object stays
     * unmodified. </b>
     */
    public Wildcards matchOn(final Wildcards.Flag flag,
	    final Wildcards.Flag flag2) {
	final int flags = flag.matchOn(flag2.matchOn(this.flags));
	if (flags == this.flags) {
	    return this;
	} else {
	    return new Wildcards(flags);
	}
    }

    /**
     * return a Wildcards object that matches on exactly the given flags
     * <p>
     * <b>NOTE:</b> NOT a mutator function. 'this' wildcard object stays
     * unmodified. </b>
     */
    public Wildcards matchOn(final Wildcards.Flag... setFlags) {
	int flags = this.flags;
	for (final Wildcards.Flag flag : setFlags) {
	    flags = flag.matchOn(flags);
	}
	if (flags == this.flags) {
	    return this;
	} else {
	    return new Wildcards(flags);
	}
    }

    /**
     * return the nw src mask in normal CIDR style, e.g., 8 means x.x.x.x/8
     * means 8 bits wildcarded
     */
    public int getNwSrcMask() {
	return Math.max(0, 32 - Flag.NW_SRC.getInt(this.flags));
    }

    /**
     * return the nw dst mask in normal CIDR style, e.g., 8 means x.x.x.x/8
     * means 8 bits wildcarded
     */
    public int getNwDstMask() {
	return Math.max(0, 32 - Flag.NW_DST.getInt(this.flags));
    }

    /**
     * return a Wildcard object that has the given nwSrcCidrMask set.
     * <b>NOTE:</b> NOT a mutator function. 'this' wildcard object stays
     * unmodified. </b>
     * 
     * @param srcCidrMask
     *            source mask to set in <b>normal CIDR notation</b>, i.e., 8
     *            means x.x.x.x/8
     * @return a modified object
     */
    public Wildcards withNwSrcMask(final int srcCidrMask) {
	final int flags = Flag.NW_SRC.setInt(this.flags,
	        Math.max(0, 32 - srcCidrMask));
	if (flags == this.flags) {
	    return this;
	} else {
	    return new Wildcards(flags);
	}
    }

    /**
     * return a Wildcard object that has the given nwDstCidrMask set.
     * <b>NOTE:</b> NOT a mutator function. 'this' wildcard object stays
     * unmodified. </b>
     * 
     * @param dstCidrMask
     *            dest mask to set in <b>normal CIDR notation</b>, i.e., 8 means
     *            x.x.x.x/8
     * @return a modified object
     */
    public Wildcards withNwDstMask(final int dstCidrMask) {
	final int flags = Flag.NW_DST.setInt(this.flags,
	        Math.max(0, 32 - dstCidrMask));
	if (flags == this.flags) {
	    return this;
	} else {
	    return new Wildcards(flags);
	}
    }

    /**
     * return a Wildcard object that is inverted to this wildcard object.
     * <b>NOTE:</b> NOT a mutator function. 'this' wildcard object stays
     * unmodified. </b>
     * 
     * @return a modified object
     */
    public Wildcards inverted() {
	return Wildcards.of(this.flags ^ OFMatch.OFPFW_ALL_SANITIZED);
    }

    public boolean isWildcarded(final Flag flag) {
	return flag.isFullyOn(this.flags);
    }

    /**
     * return all wildcard flags that are fully wildcarded as an EnumSet. Do not
     * modify. Note: some flags (like NW_SRC and NW_DST) that are partially
     * wildcarded are not returned in this set.
     * 
     * @return the EnumSet of wildcards
     */
    public EnumSet<Wildcards.Flag> getWildcardedFlags() {
	final EnumSet<Wildcards.Flag> res = EnumSet
	        .noneOf(Wildcards.Flag.class);
	for (final Wildcards.Flag flag : Flag.values()) {
	    if (flag.isFullyOn(this.flags)) {
		res.add(flag);
	    }
	}
	return res;
    }

    /** return the OpenFlow 'wire' integer representation of these wildcards */
    public int getInt() {
	return this.flags;
    }

    /**
     * return the OpenFlow 'wire' integer representation of these wildcards.
     * Sanitize nw_src and nw_dst to be max. 32 (values > 32 are technically
     * possible, but don't make semantic sense)
     */
    public static int sanitizeInt(int flags) {
	if ((flags & OFMatch.OFPFW_NW_SRC_MASK) >> OFMatch.OFPFW_NW_SRC_SHIFT > 32) {
	    flags = flags & ~OFMatch.OFPFW_NW_SRC_MASK
		    | OFMatch.OFPFW_NW_SRC_ALL;
	}
	if ((flags & OFMatch.OFPFW_NW_DST_MASK) >> OFMatch.OFPFW_NW_DST_SHIFT > 32) {
	    flags = flags & ~OFMatch.OFPFW_NW_DST_MASK
		    | OFMatch.OFPFW_NW_DST_ALL;
	}
	return flags;
    }

    /**
     * is this a wildcard set that has all flags set + and full (/0) nw_src and
     * nw_dst wildcarding ?
     */
    public boolean isFull() {
	return this.flags == OFMatch.OFPFW_ALL
	        || this.flags == OFMatch.OFPFW_ALL_SANITIZED;
    }

    /** is this a wildcard of an exact match */
    public boolean isExact() {
	return this.flags == 0;
    }

    @Override
    public int hashCode() {
	final int prime = 31;
	int result = 1;
	result = prime * result + this.flags;
	return result;
    }

    @Override
    public boolean equals(final Object obj) {
	if (this == obj) {
	    return true;
	}
	if (obj == null) {
	    return false;
	}
	if (this.getClass() != obj.getClass()) {
	    return false;
	}
	final Wildcards other = (Wildcards) obj;
	if (this.flags != other.flags) {
	    return false;
	}
	return true;
    }

    private final static Joiner pipeJoiner = Joiner.on("|");

    @Override
    public String toString() {
	final List<String> res = new ArrayList<String>();
	for (final Wildcards.Flag flag : Flag.values()) {
	    if (flag.isFullyOn(this.flags)) {
		res.add(flag.name().toLowerCase());
	    }
	}

	if (Flag.NW_SRC.isPartiallyOn(this.flags)) {
	    res.add("nw_src(/" + this.getNwSrcMask() + ")");
	}

	if (Flag.NW_DST.isPartiallyOn(this.flags)) {
	    res.add("nw_dst(/" + this.getNwDstMask() + ")");
	}

	return Wildcards.pipeJoiner.join(res);
    }

    private final static Joiner commaJoiner = Joiner.on(", ");

    /** a Java expression that constructs 'this' wildcards set */
    public String toJava() {
	if (this.isFull()) {
	    return "Wildcards.FULL";
	} else
	    if (this.isExact()) {
		return "Wildcards.EXACT";
	    }

	final StringBuilder b = new StringBuilder();

	final EnumSet<Flag> myFlags = this.getWildcardedFlags();
	if (myFlags.size() < 3) {
	    // default to start with empty
	    b.append("Wildcards.of("
		    + Wildcards.commaJoiner.join(this.prefix("Flag.",
		            myFlags.iterator())) + ")");
	} else {
	    // too many - start with full

	    final EnumSet<Flag> invFlags = this.inverted().getWildcardedFlags();
	    b.append("Wildcards.ofMatches("
		    + Wildcards.commaJoiner.join(this.prefix("Flag.",
		            invFlags.iterator())) + ")");
	}
	if (Flag.NW_SRC.isPartiallyOn(this.flags)) {
	    b.append(".setNwSrcMask(" + this.getNwSrcMask() + ")");
	}
	if (Flag.NW_DST.isPartiallyOn(this.flags)) {
	    b.append(".setNwDstMask(" + this.getNwDstMask() + ")");
	}
	return b.toString();
    }

    private Iterator<String> prefix(final String prefix, final Iterator<?> i) {
	return new Iterator<String>() {

	    @Override
	    public boolean hasNext() {
		return i.hasNext();
	    }

	    @Override
	    public String next() {
		final Object next = i.next();
		return next == null ? null : prefix + next.toString();
	    }

	    @Override
	    public void remove() {
		i.remove();
	    }
	};
    }

}