package Configuration;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

import Provisioning.ARP;

public class SoftwareConf {
	private String scriptPath = "";
	private String installDir = "";
	private String pubAddress = "";
	private String location = "";
	private String userName = "";
	private String certDir = "";
	
	private String sshOption = "-o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null";
	
	
	public SoftwareConf(String cd, String sp, String id, String pa, String lo, String un){
		scriptPath = sp;
		installDir = id;
		pubAddress = pa;
		location = lo;
		userName = un;
		certDir = cd;
	}
	
	public void installSofware(String OStype){
		if(scriptPath.equals("null")){
			System.out.println("Nothing needs to be installed!");
			return;
		}
		try{
			int lastIndex = scriptPath.lastIndexOf("/");
			String scriptName = scriptPath.substring(lastIndex+1);
			String dirPath = installDir;
			if(installDir.charAt(installDir.length()-1) == '/')
				dirPath = installDir.substring(0, installDir.length()-1);
			int lastIndexDir = dirPath.lastIndexOf('/');
			String dirName = dirPath.substring(lastIndexDir+1);
			if(OStype.toLowerCase().contains("ubuntu")){
				Process ps = Runtime.getRuntime().exec("chmod +x "+scriptPath);  
				ps.waitFor();
			    
				java.util.Calendar cal = java.util.Calendar.getInstance();
				long currentMili = cal.getTimeInMillis();
				String runFilePath = ARP.currentDir+"ec2_run_"+currentMili+".sh";
			    FileWriter fw = new FileWriter(runFilePath, false);
			    fw.write("scp -i "+certDir+location+".pem "+sshOption+" "+scriptPath+" ubuntu@"+pubAddress+":~/\n");
			    fw.write("ssh -i "+certDir+location+".pem "+sshOption+"  ubuntu@"+pubAddress+" \"sudo chmod +x "+scriptName+"\" \n");
			    if(!userName.equals("null"))
			    	fw.write("ssh -i "+certDir+location+".pem "+sshOption+" ubuntu@"+pubAddress+" \"sudo mv /home/ubuntu/"+scriptName+" /home/"+userName+"/ \" \n");
			    if(!installDir.equals("null")){
			    	fw.write("scp -i "+certDir+location+".pem -r "+sshOption+" "+installDir+" ubuntu@"+pubAddress+":~/\n");
			    	if(!userName.equals("null"))
			    		fw.write("ssh -i "+certDir+location+".pem "+sshOption+" ubuntu@"+pubAddress+" \"sudo mv /home/ubuntu/"+dirName+"/ /home/"+userName+"/\" \n");
			    }
			    if(!userName.equals("null"))
			    	fw.write("ssh -i "+certDir+location+".pem "+sshOption+" ubuntu@"+pubAddress+" \"sudo sh /home/"+userName+"/"+scriptName+" 0</dev/null 1>/dev/null 2>/dev/null\"\n");
			    else
			    	fw.write("ssh -i "+certDir+location+".pem "+sshOption+" ubuntu@"+pubAddress+" \"sudo ./"+scriptName+" 0</dev/null 1>/dev/null 2>/dev/null\"\n");
			    fw.close();
			        
			    
			    ps = Runtime.getRuntime().exec("chmod +x "+runFilePath);  
				ps.waitFor();
				
				System.out.println("start "+runFilePath);
				ps = Runtime.getRuntime().exec("sh "+runFilePath);  
				ps.waitFor();
				
				System.out.println("end "+runFilePath);
				
				Thread.sleep(20000);
				
				ps = Runtime.getRuntime().exec("rm "+runFilePath);  
		        ps.waitFor();
			}
			} catch (IOException | InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	}
	

}
