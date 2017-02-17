package toscaTransfer;

import java.util.ArrayList;


public class Node {
	
	public String type;
	public String nodeType;
	public String nodeName;
	public boolean belongToSubnet;
	public String OStype;
	public String domain;
	public String script;
	public String installation;
	public String publicAddress;
	public String instanceID;
	
	public boolean activeState = false;
	
	public ArrayList<Eth> eths = new ArrayList<Eth>();

}
