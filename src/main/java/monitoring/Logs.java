package monitoring;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class Logs {
    public static void appendLog(String text){
        SimpleDateFormat mdformat = new SimpleDateFormat("yyyyMMdd");
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss:SSS");
        String path = "C:\\MEmuRestarter\\MemuMasterLog\\";
        File dir = new File(path);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File logFile = new File(path +"/"+mdformat.format(Calendar.getInstance().getTime())+".file");
        if (!logFile.exists())
        {
            try
            {
                logFile.createNewFile();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        try
        {
            //BufferedWriter for performance, true to set append to file flag
            BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
            buf.append(timeFormat.format(Calendar.getInstance().getTime())+" - "+text);
            buf.newLine();
            buf.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}