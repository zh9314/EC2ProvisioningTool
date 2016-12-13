package toscaTransfer;

public class Eth {
	public String ethName;
	public boolean connectionOrSubnet;   ////true denotes this eth belongs to connection, otherwise it belongs to subnet
	
	public String connectionName;
	public String remoteNodeName = "null";
	public String remotePubAddress = "null";/////It's valid when it's a connection. The target address of the tunnel
	public String remotePriAddress = "null";
	public String remotePriNetmask = "null";
	
	public String subnetName;
	public String subnetAddress;
	
	public String privateAddress;
	public String netmask;
	public int netmaskNum;

}
