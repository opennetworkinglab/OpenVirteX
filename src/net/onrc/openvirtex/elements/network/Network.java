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

package net.onrc.openvirtex.elements.network;

import java.util.HashMap;
import java.util.HashSet;

import net.onrc.openvirtex.elements.OVXMap;

/**
 * @author gerola
 * 
 */
public abstract class Network<T1, T2, T3> {

    private HashSet<T1>             switchSet;
    private HashSet<T3>             linkSet;
    public HashMap<T2, T2>          neighbourPortMap;
    public OVXMap                   map;
    public HashMap<T1, HashSet<T1>> neighbourMap;

    // public OFControllerChannel channel;

    public void registerSwitch(final T1 sw) {
    }

    public void unregisterSwitch(final T1 sw) {
    }

    public void registerLink(final T3 link) {
    }

    public void unregisterLink(final T3 link) {
    }

    public boolean initialize() {
	return true;
    }

    public HashSet<T1> getNeighbours(final T1 sw) {
	return null;
    }

    public abstract void sendLLDP(T1 sw);

    public abstract void receiveLLDP(T1 sw);

}
