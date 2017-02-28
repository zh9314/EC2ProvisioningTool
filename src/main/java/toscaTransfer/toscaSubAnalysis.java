package toscaTransfer;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import Provisioning.Logger;
import java.io.FileInputStream;
import org.json.JSONException;

public class toscaSubAnalysis {

    public ArrayList<Node> nodes;
    public ArrayList<subnet> subnets;
    public ArrayList<Connection> connections;
    public String publicKeyPath = "null";
    public String userName = "null";
    public Logger swLog;
    public String[] supportDomains;

    public toscaSubAnalysis(Logger swLog, String[] SD) {
        this.swLog = swLog;
        this.supportDomains = SD;
    }

    private void completeEthInfo() {
        for (int i = 0; i < nodes.size(); i++) {
            Node tmpNode = nodes.get(i);
            boolean belong2subnet = false;      ////denote whether this node is belong to some subnet
            for (int j = 0; j < tmpNode.eths.size(); j++) {
                Eth tmpEth = tmpNode.eths.get(j);
                if (tmpEth.connectionOrSubnet) ////it's belong to a connection
                {
                    int pointIndex = tmpEth.connectionName.lastIndexOf(".");
                    String cName = tmpEth.connectionName.substring(0, pointIndex);
                    String sOrt = tmpEth.connectionName.substring(pointIndex + 1, tmpEth.connectionName.length());
                    for (int x = 0; x < connections.size(); x++) {
                        if (connections.get(x).connectionName.equals(cName)) {
                            if (sOrt.equals("source")) {
                                if (!tmpEth.ethName.equals(connections.get(x).source.ethName)) {
                                    System.out.println("The source port name of connection " + cName + " is conflicted!");
                                    System.exit(-1);
                                }
                                tmpEth.privateAddress = connections.get(x).source.address;
                                tmpEth.netmask = connections.get(x).source.netmask;
                                tmpEth.remoteNodeName = connections.get(x).target.nodeName;
                                tmpEth.remotePriAddress = connections.get(x).target.address;
                                tmpEth.remotePriNetmask = connections.get(x).target.netmask;
                            } else if (sOrt.equals("target")) {
                                if (!tmpEth.ethName.equals(connections.get(x).target.ethName)) {
                                    System.out.println("The target port name of connection " + cName + " is conflicted!");
                                    System.exit(-1);
                                }
                                tmpEth.privateAddress = connections.get(x).target.address;
                                tmpEth.netmask = connections.get(x).target.netmask;
                                tmpEth.remoteNodeName = connections.get(x).source.nodeName;
                                tmpEth.remotePriAddress = connections.get(x).source.address;
                                tmpEth.remotePriNetmask = connections.get(x).source.netmask;
                            } else {
                                System.out.println("Something wrong with the connection!");
                                System.exit(-1);
                            }
                            break;
                        }
                    }
                } else {      /////it's belong to a subnet
                    if (belong2subnet) {
                        System.out.println("EC2 node cannot belong to two subnets at same time");
                        System.exit(-1);
                    }
                    belong2subnet = true;
                    for (int x = 0; x < subnets.size(); x++) {
                        if (subnets.get(x).name.equals(tmpEth.subnetName)) {
                            tmpEth.netmask = subnets.get(x).netmask;
                            tmpEth.subnetAddress = subnets.get(x).subnet;
                        }
                    }
                }

            }
            tmpNode.belongToSubnet = belong2subnet;
            if (!belong2subnet) {
                Eth newEth = new Eth();
                newEth.connectionOrSubnet = false;  ////it's subnet
                newEth.subnetName = "default";
                newEth.ethName = "default";
                newEth.privateAddress = "null";    ////currently not be assigned
                tmpNode.eths.add(newEth);
            }

        }
    }

    public void generateInfrastructure(String toscaFilePath) {
        try {
            File file = new File(toscaFilePath);
//            YamlStream stream = Yaml.loadStream(file);
            org.yaml.snakeyaml.Yaml yaml = new org.yaml.snakeyaml.Yaml();
            Map<String, Object> hashMap = (Map<String, Object>) yaml.load(new FileInputStream(file));

            boolean find_conn = false;
//            for (Iterator iter = stream.iterator(); iter.hasNext();) {
//                HashMap hashMap = (HashMap) iter.next();
            for (Iterator iter2 = hashMap.entrySet().iterator(); iter2.hasNext();) {
                Map.Entry entry = (Map.Entry) iter2.next();
                Object key = entry.getKey();
                Object value = entry.getValue();
                String keyS = key.toString();
                String valueS = value.toString();
                String jsonValue = transfer2json(valueS);
                if (keyS.equals("subnets")) {
                    subnets = json2subnet(jsonValue);
                }
                if (keyS.equals("components")) {
                    nodes = json2node(jsonValue);
                }
                if (keyS.equals("connections")) {
                    connections = json2connection(jsonValue);
                    find_conn = true;
                }
                if (keyS.equals("publicKeyPath")) {
                    publicKeyPath = valueS;
                }
                if (keyS.equals("userName")) {
                    userName = valueS;
                    System.out.println("UserName: " + userName);
                }
            }
//            }
            if (!find_conn) {
                connections = new ArrayList<Connection>();
            }

            completeEthInfo();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String transfer2json(String x) {
        String y = x.replace("=", ":");
        char[] org = new char[y.length()];
        org = y.toCharArray();
        char[] target = new char[2 * y.length()];
        int target_i = 0;
        for (int i = 0; i < y.length(); i++) {
            target[target_i++] = org[i];
            if (i + 1 < y.length() && org[i] == ':' && org[i + 1] != '[' && org[i + 1] != '{') {
                target[target_i++] = '\'';
            }

            if (i + 1 < y.length() && org[i + 1] == ',') {
                if (org[i] == '}' || org[i] == ']')
					; else {
                    int j = i + 2;
                    boolean find_semicolon = false;
                    while (j < y.length() && org[j] != ',') {
                        if (org[j] == ':') {
                            find_semicolon = true;
                            break;
                        }
                        j++;
                    }
                    if (find_semicolon) {
                        target[target_i++] = '\'';
                    }
                }
            }

            if (i + 1 < y.length() && org[i] != '}' && org[i] != ']' && (org[i + 1] == '}' || org[i + 1] == ']')) {
                target[target_i++] = '\'';
            }
        }
        target[target_i] = 0;
        return new String(target);
    }

    private ArrayList<subnet> json2subnet(String jsonString) throws JSONException {
        ArrayList<subnet> linkSet = new ArrayList<subnet>();
        JSONArray jsonLinks = new JSONArray(jsonString);
        for (int i = 0; i < jsonLinks.length(); i++) {
            JSONObject jsonLink = jsonLinks.getJSONObject(i);
            subnet tmp = new subnet();
            tmp.name = jsonLink.getString("name");
            tmp.subnet = jsonLink.getString("subnet");
            tmp.netmask = jsonLink.getString("netmask");
            linkSet.add(tmp);
        }
        return linkSet;
    }

    private ArrayList<Connection> json2connection(String jsonString) throws JSONException {
        ArrayList<Connection> connectionSet = new ArrayList<Connection>();
        JSONArray jsonConnections = new JSONArray(jsonString);
        for (int i = 0; i < jsonConnections.length(); i++) {
            JSONObject jsonConnection = jsonConnections.getJSONObject(i);
            Connection tmp = new Connection();
            tmp.connectionName = jsonConnection.getString("name");
            JSONObject jsonSource = jsonConnection.getJSONObject("source");
            tmp.source.nodeName = jsonSource.getString("component_name");
            tmp.source.ethName = jsonSource.getString("port_name");
            tmp.source.netmask = jsonSource.getString("netmask");
            tmp.source.address = jsonSource.getString("address");
            JSONObject jsonTarget = jsonConnection.getJSONObject("target");
            tmp.target.nodeName = jsonTarget.getString("component_name");
            tmp.target.ethName = jsonTarget.getString("port_name");
            tmp.target.netmask = jsonTarget.getString("netmask");
            tmp.target.address = jsonTarget.getString("address");
            tmp.bandwidth = jsonConnection.getInt("bandwidth");
            tmp.latency = jsonConnection.getDouble("latency");
            connectionSet.add(tmp);
        }
        return connectionSet;
    }

    private boolean checkValidDomain(String domain) {
        for (int i = 0; i < supportDomains.length; i++) {
            if (supportDomains[i].equals(domain)) {
                return true;
            }
        }
        return false;
    }

    private ArrayList<Node> json2node(String jsonString) throws JSONException {
        ArrayList<Node> nodeSet = new ArrayList<Node>();
        JSONArray jsonNodes = new JSONArray(jsonString);
        for (int i = 0; i < jsonNodes.length(); i++) {
            JSONObject jsonNode = jsonNodes.getJSONObject(i);
            Node tmp = new Node();
            tmp.type = jsonNode.getString("type");
            tmp.nodeType = jsonNode.getString("nodetype");
            tmp.OStype = jsonNode.getString("OStype");
            tmp.nodeName = jsonNode.getString("name");
            tmp.domain = jsonNode.getString("domain");
            if (!checkValidDomain(tmp.domain)) {
                swLog.log("ERROR", "toscaSubAnalysis.json2node", "The private key of domain " + tmp.domain + " is not configurated!");
                System.exit(-1);
            }
            tmp.script = jsonNode.getString("script");
            tmp.installation = jsonNode.getString("installation");
            tmp.publicAddress = jsonNode.getString("public_address");
            if (jsonNode.has("ethernet_port")) {
                JSONArray jsonEths = jsonNode.getJSONArray("ethernet_port");
                for (int j = 0; j < jsonEths.length(); j++) {
                    Eth tmpEth = new Eth();
                    JSONObject jsonEth = jsonEths.getJSONObject(j);
                    tmpEth.ethName = jsonEth.getString("name");
                    if (jsonEth.has("connection_name") && jsonEth.has("subnet_name")) {
                        System.out.println("Format is wrong with both connection and subnet!");
                        swLog.log("ERROR", "toscaSubAnalysis.json2node", "Format is wrong with both connection and subnet!");
                        System.exit(-1);
                    }
                    if (!jsonEth.has("connection_name") && !jsonEth.has("subnet_name")) {
                        System.out.println("Format is wrong without any connection or subnet!");
                        swLog.log("ERROR", "toscaSubAnalysis.json2node", "Format is wrong without any connection or subnet!");
                        System.exit(-1);
                    }
                    if (jsonEth.has("connection_name")) {
                        tmpEth.connectionOrSubnet = true;
                        tmpEth.connectionName = jsonEth.getString("connection_name");
                    }
                    if (jsonEth.has("subnet_name")) {
                        tmpEth.connectionOrSubnet = false;
                        tmpEth.subnetName = jsonEth.getString("subnet_name");
                        tmpEth.privateAddress = jsonEth.getString("address");
                    }
                    tmp.eths.add(tmpEth);
                }
            }
            nodeSet.add(tmp);
        }
        return nodeSet;
    }

}
