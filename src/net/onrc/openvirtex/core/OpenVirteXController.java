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

package net.onrc.openvirtex.core;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.Executors;

import net.onrc.openvirtex.api.APIServer;
import net.onrc.openvirtex.config.OVXConfig;
import net.onrc.openvirtex.core.io.ClientChannelPipeline;
import net.onrc.openvirtex.core.io.SwitchChannelPipeline;
import net.onrc.openvirtex.elements.OVXMap;
import net.onrc.openvirtex.elements.datapath.OVXSwitch;
import net.onrc.openvirtex.elements.network.OVXNetwork;
import net.onrc.openvirtex.elements.network.PhysicalNetwork;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;

public class OpenVirteXController implements Runnable {

    Logger                                      log              = LogManager
	                                                                 .getLogger(OpenVirteXController.class
	                                                                         .getName());

    private static final int                    SEND_BUFFER_SIZE = 1024 * 1024;
    private static OpenVirteXController         instance         = null;

    public static String                        HOST             = "host";
    public static String                        OFPORT           = "openflow-port";
    public static String                        APIPORT          = "api-port";
    public static String                        OPENVIRTEX       = "openvirtex";

    private String                              configFile       = null;
    private String                              ofHost           = null;
    private Integer                             ofPort           = null;
    APIServer                                   server;

    public static Integer                       apiPort          = 8080;

    private final NioClientSocketChannelFactory clientSockets    = new NioClientSocketChannelFactory(
	                                                                 Executors
	                                                                         .newCachedThreadPool(),
	                                                                 Executors
	                                                                         .newCachedThreadPool());

    private final ChannelGroup                  sg               = new DefaultChannelGroup();
    private final ChannelGroup                  cg               = new DefaultChannelGroup();

    private SwitchChannelPipeline               pfact            = null;
    private ClientChannelPipeline               cfact            = null;

    private int                                 maxVirtual       = 0;

    public OpenVirteXController(final String configFile, final String ofHost,
	    final Integer ofPort, final int maxVirtual) {
	this.configFile = configFile;
	this.ofHost = ofHost;
	this.ofPort = ofPort;
	this.maxVirtual = maxVirtual;
	OpenVirteXController.instance = this;
    }

    public OpenVirteXController(final String ofHost, final Integer ofPort,
	    final Integer apiPort) {
	this.ofHost = ofHost;
	this.ofPort = ofPort;
	this.apiPort = apiPort;
    }

    public OpenVirteXController() {

    }

    @Override
    public void run() {
	Runtime.getRuntime().addShutdownHook(new OpenVirtexShutdownHook(this));
	PhysicalNetwork.getInstance().boot();

	// OVXSingleSwitch sw = new OVXSingleSwitch(1,1);
	// sw.init();
	// this.registerOVXSwitch(sw, "192.168.2.136", 6633);
	// this.registerOVXSwitch(new OVXSingleSwitch("fake", (long)2, null, 1,
	// (short)100), "192.168.2.136", 6633);

	try {
	    final ServerBootstrap switchServerBootStrap = this
		    .createServerBootStrap();

	    this.setServerBootStrapParams(switchServerBootStrap);

	    this.pfact = new SwitchChannelPipeline(this, null);
	    switchServerBootStrap.setPipelineFactory(this.pfact);
	    final InetSocketAddress sa = this.ofHost == null ? new InetSocketAddress(
		    this.ofPort) : new InetSocketAddress(this.ofHost,
		    this.ofPort);
	    this.sg.add(switchServerBootStrap.bind(sa));

	    if (this.configFile != null) {
		OVXConfig.loadConfig(this.configFile);
	    }
	    /*
	     * System.out.println("Before starting server1"+this.configFile);
	     * this.startServer();
	     * System.out.println("After starting server");
	     * }
	     * else{
	     */
	    System.out.println("Before starting server2");
	    this.startServer();
	    System.out.println("After starting server");
	    // }

	} catch (final Exception e) {
	    throw new RuntimeException(e);
	}

    }

    public void registerOVXSwitch(final OVXSwitch sw) {
	final OVXNetwork ovxNetwork = OVXMap.getInstance().getVirtualNetwork(
	        sw.getTenantId());
	final String host = ovxNetwork.getControllerHost();
	final Integer port = ovxNetwork.getControllerPort();

	final ClientBootstrap clientBootStrap = this.createClientBootStrap();
	this.setClientBootStrapParams(clientBootStrap);
	final InetSocketAddress remoteAddr = new InetSocketAddress(host, port);
	clientBootStrap.setOption("remoteAddress", remoteAddr);

	this.cfact = new ClientChannelPipeline(this, this.cg, null,
	        clientBootStrap, sw);
	clientBootStrap.setPipelineFactory(this.cfact);

	final ChannelFuture cf = clientBootStrap.connect();

	cf.addListener(new ChannelFutureListener() {

	    @Override
	    public void operationComplete(final ChannelFuture e)
		    throws Exception {
		if (e.isSuccess()) {
		    final Channel chan = e.getChannel();
		    sw.setChannel(chan);
		    OpenVirteXController.this.cg.add(chan);
		} else {
		    OpenVirteXController.this.log.error(
			    "Failed to connect to controller {} for switch {}",
			    remoteAddr, sw.getSwitchId());
		}
	    }
	});
    }

    private void setServerBootStrapParams(final ServerBootstrap bootstrap) {
	bootstrap.setOption("reuseAddr", true);
	bootstrap.setOption("child.keepAlive", true);
	bootstrap.setOption("child.tcpNoDelay", true);
	bootstrap.setOption("child.sendBufferSize",
	        OpenVirteXController.SEND_BUFFER_SIZE);

    }

    private void setClientBootStrapParams(final ClientBootstrap bootstrap) {
	bootstrap.setOption("reuseAddr", true);
	bootstrap.setOption("child.keepAlive", true);
	bootstrap.setOption("child.tcpNoDelay", true);
	bootstrap.setOption("child.sendBufferSize",
	        OpenVirteXController.SEND_BUFFER_SIZE);

    }

    private ClientBootstrap createClientBootStrap() {
	return new ClientBootstrap(this.clientSockets);
    }

    private ServerBootstrap createServerBootStrap() {
	return new ServerBootstrap(new NioServerSocketChannelFactory(
	        Executors.newCachedThreadPool(),
	        Executors.newCachedThreadPool()));
    }

    private void startServer() {
	this.server = new APIServer();
	new Thread(this.server).start();
    }

    private void stopServer() {
	this.server.stop();
    }

    public void terminate() {
	if (this.cg != null && this.cg.close().awaitUninterruptibly(1000)) {
	    this.log.info("Shut down all controller connections. Quitting...");
	} else {
	    this.log.error("Error shutting down all controller connections. Quitting anyway.");
	}

	if (this.sg != null && this.sg.close().awaitUninterruptibly(1000)) {
	    this.log.info("Shut down all switch connections. Quitting...");
	} else {
	    this.log.error("Error shutting down all switch connections. Quitting anyway.");
	}

	if (this.pfact != null) {
	    this.pfact.releaseExternalResources();
	}
	if (this.cfact != null) {
	    this.cfact.releaseExternalResources();
	}

	this.stopServer();
    }

    public static OpenVirteXController getInstance() {
	if (OpenVirteXController.instance == null) {
	    throw new RuntimeException(
		    "The OpenVirtexController has not been initialized; quitting.");
	}
	return OpenVirteXController.instance;
    }

    /*
     * return the number of bits needed to encode the tenant id
     */
    public int getNumberVirtualNets() {
	return this.maxVirtual;
    }

    public void fromJson(final ArrayList<HashMap<String, Object>> list) {
	for (final HashMap<String, Object> row : list) {
	    this.ofHost = (String) row.get(OpenVirteXController.HOST);
	    this.ofPort = ((Double) row.get(OpenVirteXController.OFPORT))
		    .intValue();
	    this.apiPort = ((Double) row.get(OpenVirteXController.APIPORT))
		    .intValue();
	}
    }

    public HashMap<String, Object> toJson() {
	final HashMap<String, Object> output = new HashMap<String, Object>();
	final LinkedList<Object> list = new LinkedList<Object>();
	final HashMap<String, Object> ovxMap = new HashMap<String, Object>();
	ovxMap.put(OpenVirteXController.APIPORT, this.apiPort);
	ovxMap.put(OpenVirteXController.OFPORT, this.ofPort);
	ovxMap.put(OpenVirteXController.HOST, this.ofHost);

	list.add(ovxMap);
	output.put(OpenVirteXController.OPENVIRTEX, list);
	return output;
    }

    public static int getApiPort() {
	System.out.println("The port is:" + OpenVirteXController.apiPort);
	return OpenVirteXController.apiPort;
    }

}
