package toscaTransfer;

public class Connection {
	
	public class point{
		public String nodeName;
		public String ethName;
		public String netmask;
		public String address;
	}

	public String connectionName;
	public point source = new point();
	public point target = new point();
	public int bandwidth;
	public double latency;
}
