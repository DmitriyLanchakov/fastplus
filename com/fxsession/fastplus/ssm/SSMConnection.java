package com.fxsession.fastplus.ssm;

/**
 * @author Dmitry Vulf
 * Implements Connection. 
 * implements only close logic that can be actually placed to SSMEndpoint     
 *
 */

import java.io.IOException;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.nio.channels.DatagramChannel;

import org.openfast.session.Connection;
import org.apache.log4j.Logger;
public class SSMConnection implements Connection {
	static public final String GROUP_IP = "groupIp";
	static public final String INTERFACE_IP ="interfaceIp";
	static public final String PORT_N="port";
	static public final String TEMPLATE_FILE ="templateFileName";
	static public final String TRACE_DECODED = "traceDecoded";
	static public final String TRACE_DECODED_FILE = "traceDecodedFile";
	static public final String TRACE_RAW = "traceRaw";
	static public final String TRACE_RAW_FILE = "traceRawFile";
	static public final String CONNS_NODE = "connection";
	
	private static Logger logger = Logger.getLogger(SSMConnection.class);
	
    protected DatagramChannel datachannel;
    protected int port;
    protected InetAddress group;
    protected InetAddress ifaddr;

 
	
    public SSMConnection(DatagramChannel dc, int port, InetAddress group, InetAddress ifaddr) {
        this.datachannel = dc;
        this.port = port;
        this.group = group;
    }
    
    public void close() {
        try {
            datachannel.close();
            logger.info("Closed connection to group");
        }
        catch (IOException e) {
            logger.error("Failed to close connection to the group ",e);
        }
    }

    public InputStream getInputStream() throws IOException {
        return new SSMInputStream(datachannel);
    }

    public OutputStream getOutputStream() throws IOException {
        return null;
    }
    
	/**
	 * To simplify reading from connection structure
	 */
    public static String 	readConnectionElementA(String elementName){
    	return SSMParameters.getInstance().readElement(CONNS_NODE,SSMParameters.SITE_A,elementName);
    }


}
