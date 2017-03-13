package Provisioning;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import Configuration.SoftwareConf;
import Configuration.SshConf;
import Configuration.TunnelConf;
import toscaTransfer.Connection;
import toscaTransfer.Eth;
import toscaTransfer.Node;
import toscaTransfer.subnet;
import toscaTransfer.toscaSubAnalysis;

public class ARP {

    private static EC2Agent ec2agent;

    private static String sharedSubnetVPCID = "null";  ///This is shared subnet for all the instances without subnet defined.
    private static String sharedSubnetRTID = "null";  ////route table id
    private static String sharedSubnetIGID = "null";  ////internet gateway id
    private static String sharedSubnetSGID = "null";  /////security group id

    private static Logger swLog;
    public static String currentDir = ""; ///identify current directory of the jar file.

    private static ArrayList<String> instanceIds = new ArrayList<String>();

    private static Map<String, String> endpointMap = new HashMap<String, String>();
    /*static {
		Map<String, String> em = new HashMap<String, String>();
		em.put("ec2.us-east-1.amazonaws.com", "Virginia");
		em.put("ec2.us-west-1.amazonaws.com", "California");
		em.put("ec2.us-west-2.amazonaws.com", "Oregon");
		em.put("ec2.ap-south-1.amazonaws.com", "Mumbai");
		em.put("ec2.ap-southeast-1.amazonaws.com", "Singapore");
		em.put("ec2.ap-northeast-2.amazonaws.com", "Seoul");
		em.put("ec2.ap-southeast-2.amazonaws.com", "Sydney");
		em.put("ec2.ap-northeast-1.amazonaws.com", "Tokyo");
		em.put("ec2.eu-central-1.amazonaws.com", "Frankfurt");
		em.put("ec2.eu-west-1.amazonaws.com", "Ireland");
		em.put("ec2.sa-east-1.amazonaws.com", "Paulo");

		endpointMap = Collections.unmodifiableMap(em);
	}*/

    private static Map<String, Map<String, String>> OS_DM_AMI = new HashMap<String, Map<String, String>>();
    /*private static final Map<String, String> ubuntu14_TypeMap;
	static {
		Map<String, String> is = new HashMap<String, String>();
		Map<String, Map<String, String>> im = new HashMap<String, Map<String, String>>();
		im.put("ec2.us-east-1.amazonaws.com", "ami-2d39803a");
		im.put("ec2.us-west-1.amazonaws.com", "ami-48db9d28");

		ubuntu14_TypeMap = Collections.unmodifiableMap(im);
	}*/

    // converting to netmask
    private static final String[] netmaskConverter = {
        "128.0.0.0", "192.0.0.0", "224.0.0.0", "240.0.0.0", "248.0.0.0", "252.0.0.0", "254.0.0.0", "255.0.0.0",
        "255.128.0.0", "255.192.0.0", "255.224.0.0", "255.240.0.0", "255.248.0.0", "255.252.0.0", "255.254.0.0", "255.255.0.0",
        "255.255.128.0", "255.255.192.0", "255.255.224.0", "255.255.240.0", "255.255.248.0", "255.255.252.0", "255.255.254.0", "255.255.255.0",
        "255.255.255.128", "255.255.255.192", "255.255.255.224", "255.255.255.240", "255.255.255.248", "255.255.255.252", "255.255.255.254", "255.255.255.255"
    };

    /**
     * Convert netmask int to string (255.255.255.0 returned if nm > 32 or nm <
     * 1) @
     *
     *
     * param nm @return
     */
    public static String netmaskIntToString(int nm) {
        if ((nm > 32) || (nm < 1)) {
            return "255.255.255.0";
        } else {
            return netmaskConverter[nm - 1];
        }
    }

    /**
     * Convert netmask string to an integer (24-bit returned if no match)
     *
     * @param nm
     * @return
     */
    public static int netmaskStringToInt(String nm) {
        int i = 1;
        for (String s : netmaskConverter) {
            if (s.equals(nm)) {
                return i;
            }
            i++;
        }
        return 24;
    }

    private static void setupNodesWithoutSubnet(ArrayList<Node> nodes, ArrayList<Connection> connections) {
        int subnetIndex = -1;
        for (int i = 0; i < nodes.size(); i++) {
            if (nodes.get(i).belongToSubnet) {
                continue;
            }
            subnetIndex++;
            Node tmpNode = nodes.get(i);
            String vpcCIDR = "192.168.0.0/16";
            if (sharedSubnetVPCID.equals("null")) {
                sharedSubnetVPCID = ec2agent.createVPC(vpcCIDR);
            }
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            if (sharedSubnetRTID.equals("null")) {
                sharedSubnetRTID = ec2agent.getAssociateRouteTableId(sharedSubnetVPCID);
                sharedSubnetIGID = ec2agent.createInternetGateway(sharedSubnetVPCID);
            }

            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            ec2agent.createRouteToGate(sharedSubnetRTID, sharedSubnetIGID, "0.0.0.0/0");
            ec2agent.enableVpcDNSHostName(sharedSubnetVPCID);
            String subnetAddress = "192.168." + subnetIndex + ".0";
            String subnetCIDR = subnetAddress + "/24";
            String subnetId = ec2agent.createSubnet(sharedSubnetVPCID, subnetCIDR);
            ec2agent.enableMapPubAddress(subnetId);
            if (sharedSubnetSGID.equals("null")) {
                sharedSubnetSGID = ec2agent.createBasicSecurityGroup(sharedSubnetVPCID, "AllTrafficGroup", "AllTraffic");
            }
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            String privateAddress = "192.168." + subnetIndex + ".10";
            String instanceType = tmpNode.nodeType;
            for (int k = 0; k < tmpNode.eths.size(); k++) {
                if (!tmpNode.eths.get(k).connectionOrSubnet) {
                    tmpNode.eths.get(k).privateAddress = privateAddress;
                    tmpNode.eths.get(k).subnetAddress = subnetAddress;
                    tmpNode.eths.get(k).netmask = "255.255.255.0";
                }
            }

            String OStype = tmpNode.OStype.toLowerCase();
            String imageType = "";
            if (!OS_DM_AMI.containsKey(OStype.toLowerCase())) {
                swLog.log("ERROR", "ARP.setupNodesWithoutSubnet", "Currently do not support the OS type of " + OStype);
                System.exit(1);
            }
            imageType = OS_DM_AMI.get(OStype.toLowerCase()).get(nodes.get(i).domain);
            String instanceId = ec2agent.runInstance(subnetId, sharedSubnetSGID, imageType,
                    privateAddress, instanceType, "testKey");
            instanceIds.add(instanceId);
            tmpNode.instanceID = instanceId;
            System.out.println("Node " + tmpNode.nodeName + " is setup. InstanceID: " + instanceId);

        }
    }

    private static void setupNodesWithSubnet(ArrayList<subnet> subnets, ArrayList<Node> nodes, ArrayList<Connection> connections) {

        for (int i = 0; i < nodes.size(); i++) {
            Node tmpNode = nodes.get(i);
            if (!tmpNode.belongToSubnet) {
                continue;
            }

            String netmask = "null";
            String subnetAddress = "null";
            String privateAddress = "null";
            for (int j = 0; j < tmpNode.eths.size(); j++) {
                Eth tmpEth = tmpNode.eths.get(j);
                if (!tmpEth.connectionOrSubnet) {
                    netmask = tmpEth.netmask;
                    subnetAddress = tmpEth.subnetAddress;
                    privateAddress = tmpEth.privateAddress;
                    break;
                }
            }
            if (netmask.equals("null") || subnetAddress.equals("null") || privateAddress.equals("null")) {
                System.out.println("Unexpected error!");
                swLog.log("ERROR", "ARP.setupNodesWithSubnet", "Unexpected error!EXIT!");
                System.exit(-1);
            }
            int netmaskNum = 0;
            if (netmask.contains(".")) {
                netmaskNum = netmaskStringToInt(netmask);
            } else {
                netmaskNum = Integer.valueOf(netmask);
            }
            String vpcCIDR = subnetAddress + "/" + netmaskNum;
            String vpcId = ec2agent.createVPC(vpcCIDR);
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            String routeTableId = ec2agent.getAssociateRouteTableId(vpcId);
            String internetGatewayId = ec2agent.createInternetGateway(vpcId);
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            ec2agent.createRouteToGate(routeTableId, internetGatewayId, "0.0.0.0/0");
            ec2agent.enableVpcDNSHostName(vpcId);
            String subnetId = ec2agent.createSubnet(vpcId, vpcCIDR);
            ec2agent.enableMapPubAddress(subnetId);
            String securityGroupId = ec2agent.createBasicSecurityGroup(vpcId, "AllTrafficGroup", "AllTraffic");
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            String instanceType = tmpNode.nodeType;
            String OStype = tmpNode.OStype;
            String imageType = "";
            if (!OS_DM_AMI.containsKey(OStype.toLowerCase())) {
                swLog.log("ERROR", "ARP.setupNodesWithSubnet", "Currently do not support the OS type of " + OStype);
                System.exit(1);
            }
            imageType = OS_DM_AMI.get(OStype.toLowerCase()).get(nodes.get(i).domain);

            String instanceId = ec2agent.runInstance(subnetId, securityGroupId, imageType,
                    privateAddress, instanceType, "testKey");
            instanceIds.add(instanceId);
            tmpNode.instanceID = instanceId;
            System.out.println("Node " + tmpNode.nodeName + " is setup. InstanceID: " + instanceId);
            swLog.log("INFO", "ARP.setupNodesWithSubnet", "Node " + tmpNode.nodeName + " is setup. InstanceID: " + instanceId);
        }
    }

    private static void completeRemotePubInfo(ArrayList<Node> nodes) {
        for (int i = 0; i < nodes.size(); i++) {
            Node tmpNode = nodes.get(i);
            for (int j = 0; j < tmpNode.eths.size(); j++) {
                Eth tmpEth = tmpNode.eths.get(j);
                if (!tmpEth.netmask.contains(".")) {   ////Uniform the format of netmask
                    int numNetmask = Integer.parseInt(tmpEth.netmask);
                    tmpEth.netmask = netmaskIntToString(numNetmask);
                    tmpEth.netmaskNum = numNetmask;
                } else {
                    tmpEth.netmaskNum = netmaskStringToInt(tmpEth.netmask);
                }

                if (tmpEth.connectionOrSubnet) {
                    String remoteNode = tmpEth.remoteNodeName;
                    for (int k = 0; k < nodes.size(); k++) {
                        if (remoteNode.equals(nodes.get(k).nodeName)) {
                            tmpEth.remotePubAddress = nodes.get(k).publicAddress;
                            System.out.println("Got PubAddress \"" + tmpEth.remotePubAddress + "\" of " + tmpEth.remoteNodeName);
                            swLog.log("INFO", "ARP.completeRemotePubInfo",
                                    "Got PubAddress \"" + tmpEth.remotePubAddress + "\" of " + tmpEth.remoteNodeName);
                            break;
                        }
                    }
                }
            }
        }
    }

    public static void setupInfrastructure(ArrayList<subnet> subnets, ArrayList<Node> nodes,
            ArrayList<Connection> connections, String toscaFilePath, String userName, String pubKeyPath, String certDir) {
        String endpoint = nodes.get(0).domain;
        String certPath = certDir + endpointMap.get(nodes.get(0).domain) + ".pem";
        ec2agent.setEndpoint(endpoint);
        setupNodesWithoutSubnet(nodes, connections);
        setupNodesWithSubnet(subnets, nodes, connections);

        try {
            Thread.sleep(nodes.size() * 10000);
        } catch (InterruptedException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        ArrayList<String> PubAddress_instance = ec2agent.getPriPubAddressPair(instanceIds);
        for (int i = 0; i < PubAddress_instance.size(); i++) {
            System.out.println(PubAddress_instance.get(i));
        }
        ec2agent.waitValid(PubAddress_instance);

        for (int i = 0; i < PubAddress_instance.size(); i++) {
            String[] addresses = PubAddress_instance.get(i).split(":");
            String pubAddress = addresses[0];
            String instanceId = addresses[1];
            for (int j = 0; j < nodes.size(); j++) {
                if (nodes.get(j).instanceID.equals(instanceId)) {
                    nodes.get(j).publicAddress = pubAddress;
                    break;
                }
            }
        }

        /////generate the output TOSCA file
        int point = toscaFilePath.lastIndexOf(".");
        System.out.println("Generate output!");
        String newToscaFilePath = toscaFilePath.substring(0, point) + "_provisioned" + toscaFilePath.substring(point);
        File orgToscaFile = new File(toscaFilePath);
        try {
            BufferedReader in = new BufferedReader(new FileReader(orgToscaFile));
            FileWriter fw = new FileWriter(newToscaFilePath, false);
            String line = null;
            while ((line = in.readLine()) != null) {
                if (line.contains("public_address:")) {
                    int start = line.indexOf("public_address:");
                    String spaceString = line.substring(0, start);
                    int begin = line.indexOf(':');
                    String nodeName = line.substring(begin + 1).trim();
                    for (int i = 0; i < nodes.size(); i++) {
                        if (nodes.get(i).nodeName.equals(nodeName)) {
                            fw.write(spaceString + "public_address: " + nodes.get(i).publicAddress + "\n");
                            String emptyString = spaceString;
                            if (spaceString.contains("-")) {
                                emptyString = spaceString.replace('-', ' ');
                            }
                            fw.write(emptyString + "instanceId: " + nodes.get(i).instanceID + "\n");
                            break;
                        }
                    }
                } else {
                    fw.write(line + "\n");
                }
            }

            fw.close();
            in.close();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        try {
            Thread.sleep(8000);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        /////completing the task remote connection information
        completeRemotePubInfo(nodes);

        ///install the software
        for (int i = 0; i < nodes.size(); i++) {
            Node tmpNode = nodes.get(i);
            String scriptPath = tmpNode.script;
            String installDir = tmpNode.installation;
            String pubAddress = tmpNode.publicAddress;
            String location = endpointMap.get(nodes.get(i).domain);
            String OStype = tmpNode.OStype;

            System.out.println("OS: " + OStype);
            SshConf sshConf = new SshConf(certDir, pubAddress, location, userName, pubKeyPath, swLog);
            //sshConf.firstConnect();

            if (pubKeyPath != (null)) {
                sshConf.confUserSSH(OStype);
            }

            SoftwareConf softConf = new SoftwareConf(certDir, scriptPath, installDir,
                    pubAddress, location, userName, swLog);
            softConf.installSofware(OStype);

            TunnelConf tunnelConf = new TunnelConf(certDir, tmpNode, location, swLog);
            String confName = tunnelConf.generateConfFile();
            tunnelConf.runConf(confName);

        }

    }

    ///There are two kinds of files in database.
    ///domains: domainNmae&&domainID
    ///OS_Domain_AMI: OStype&&domainID&&AMI  
    private static void loadInfoFromDB(String databaseDir) {
        String infoPath = databaseDir + "domains";
        File info = new File(infoPath);
        try {
            BufferedReader in = new BufferedReader(new FileReader(info));
            String line = null;
            while ((line = in.readLine()) != null) {
                String[] domainNameID = line.split("&&");
                endpointMap.put(domainNameID[1].trim(), domainNameID[0].trim());
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        infoPath = databaseDir + "OS_Domain_AMI";
        info = new File(infoPath);
        try {
            BufferedReader in = new BufferedReader(new FileReader(info));
            String line = null;
            while ((line = in.readLine()) != null) {
                String[] OS_DM_AMI_String = line.split("&&");
                String OS = OS_DM_AMI_String[0].trim().toLowerCase();
                String DM = OS_DM_AMI_String[1].trim();
                String AMI = OS_DM_AMI_String[2].trim();
                if (OS_DM_AMI.containsKey(OS)) {
                    Map<String, String> DM_AMI = OS_DM_AMI.get(OS);
                    DM_AMI.put(DM, AMI);
                } else {
                    Map<String, String> DM_AMI = new HashMap<String, String>();
                    DM_AMI.put(DM, AMI);
                    OS_DM_AMI.put(OS, DM_AMI);
                }
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    ////absolute path of current directory
    private static String getCurrentDir() {
        String curDir = new ARP().getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
        int index = curDir.lastIndexOf('/');
        return curDir.substring(0, index + 1);
    }

    ////args[0]: configuration file path. args[1]: topology file path
    public static void main(String[] args) {
        // TODO Auto-generated method stub
        currentDir = getCurrentDir();
        ConfLoader confLoader = new ConfLoader(args[0]);
        confLoader.loadConfiguration(currentDir);
        swLog = new Logger(confLoader.LogsDir + "ec2.log");
        swLog.log("INFO", "ARP.main", "Create topology from " + args[1]);
        loadInfoFromDB(confLoader.DatabaseDir);
        ec2agent = new EC2Agent(confLoader.AWSAccessKeyId, confLoader.AWSSecretKey, swLog);
        toscaSubAnalysis tsa = new toscaSubAnalysis(swLog, confLoader.SupportDomains);
        String tf = args[1];
        tsa.generateInfrastructure(tf);
        setupInfrastructure(tsa.subnets, tsa.nodes, tsa.connections, tf, tsa.userName, tsa.publicKeyPath, confLoader.KeyDir);
        swLog.closeLog();

    }

}
