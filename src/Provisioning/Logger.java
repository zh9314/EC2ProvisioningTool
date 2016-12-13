package Provisioning;

import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Logger {
	
	private FileWriter logFile;
	
	public Logger(String logFilePath){
		try {
			logFile = new FileWriter(logFilePath, true);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void log(String level, String className, String logContent){
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS");
		String currentTime = df.format(new Date());
		try {
			logFile.write(currentTime+" ["+level+"] "+className+": "+logContent+"\n");
			logFile.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public void closeLog(){
		try {
			logFile.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
