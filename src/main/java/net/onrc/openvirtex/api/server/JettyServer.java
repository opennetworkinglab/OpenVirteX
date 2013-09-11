package net.onrc.openvirtex.api.server;


import java.io.File;

import net.onrc.openvirtex.api.JSONRPCAPI;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.LoginService;
import org.eclipse.jetty.security.authentication.BasicAuthenticator;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.util.security.Constraint;
import org.eclipse.jetty.util.ssl.SslContextFactory;





public class JettyServer implements Runnable {
	
	private static Logger log = LogManager.getLogger(JettyServer.class.getName());
	
	public static String REALM_NAME = "OVXREALM";
	
	private JSONRPCAPI service = null;
	private Server server = null;
	
	public JettyServer(int port){
		service = new JSONRPCAPI();
		init(port);
	}
	
	private void init(int port) {
		log.info("Initializing API WebServer on port {}", port);
		server = new Server(port);
		
		
		String sslKeyStore = System.getProperty("javax.net.ssl.keyStore");;
		if (sslKeyStore == null) {
			throw new RuntimeException(
			"Property javax.net.ssl.keyStore not defined; missing keystore file : Use startup script to start OVX");
		}
		if (!(new File(sslKeyStore)).exists())
			throw new RuntimeException("SSL Key Store file not found: '"
					+ sslKeyStore
					+ " make sure you installed OVX correctly : see Installation manual");
		
		
	    // HTTP Configuration
        HttpConfiguration http_config = new HttpConfiguration();
        http_config.setSecureScheme("https");
        http_config.setSecurePort(8443);
        http_config.setOutputBufferSize(32768);

        // HTTP connector
        ServerConnector http = new ServerConnector(server,new HttpConnectionFactory(http_config));        
        http.setPort(port);
        http.setIdleTimeout(30000);
        
        // SSL Context Factory for HTTPS and SPDY
        SslContextFactory sslContextFactory = new SslContextFactory();
        sslContextFactory.setKeyStorePath(sslKeyStore);
        sslContextFactory.setKeyStorePassword("OBF:1lbw1wg41sox1kfx1vub1w8t1idn1zer1zej1igj1w8x1vuz1kch1sot1wfu1lfm");
        sslContextFactory.setKeyManagerPassword("OBF:1ym71u2g1uh61l8h1l4t1ugk1u2u1ym7");
        

        // HTTPS Configuration
        HttpConfiguration https_config = new HttpConfiguration(http_config);
        https_config.addCustomizer(new SecureRequestCustomizer());

        // HTTPS connector
        ServerConnector https = new ServerConnector(server,
            new SslConnectionFactory(sslContextFactory,"http/1.1"),
            new HttpConnectionFactory(https_config));
        https.setPort(8443);
        https.setIdleTimeout(500000);

        // Set the connectors
        server.setConnectors(new Connector[] { http, https });
        
        Constraint user_c = new Constraint();
        user_c.setName(Constraint.__BASIC_AUTH);;
        user_c.setRoles(new String[]{"user"});
        user_c.setAuthenticate(true);
        
        Constraint admin_c = new Constraint();
        admin_c.setName(Constraint.__BASIC_AUTH);;
        admin_c.setRoles(new String[]{"admin"});
        admin_c.setAuthenticate(true);
        
        Constraint ui_c = new Constraint();
        ui_c.setName(Constraint.__BASIC_AUTH);;
        ui_c.setRoles(new String[]{"ui"});
        ui_c.setAuthenticate(true);
        
        ConstraintMapping usermapping = new ConstraintMapping();
        usermapping.setConstraint(user_c);
        usermapping.setPathSpec("/tenant");
        
        ConstraintMapping adminmapping = new ConstraintMapping();
        adminmapping.setConstraint(admin_c);
        adminmapping.setPathSpec("/admin");
        
        ConstraintMapping uimapping = new ConstraintMapping();
        uimapping.setConstraint(ui_c);
        uimapping.setPathSpec("/ui");
        
        ConstraintSecurityHandler sh = new ConstraintSecurityHandler();
        sh.setRealmName(JettyServer.REALM_NAME);
        sh.setConstraintMappings(new ConstraintMapping[]{usermapping, adminmapping, uimapping});
        sh.setAuthenticator(new BasicAuthenticator());
        sh.setHandler(service);
        LoginService loginSrv = new OVXLoginService();
        sh.setLoginService(loginSrv);
        
        
   
        server.setHandler(sh);
	}
	

	@Override
	public void run() {
		try {
			server.start();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			server.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();

		}
		
	}
	

}
