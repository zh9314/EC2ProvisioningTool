package Provisioning;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

public class ConfLoader {

    private String confFilePath;
    public String AWSAccessKeyId = "";
    public String AWSSecretKey = "";
    public String KeyDir = "";   /////The dir stores all private keys to access domains.
    private String SupportDomainString = "";
    public String[] SupportDomains;
    public String DatabaseDir = "";
    public String LogsDir = "";

    public ConfLoader(String confFilePath) {
        this.confFilePath = confFilePath;
    }

    public boolean loadConfiguration(String currentDir) {
        File conf = new File(confFilePath);
        try {
            BufferedReader in = new BufferedReader(new FileReader(conf));
//            String line = null;
            Properties porp = new Properties();
            porp.load(in);
            AWSAccessKeyId = porp.getProperty("AWSAccessKeyId");
            AWSSecretKey = porp.getProperty("AWSSecretKey");
            KeyDir = porp.getProperty("KeyDir");
            SupportDomainString = porp.getProperty("SupportDomains");
            DatabaseDir = porp.getProperty("DatabaseDir", currentDir + "database/EC2/");
            LogsDir = porp.getProperty("LogsDir", System.getProperty("java.io.tmpdir"));

//            while ((line = in.readLine()) != null) {
//                String[] cmd = line.split("=");
//                if (cmd[0].trim().toLowerCase().equals("awsaccesskeyid")) {
//                    AWSAccessKeyId = cmd[1];
//                }
//                if (cmd[0].trim().toLowerCase().equals("awssecretkey")) {
//                    AWSSecretKey = cmd[1];
//                }
//                if (cmd[0].trim().toLowerCase().equals("keydir")) {
//                    KeyDir = cmd[1];
//                }
//                if (cmd[0].trim().toLowerCase().equals("supportdomains")) {
//                    SupportDomainString = cmd[1];
//                }
//                if (cmd[0].trim().toLowerCase().equals("databasedir")) {
//                    DatabaseDir = cmd[1];
//                }
//                if (cmd[0].trim().toLowerCase().equals("logsdir")) {
//                    LogsDir = cmd[1];
//                }
//            }
            if (AWSAccessKeyId.equals("") || AWSSecretKey.equals("")
                    || KeyDir.equals("") || SupportDomainString.equals("")
                    || LogsDir.equals("")) {
                return false;
            }
            KeyDir = rephaseTheDir(KeyDir);
//            if (DatabaseDir.equals("")) {
//                DatabaseDir = currentDir + "database/EC2/";
//            } else {
//                DatabaseDir = rephaseTheDir(DatabaseDir);
//            }
            DatabaseDir = rephaseTheDir(DatabaseDir);
            LogsDir = rephaseTheDir(LogsDir);
            SupportDomains = SupportDomainString.split(", ");
            return true;
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return false;
        }
    }

    ///make the dir path always end up with character '/'
    private String rephaseTheDir(String inputDir) {
        String outputDir = inputDir;
        if (inputDir.lastIndexOf('/') != inputDir.length() - 1) {
            outputDir += "/";
        }
        return outputDir;
    }

}
