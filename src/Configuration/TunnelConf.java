package Configuration;

import java.io.FileWriter;
import java.io.IOException;

import Provisioning.ARP;
import toscaTransfer.Eth;
import toscaTransfer.Node;


public class TunnelConf {
	
	private Node info = new Node();
	private String location = "";
	private String certDir = "";
	
	private String sshOption = "-o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null";
	
	
	public TunnelConf(String cd, Node curNode, String lo){
		info = curNode;
		location = lo;
		certDir = cd;
	}
	
	private String getSubnet(int netmaskNum, String privateAddress){
		String subnet = "";
		String [] subPriAddress = privateAddress.split("\\.");
		String combineAddress = "";
		for(int i = 0 ; i<subPriAddress.length ; i++){
			int subAddNum = Integer.valueOf(subPriAddress[i]);
			String bString = Integer.toBinaryString(subAddNum);
			int len = 8 - bString.length();
			for(int j = 0 ; j<len ; j++)
				bString = "0"+bString;
			combineAddress += bString;
		}
		String binarySubnet = combineAddress.substring(0, netmaskNum);
		for(int i = 0 ; i<(32-netmaskNum) ; i++)
			binarySubnet += "0";
		
		for(int i = 0 ; i<4 ; i++){
			String nums = binarySubnet.substring(i*8, i*8+8);
			int num = Integer.parseInt(nums, 2);
			if(i == 0)
				subnet = num+"";
			else
				subnet += "."+num;
		}
		
		return subnet;
	}
	
	public String generateConfFile(){
		java.util.Calendar cal = java.util.Calendar.getInstance();
		long currentMili = cal.getTimeInMillis();
		String confFileName = info.nodeName+"_ec2_conf_"+currentMili+".sh";
		String confFilePath = ARP.currentDir+confFileName;
		String subnetOfEth0 = "null", netmaskOfEth0 = "null", privateAddressOfEth0 = "null";
		for(int i = 0 ; i<info.eths.size() ; i++){
			Eth tmpEth = info.eths.get(i);
			if(!tmpEth.connectionOrSubnet){
				subnetOfEth0 = tmpEth.subnetAddress;
				netmaskOfEth0 = tmpEth.netmask;
				privateAddressOfEth0 = tmpEth.privateAddress;
				break;
			}
		}
		
		if(info.OStype.toLowerCase().contains("ubuntu")){
		try{
		FileWriter fw = new FileWriter(confFilePath, false);
		fw.write("route del -net "+subnetOfEth0+" netmask "+netmaskOfEth0+" dev eth0\n");
		
		for(int i = 0 ; i<info.eths.size() ; i++){
			Eth tmpEth = info.eths.get(i);
			if(tmpEth.connectionOrSubnet){
				//int lastIndex = tmpEth.connectionName.lastIndexOf('.');
				//String linkName = tmpEth.connectionName.substring(0, lastIndex);
				String linkName = tmpEth.ethName;
				String remotePubAddress = tmpEth.remotePubAddress;
				String tunnelLocalAddress = privateAddressOfEth0;
				String localPrivateAddress = tmpEth.privateAddress;
				String netmask = tmpEth.netmask;
				String remotePrivateAddress = tmpEth.remotePriAddress;
				String subnet = getSubnet(tmpEth.netmaskNum, localPrivateAddress);
				fw.write("ip tunnel add "+linkName+" mode ipip remote "+remotePubAddress+" local "+tunnelLocalAddress+"\n");
				fw.write("ifconfig "+linkName+" "+localPrivateAddress+" netmask "+netmask+"\n");
				fw.write("route del -net "+subnet+" netmask "+netmask+" dev "+linkName+"\n");
				fw.write("route add -host "+remotePrivateAddress+" dev "+linkName+"\n");
				
			}
		}
		fw.close();
		
		}catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		}
		
		
		
		return confFileName;
	}
	
	public void runConf(String confName){
		try {
			String prefixOS = "";
			if(info.OStype.toLowerCase().contains("ubuntu"))
				prefixOS = "ubuntu";
			else{
				System.out.println("Unknown OS for "+info.OStype);
				System.exit(-1);
			}
			
			java.util.Calendar cal = java.util.Calendar.getInstance();
			long currentMili = cal.getTimeInMillis();
			String confPath = ARP.currentDir+confName;
			String runFile = "run_ec2_"+info.nodeName+"_"+currentMili+".sh";
			String runFilePath = ARP.currentDir+runFile;
			FileWriter fw = new FileWriter(runFilePath, false);
			fw.write("scp -i "+certDir+location+".pem "+sshOption+" "+confPath+" "+prefixOS+"@"+info.publicAddress+":~/\n");
		    fw.write("ssh -i "+certDir+location+".pem "+sshOption+" "+prefixOS+"@"+info.publicAddress+" \"sudo ./"+confName+" 0</dev/null 1>/dev/null 2>/dev/null\"\n");
		    fw.write("ssh -i "+certDir+location+".pem "+sshOption+" "+prefixOS+"@"+info.publicAddress+" \"sudo rm ./"+confName+"\"\n");
		    
		    fw.close();
			
	        Process ps = Runtime.getRuntime().exec("chmod +x "+confPath);  
			ps.waitFor();
			ps = Runtime.getRuntime().exec("chmod +x "+runFilePath);  
			ps.waitFor();
	        ps = Runtime.getRuntime().exec("sh "+runFilePath);  
			ps.waitFor();
			ps = Runtime.getRuntime().exec("rm "+runFilePath+" "+confPath);  
			ps.waitFor();
			System.out.println("Configuration for node "+info.nodeName+" is done!");
		} catch (IOException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
