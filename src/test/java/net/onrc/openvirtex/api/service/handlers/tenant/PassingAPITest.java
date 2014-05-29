/*******************************************************************************
 * Copyright 2014 Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package net.onrc.openvirtex.api.service.handlers.tenant;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;
import junit.framework.TestSuite;
import net.onrc.openvirtex.api.service.handlers.TenantHandler;
import net.onrc.openvirtex.core.OpenVirteXController;
import net.onrc.openvirtex.core.cmd.CmdLineSettings;
import net.onrc.openvirtex.elements.OVXMap;
import net.onrc.openvirtex.elements.datapath.PhysicalSwitch;
import net.onrc.openvirtex.elements.link.PhysicalLink;
import net.onrc.openvirtex.elements.network.OVXNetwork;
import net.onrc.openvirtex.elements.network.PhysicalNetwork;
import net.onrc.openvirtex.elements.port.PhysicalPort;

import org.openflow.protocol.OFFeaturesReply;
import org.openflow.protocol.OFPhysicalPort;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;

/**
 * Tests for the API calls.
 */
public class PassingAPITest extends AbstractAPICalls {

    private static final OpenVirteXController CTRL =
            new OpenVirteXController(new CmdLineSettings());

    /**
     * @return the suite of tests being tested
     */
    public static TestSuite suite() {
        return new TestSuite(PassingAPITest.class);
    }

    /**
     * Tests whether a create network call succeeds.
     */
    @SuppressWarnings("unchecked")
    public void testCreateNetPass() {

        final JSONRPC2Response resp = super.createNetwork();

        Assert.assertNull("CreateOVXNetwork should not return null",
                resp.getError());

        Assert.assertTrue("CreateOVXNetwork has incorrect return type",
                resp.getResult() instanceof Map<?, ?>);

        Map<String, Object> result = (Map<String, Object>) resp.getResult();

        Assert.assertEquals(1, result.get(TenantHandler.TENANT));
    }

    /**
     * Test whether the creation of a host actually succeeds.
     */
    @SuppressWarnings("unchecked")
    public void testCreateSingleSwitch() {
        final PhysicalSwitch sw = new PhysicalSwitch(1);
        configNW(Collections.singletonList(sw));
        super.createNetwork();
        final JSONRPC2Response resp = super.createSwitch(1,
                Collections.singletonList(1));
        Assert.assertNull("CreateOVXSwitch should not return null",
                resp.getError());

        Assert.assertTrue("CreateOVXSwitch has incorrect return type",
                resp.getResult() instanceof Map<?, ?>);

        Map<String, Object> result = (Map<String, Object>) resp.getResult();

        Assert.assertEquals((long) 46200400562356225L,
                result.get(TenantHandler.VDPID));
    }

    /**
     * Test whether the creation of a host actually succeeds.
     */
    @SuppressWarnings("unchecked")
    public void testCreateBigSwitch() {
        // final PhysicalSwitch sw1 = new PhysicalSwitch(1);
        // final PhysicalSwitch sw2 = new PhysicalSwitch(2);
        final TestSwitch sw1 = new TestSwitch(1);
        final TestSwitch sw2 = new TestSwitch(2);
        configNW(new LinkedList<PhysicalSwitch>(Arrays.asList(sw1, sw2)));

        final PhysicalPort p1 = configPort(new byte[] {0x01, 0x02, 0x03, 0x04,
                0x05, 0x06}, (short) 1, false, sw1);
        configSW(Collections.singletonList((OFPhysicalPort) p1), sw1);

        final PhysicalPort p2 = configPort(new byte[] {0x11, 0x12, 0x13, 0x14,
                0x15, 0x16}, (short) 1, false, sw2);
        configSW(Collections.singletonList((OFPhysicalPort) p2), sw2);

        PhysicalNetwork.getInstance().createLink(p1, p2);
        PhysicalNetwork.getInstance().createLink(p2, p1);
        for (PhysicalLink l : PhysicalNetwork.getInstance().getLinks()) {
            l.register();
            l.boot();
        }

        super.createNetwork();
        final List<Integer> l = new LinkedList<>();
        l.add(1);
        l.add(2);
        final JSONRPC2Response resp = super.createSwitch(1, l);

        Assert.assertNull("CreateOVXSwitch should not return null",
                resp.getError());

        Assert.assertTrue("CreateOVXSwitch has incorrect return type",
                resp.getResult() instanceof Map<?, ?>);

        Map<String, Object> result = (Map<String, Object>) resp.getResult();

        Assert.assertEquals((long) 46200400562356225L,
                result.get(TenantHandler.VDPID));
    }

    /**
     * Tests whether a create port call succeeds.
     */
    @SuppressWarnings("unchecked")
    public void testCreatePort() {
        final TestSwitch sw = new TestSwitch(1);
        configNW(new LinkedList<PhysicalSwitch>(Arrays.asList(sw)));

        final PhysicalPort port = configPort(new byte[] {0x01, 0x02, 0x03,
                0x04, 0x05, 0x06}, (short) 1, false, sw);
        configSW(Collections.singletonList((OFPhysicalPort) port), sw);

        super.createNetwork();
        super.createSwitch(1, Collections.singletonList(1));

        final JSONRPC2Response resp = super.createPort(1, (long) 1, (short) 1);

        Assert.assertNull("CreateOVXPort should not return null",
                resp.getError());

        Assert.assertTrue("CreateOVXPort has incorrect return type",
                resp.getResult() instanceof Map<?, ?>);

        Map<String, Object> result = (Map<String, Object>) resp.getResult();

        Assert.assertEquals((short) 1, result.get(TenantHandler.VPORT));
    }

    /**
     * Tests whether a connect host call succeeds.
     */
    @SuppressWarnings("unchecked")
    public void testConnectHost() {
        final TestSwitch sw1 = new TestSwitch(1);
        configNW(new LinkedList<PhysicalSwitch>(Arrays.asList(sw1)));

        final PhysicalPort port = configPort(new byte[] {0x01, 0x02, 0x03,
                0x04, 0x05, 0x06}, (short) 1, false, sw1);
        configSW(Collections.singletonList((OFPhysicalPort) port), sw1);

        super.createNetwork();
        super.createSwitch(1, Collections.singletonList(1));
        super.createPort(1, (long) 1, (short) 1);
        JSONRPC2Response resp = super.connectHost(1, (long) 46200400562356225L,
                (short) 1, "00:00:00:00:00:01");

        Assert.assertNull(
                resp.getError() == null ? "ConnectHost should not return null"
                        : resp.getError().getMessage(), resp.getError());

        Assert.assertTrue("ConnectHost has incorrect return type",
                resp.getResult() instanceof Map<?, ?>);

        Map<String, Object> result = (Map<String, Object>) resp.getResult();

        Assert.assertEquals(1, result.get(TenantHandler.HOST));

        // Try to create another host with same MAC
        resp = super.connectHost(1, (long) 46200400562356225L, (short) 1,
                "00:00:00:00:00:01");
        Assert.assertNotNull("ConnectHost should not allow duplicate MACs",
                resp.getError());
    }

    /**
     * Tests whether a connect link call succeeds.
     */
    @SuppressWarnings("unchecked")
    public void testConnectLink() {
        super.createNetwork();
        final TestSwitch sw1 = new TestSwitch(1);
        final TestSwitch sw2 = new TestSwitch(2);

        configNW(new LinkedList<PhysicalSwitch>(Arrays.asList(sw1, sw2)));

        final PhysicalPort p1 = configPort(new byte[] {0x01, 0x02, 0x03, 0x04,
                0x05, 0x06}, (short) 1, false, sw1);
        configSW(Collections.singletonList((OFPhysicalPort) p1), sw1);

        final PhysicalPort p2 = configPort(new byte[] {0x11, 0x12, 0x13, 0x14,
                0x15, 0x16}, (short) 1, false, sw2);
        configSW(Collections.singletonList((OFPhysicalPort) p2), sw2);

        PhysicalNetwork.getInstance().createLink(p1, p2);
        PhysicalNetwork.getInstance().createLink(p2, p1);

        super.createSwitch(1, Collections.singletonList(1));
        super.createSwitch(1, Collections.singletonList(2));
        super.createPort(1, (long) 1, (short) 1);
        super.createPort(1, (long) 2, (short) 1);

        JSONRPC2Response resp = super.connectLink(1, (long) 46200400562356225L,
                (short) 1, (long) 46200400562356226L, (short) 1, "manual",
                (byte) 0);

        Assert.assertNull(
                resp.getError() == null ? "ConnectLink should not return null"
                        : resp.getError().getMessage(), resp.getError());

        Assert.assertTrue("ConnectLink has incorrect return type",
                resp.getResult() instanceof Map<?, ?>);

        Map<String, Object> result = (Map<String, Object>) resp.getResult();

        Assert.assertEquals(1, result.get(TenantHandler.LINK));

        // TODO: should this not be its own separate test?

        resp = super.setLinkPath(1, 1, "1/1-2/1", (byte) 100);

        Assert.assertNull(
                resp.getError() == null ? "SetLinkPath should not return null"
                        : resp.getError().getMessage(), resp.getError());

        Assert.assertTrue("SetLinkPath has incorrect return type",
                resp.getResult() instanceof Map<?, ?>);

        result = (Map<String, Object>) resp.getResult();

        // TODO: we should check if the path really has been set
        // can't do this now as map is not working properly in the tests

        Assert.assertEquals(1, result.get(TenantHandler.LINK));
    }

    /**
     * Tests whether a create switch route call succeeds.
     */
    @SuppressWarnings("unchecked")
    public void testConnectRoutePass() {
        // set the physical network (linear 2 sws with 1 host x sw)
        final TestSwitch sw1 = new TestSwitch(1);
        final TestSwitch sw2 = new TestSwitch(2);
        configNW(new LinkedList<PhysicalSwitch>(Arrays.asList(sw1, sw2)));

        final PhysicalPort p1 = configPort(new byte[] {0x01, 0x02, 0x03, 0x04,
                0x05, 0x06}, (short) 1, false, sw1);
        final PhysicalPort p2 = configPort(new byte[] {0x07, 0x08, 0x09, 0x0a,
                0x0b, 0x0c}, (short) 2, true, sw1);

        final PhysicalPort p3 = configPort(new byte[] {0x11, 0x12, 0x13, 0x14,
                0x15, 0x16}, (short) 1, false, sw2);
        final PhysicalPort p4 = configPort(new byte[] {0x17, 0x18, 0x19, 0x1a,
                0x1b, 0x1c}, (short) 2, true, sw2);

        configSW(new LinkedList<OFPhysicalPort>(Arrays.asList(p1, p2)), sw1);
        configSW(new LinkedList<OFPhysicalPort>(Arrays.asList(p3, p4)), sw2);

        PhysicalNetwork.getInstance().createLink(p1, p3);
        PhysicalNetwork.getInstance().createLink(p3, p1);

        // set the virtual network (copy of the phy network)
        super.createNetwork();
        final List<Integer> l = new LinkedList<>();
        l.add(1);
        l.add(2);
        super.createSwitch(1, l);
        super.createPort(1, (long) 1, (short) 2);
        super.createPort(1, (long) 2, (short) 2);
        super.connectHost(1, (long) 46200400562356225L, (short) 1,
                "00:00:00:00:00:01");
        super.connectHost(1, (long) 46200400562356225L, (short) 2,
                "00:00:00:00:00:02");

        final JSONRPC2Response resp = super.connectRoute(1, 46200400562356225L,
                (short) 1, (short) 2, "1/1-2/1", (byte) 100);

        Assert.assertNull("ConnectOVXRoute should not return null",
                resp.getError());

        Assert.assertTrue("ConnectOVXRoute has incorrect return type",
                resp.getResult() instanceof Map<?, ?>);

        Map<String, Object> result = (Map<String, Object>) resp.getResult();

        Assert.assertEquals(1, result.get(TenantHandler.ROUTE));
    }

    /**
     * Tests whether a remove network call succeeds.
     */
    public void testRemoveNetPass() {
        // set the physical network (linear 2 sws with 1 host x sw)
        final TestSwitch sw1 = new TestSwitch(1);
        final TestSwitch sw2 = new TestSwitch(2);
        configNW(new LinkedList<PhysicalSwitch>(Arrays.asList(sw1, sw2)));

        final PhysicalPort p1 = configPort(new byte[] {0x01, 0x02, 0x03, 0x04,
                0x05, 0x06}, (short) 1, false, sw1);
        final PhysicalPort p2 = configPort(new byte[] {0x07, 0x08, 0x09, 0x0a,
                0x0b, 0x0c}, (short) 2, true, sw1);

        final PhysicalPort p3 = configPort(new byte[] {0x11, 0x12, 0x13, 0x14,
                0x15, 0x16}, (short) 1, false, sw2);
        final PhysicalPort p4 = configPort(new byte[] {0x17, 0x18, 0x19, 0x1a,
                0x1b, 0x1c}, (short) 2, true, sw2);

        configSW(new LinkedList<OFPhysicalPort>(Arrays.asList(p1, p2)), sw1);
        configSW(new LinkedList<OFPhysicalPort>(Arrays.asList(p3, p4)), sw2);

        PhysicalNetwork.getInstance().createLink(p1, p3);
        PhysicalNetwork.getInstance().createLink(p3, p1);

        // set the virtual network (copy of the phy network)
        super.createNetwork();
        super.createSwitch(1, Collections.singletonList(1));
        super.createSwitch(1, Collections.singletonList(2));
        super.createPort(1, (long) 1, (short) 1);
        super.createPort(1, (long) 2, (short) 1);
        super.createPort(1, (long) 1, (short) 2);
        super.createPort(1, (long) 2, (short) 2);
        super.connectHost(1, (long) 46200400562356225L, (short) 2,
                "00:00:00:00:00:01");
        super.connectHost(1, (long) 46200400562356226L, (short) 2,
                "00:00:00:00:00:02");
        super.connectLink(1, (long) 46200400562356225L, (short) 1,
                (long) 46200400562356226L, (short) 1, "manual", (byte) 0);
        super.setLinkPath(1, 1, "1/1-2/1", (byte) 100);
        final JSONRPC2Response resp = super.removeNetwork(1);

        Assert.assertNull(resp.getResult());
    }

    /**
     * Tests whether a remove switch call succeeds.
     */
    public void testRemoveSwitchPass() {
        // set the physical network (linear 2 sws with 1 host x sw)
        final TestSwitch sw1 = new TestSwitch(1);
        final TestSwitch sw2 = new TestSwitch(2);
        configNW(new LinkedList<PhysicalSwitch>(Arrays.asList(sw1, sw2)));

        final PhysicalPort p1 = new PhysicalPort(new OFPhysicalPort(), sw1,
                false);
        p1.setHardwareAddress(new byte[] {0x01, 0x02, 0x03, 0x04, 0x05, 0x06});
        p1.setPortNumber((short) 1);
        sw1.addPort(p1);
        final PhysicalPort p2 = new PhysicalPort(new OFPhysicalPort(), sw1,
                true);
        p2.setHardwareAddress(new byte[] {0x07, 0x08, 0x09, 0x0a, 0x0b, 0x0c});
        p2.setPortNumber((short) 2);
        sw1.addPort(p2);
        final PhysicalPort p3 = new PhysicalPort(new OFPhysicalPort(), sw2,
                false);
        p3.setHardwareAddress(new byte[] {0x11, 0x12, 0x13, 0x14, 0x15, 0x16});
        p3.setPortNumber((short) 1);
        sw2.addPort(p3);
        final PhysicalPort p4 = new PhysicalPort(new OFPhysicalPort(), sw2,
                true);
        p4.setHardwareAddress(new byte[] {0x17, 0x18, 0x19, 0x1a, 0x1b, 0x1c});
        p4.setPortNumber((short) 2);
        sw2.addPort(p4);
        PhysicalNetwork.getInstance().createLink(p1, p3);
        PhysicalNetwork.getInstance().createLink(p3, p1);

        // set the virtual network (copy of the phy network)
        super.createNetwork();
        super.createSwitch(1, Collections.singletonList(1));
        super.createSwitch(1, Collections.singletonList(2));
        super.createPort(1, (long) 1, (short) 1);
        super.createPort(1, (long) 2, (short) 1);
        super.createPort(1, (long) 1, (short) 2);
        super.createPort(1, (long) 2, (short) 2);
        super.connectHost(1, (long) 46200400562356225L, (short) 2,
                "00:00:00:00:00:01");
        super.connectHost(1, (long) 46200400562356226L, (short) 2,
                "00:00:00:00:00:02");
        super.connectLink(1, (long) 46200400562356225L, (short) 1,
                (long) 46200400562356226L, (short) 1, "manual", (byte) 0);
        super.setLinkPath(1, 1, "1/1-2/1", (byte) 100);
        final JSONRPC2Response resp = super.removeSwitch(1, 46200400562356225L);

        Assert.assertNull(resp.getError());
    }

    /**
     * Tests whether a remove port call succeeds.
     */
    public void testRemovePortPass() {
        // set the physical network (linear 2 sws with 1 host x sw)
        final TestSwitch sw1 = new TestSwitch(1);
        final TestSwitch sw2 = new TestSwitch(2);
        configNW(new LinkedList<PhysicalSwitch>(Arrays.asList(sw1, sw2)));

        final PhysicalPort p1 = configPort(new byte[] {0x01, 0x02, 0x03, 0x04,
                0x05, 0x06}, (short) 1, false, sw1);
        final PhysicalPort p2 = configPort(new byte[] {0x07, 0x08, 0x09, 0x0a,
                0x0b, 0x0c}, (short) 2, true, sw1);

        final PhysicalPort p3 = configPort(new byte[] {0x11, 0x12, 0x13, 0x14,
                0x15, 0x16}, (short) 1, false, sw2);
        final PhysicalPort p4 = configPort(new byte[] {0x17, 0x18, 0x19, 0x1a,
                0x1b, 0x1c}, (short) 2, true, sw2);

        configSW(new LinkedList<OFPhysicalPort>(Arrays.asList(p1, p2)), sw1);
        configSW(new LinkedList<OFPhysicalPort>(Arrays.asList(p3, p4)), sw2);

        PhysicalNetwork.getInstance().createLink(p1, p3);
        PhysicalNetwork.getInstance().createLink(p3, p1);

        // set the virtual network (copy of the phy network)
        super.createNetwork();
        super.createSwitch(1, Collections.singletonList(1));
        super.createSwitch(1, Collections.singletonList(2));
        super.createPort(1, (long) 1, (short) 1);
        super.createPort(1, (long) 2, (short) 1);
        super.createPort(1, (long) 1, (short) 2);
        super.createPort(1, (long) 2, (short) 2);
        super.connectHost(1, (long) 46200400562356225L, (short) 2,
                "00:00:00:00:00:01");
        super.connectHost(1, (long) 46200400562356226L, (short) 2,
                "00:00:00:00:00:02");
        super.connectLink(1, (long) 46200400562356225L, (short) 1,
                (long) 46200400562356226L, (short) 1, "manual", (byte) 0);
        super.setLinkPath(1, 1, "1/1-2/1", (byte) 100);
        final JSONRPC2Response resp = super.removePort(1, 46200400562356225L,
                (short) 1);

        Assert.assertNull(resp.getError());
    }

    /**
     * Tests whether a remove link call succeeds.
     */
    public void testDisconnectLinkPass() {
        // set the physical network (linear 2 sws with 1 host x sw)
        final TestSwitch sw1 = new TestSwitch(1);
        final TestSwitch sw2 = new TestSwitch(2);
        configNW(new LinkedList<PhysicalSwitch>(Arrays.asList(sw1, sw2)));

        final PhysicalPort p1 = configPort(new byte[] {0x01, 0x02, 0x03, 0x04,
                0x05, 0x06}, (short) 1, false, sw1);
        final PhysicalPort p2 = configPort(new byte[] {0x07, 0x08, 0x09, 0x0a,
                0x0b, 0x0c}, (short) 2, true, sw1);

        final PhysicalPort p3 = configPort(new byte[] {0x11, 0x12, 0x13, 0x14,
                0x15, 0x16}, (short) 1, false, sw2);
        final PhysicalPort p4 = configPort(new byte[] {0x17, 0x18, 0x19, 0x1a,
                0x1b, 0x1c}, (short) 2, true, sw2);

        configSW(new LinkedList<OFPhysicalPort>(Arrays.asList(p1, p2)), sw1);
        configSW(new LinkedList<OFPhysicalPort>(Arrays.asList(p3, p4)), sw2);

        PhysicalNetwork.getInstance().createLink(p1, p3);
        PhysicalNetwork.getInstance().createLink(p3, p1);

        // set the virtual network (copy of the phy network)
        super.createNetwork();
        super.createSwitch(1, Collections.singletonList(1));
        super.createSwitch(1, Collections.singletonList(2));
        super.createPort(1, (long) 1, (short) 1);
        super.createPort(1, (long) 2, (short) 1);
        super.createPort(1, (long) 1, (short) 2);
        super.createPort(1, (long) 2, (short) 2);
        super.connectHost(1, (long) 46200400562356225L, (short) 2,
                "00:00:00:00:00:01");
        super.connectHost(1, (long) 46200400562356226L, (short) 2,
                "00:00:00:00:00:02");
        super.connectLink(1, (long) 46200400562356225L, (short) 1,
                (long) 46200400562356226L, (short) 1, "manual", (byte) 0);
        super.setLinkPath(1, 1, "1/1-2/1", (byte) 100);

        final JSONRPC2Response resp = super.disconnectLink(1, 1);

        Assert.assertNull(resp.getError());
    }

    /**
     * Tests whether a disconnect host call succeeds.
     */
    public void testDisconnectHostPass() {
        // set the physical network (linear 2 sws with 1 host x sw)
        final TestSwitch sw1 = new TestSwitch(1);
        final TestSwitch sw2 = new TestSwitch(2);
        configNW(new LinkedList<PhysicalSwitch>(Arrays.asList(sw1, sw2)));

        final PhysicalPort p1 = configPort(new byte[] {0x01, 0x02, 0x03, 0x04,
                0x05, 0x06}, (short) 1, false, sw1);
        final PhysicalPort p2 = configPort(new byte[] {0x07, 0x08, 0x09, 0x0a,
                0x0b, 0x0c}, (short) 2, true, sw1);

        final PhysicalPort p3 = configPort(new byte[] {0x11, 0x12, 0x13, 0x14,
                0x15, 0x16}, (short) 1, false, sw2);
        final PhysicalPort p4 = configPort(new byte[] {0x17, 0x18, 0x19, 0x1a,
                0x1b, 0x1c}, (short) 2, true, sw2);

        configSW(new LinkedList<OFPhysicalPort>(Arrays.asList(p1, p2)), sw1);
        configSW(new LinkedList<OFPhysicalPort>(Arrays.asList(p3, p4)), sw2);

        PhysicalNetwork.getInstance().createLink(p1, p3);
        PhysicalNetwork.getInstance().createLink(p3, p1);

        // set the virtual network (copy of the phy network)
        super.createNetwork();
        super.createSwitch(1, Collections.singletonList(1));
        super.createSwitch(1, Collections.singletonList(2));
        super.createPort(1, (long) 1, (short) 1);
        super.createPort(1, (long) 2, (short) 1);
        super.createPort(1, (long) 1, (short) 2);
        super.createPort(1, (long) 2, (short) 2);
        super.connectHost(1, (long) 46200400562356225L, (short) 2,
                "00:00:00:00:00:01");
        super.connectHost(1, (long) 46200400562356226L, (short) 2,
                "00:00:00:00:00:02");
        super.connectLink(1, (long) 46200400562356225L, (short) 1,
                (long) 46200400562356226L, (short) 1, "manual", (byte) 0);
        super.setLinkPath(1, 1, "1/1-2/1", (byte) 100);

        final JSONRPC2Response resp = super.disconnectHost(1, 1);

        Assert.assertNull("Disconnect Host should have not returned null",
                resp.getError());

        Assert.assertNull(resp.getResult());
    }

    /**
     * Tests whether a disconnect host call succeeds.
     */
    public void testDisconnectRoutePass() {
        // set the physical network (linear 2 sws with 1 host x sw)
        final TestSwitch sw1 = new TestSwitch(1);
        final TestSwitch sw2 = new TestSwitch(2);
        configNW(new LinkedList<PhysicalSwitch>(Arrays.asList(sw1, sw2)));

        final PhysicalPort p1 = configPort(new byte[] {0x01, 0x02, 0x03, 0x04,
                0x05, 0x06}, (short) 1, false, sw1);
        final PhysicalPort p2 = configPort(new byte[] {0x07, 0x08, 0x09, 0x0a,
                0x0b, 0x0c}, (short) 2, true, sw1);

        final PhysicalPort p3 = configPort(new byte[] {0x11, 0x12, 0x13, 0x14,
                0x15, 0x16}, (short) 1, false, sw2);
        final PhysicalPort p4 = configPort(new byte[] {0x17, 0x18, 0x19, 0x1a,
                0x1b, 0x1c}, (short) 2, true, sw2);

        configSW(new LinkedList<OFPhysicalPort>(Arrays.asList(p1, p2)), sw1);
        configSW(new LinkedList<OFPhysicalPort>(Arrays.asList(p3, p4)), sw2);

        PhysicalNetwork.getInstance().createLink(p1, p3);
        PhysicalNetwork.getInstance().createLink(p3, p1);

        // set the virtual network (copy of the phy network)
        super.createNetwork();
        final List<Integer> l = new LinkedList<>();
        l.add(1);
        l.add(2);
        super.createSwitch(1, l);
        super.createPort(1, (long) 1, (short) 2);
        super.createPort(1, (long) 2, (short) 2);
        super.connectHost(1, (long) 46200400562356225L, (short) 1,
                "00:00:00:00:00:01");
        super.connectHost(1, (long) 46200400562356225L, (short) 2,
                "00:00:00:00:00:02");
        super.connectRoute(1, 46200400562356225L, (short) 1, (short) 2,
                "1/1-2/1", (byte) 100);

        final JSONRPC2Response resp = super.disconnectRoute(1,
                (long) 46200400562356225L, 1);

        Assert.assertNull("Remove switch route should not return null",
                resp.getError());

        Assert.assertNull(resp.getResult());
    }

    /**
     * Tests whether a disconnect host call succeeds.
     */
    @SuppressWarnings("unchecked")
    public void testStopNetPass() {
        // set the physical network (linear 2 sws with 1 host x sw)
        final TestSwitch sw1 = new TestSwitch(1);
        final TestSwitch sw2 = new TestSwitch(2);
        configNW(new LinkedList<PhysicalSwitch>(Arrays.asList(sw1, sw2)));

        final PhysicalPort p1 = new PhysicalPort(new OFPhysicalPort(), sw1,
                false);
        p1.setHardwareAddress(new byte[] {0x01, 0x02, 0x03, 0x04, 0x05, 0x06});
        p1.setPortNumber((short) 1);
        sw1.addPort(p1);
        final PhysicalPort p2 = new PhysicalPort(new OFPhysicalPort(), sw1,
                true);
        p2.setHardwareAddress(new byte[] {0x07, 0x08, 0x09, 0x0a, 0x0b, 0x0c});
        p2.setPortNumber((short) 2);
        sw1.addPort(p2);
        final PhysicalPort p3 = new PhysicalPort(new OFPhysicalPort(), sw2,
                false);
        p3.setHardwareAddress(new byte[] {0x11, 0x12, 0x13, 0x14, 0x15, 0x16});
        p3.setPortNumber((short) 1);
        sw2.addPort(p3);
        final PhysicalPort p4 = new PhysicalPort(new OFPhysicalPort(), sw2,
                true);
        p4.setHardwareAddress(new byte[] {0x17, 0x18, 0x19, 0x1a, 0x1b, 0x1c});
        p4.setPortNumber((short) 2);
        sw2.addPort(p4);
        sw1.boot();
        sw2.boot();

        PhysicalNetwork.getInstance().createLink(p1, p3);
        PhysicalNetwork.getInstance().createLink(p3, p1);

        // set the virtual network (copy of the phy network)
        super.createNetwork();
        super.createSwitch(1, Collections.singletonList(1));
        super.createSwitch(1, Collections.singletonList(2));
        super.createPort(1, (long) 1, (short) 1);
        super.createPort(1, (long) 2, (short) 1);
        super.createPort(1, (long) 1, (short) 2);
        super.createPort(1, (long) 2, (short) 2);
        super.connectHost(1, (long) 46200400562356225L, (short) 2,
                "00:00:00:00:00:01");
        super.connectHost(1, (long) 46200400562356226L, (short) 2,
                "00:00:00:00:00:02");
        super.connectLink(1, (long) 46200400562356225L, (short) 1,
                (long) 46200400562356226L, (short) 1, "manual", (byte) 0);
        super.setLinkPath(1, 1, "1/1-2/1", (byte) 100);
        final JSONRPC2Response resp = super.stopNetwork(1);

        Assert.assertNull("Stop network should not return null",
                resp.getError());

        Assert.assertTrue("Stop network has incorrect return type",
                resp.getResult() instanceof Map<?, ?>);

        Map<String, Object> result = (Map<String, Object>) resp.getResult();

        Assert.assertEquals(1, result.get(TenantHandler.TENANT));

        Assert.assertEquals(false, result.get(TenantHandler.IS_BOOTED));
    }

    @Override
    protected void setUp() throws Exception {
        PhysicalNetwork.getInstance().register();
        PhysicalNetwork.getInstance().boot();
    }

    @Override
    protected void tearDown() throws Exception {
        OVXMap.reset();
        PhysicalNetwork.getInstance().tearDown();
        PhysicalNetwork.getInstance().unregister();
        OVXNetwork.reset();
    }

    private void configNW(List<PhysicalSwitch> switches) {
        for (PhysicalSwitch dp : switches) {
            PhysicalNetwork.getInstance().addSwitch(dp);
        }
    }

    private PhysicalPort configPort(byte[] hwaddr, short portnum, boolean flag,
            PhysicalSwitch sw) {
        final PhysicalPort p = new PhysicalPort(new OFPhysicalPort(), sw, flag);
        p.setHardwareAddress(hwaddr);
        p.setPortNumber(portnum);
        p.register();
        p.boot();

        return p;
    }

    private void configSW(List<OFPhysicalPort> ports, PhysicalSwitch sw) {
        OFFeaturesReply fr = new OFFeaturesReply();
        fr.setPorts(ports);
        sw.setFeaturesReply(fr);
        sw.register();
        sw.boot();
    }

}
