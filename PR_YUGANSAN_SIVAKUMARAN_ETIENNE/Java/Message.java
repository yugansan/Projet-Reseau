import java.io.*;
import java.util.*;

public class Message{
  private String []tabMessage;
  private String messageTime,id;

  public Message(String []tabMessage,String id){
    this.tabMessage=tabMessage;
    this.messageTime=getLocalTime();
    this.id=id;
  }

  public Message(String message,String id){
    this.tabMessage=new String[1];
    this.tabMessage[0]=message;
    this.messageTime=getLocalTime();
    this.id=id;
  }

  public String []getTabMessage(){
    return this.tabMessage;
  }

  public String getMessageTime(){
    return this.messageTime;
  }

  public String getId(){
    return this.id;
  }

  /*
  * return local time format day:month:year:hour:min:sec
  */
  private static String getLocalTime(){
    GregorianCalendar gcalendar = new GregorianCalendar();
    String time=gcalendar.get(Calendar.DATE)+":"+gcalendar.get(Calendar.MONTH)+":"+gcalendar.get(Calendar.YEAR)+":"+gcalendar.get(Calendar.HOUR)+":"+gcalendar.get(Calendar.MINUTE)+":"+gcalendar.get(Calendar.SECOND);
    return time;
  }

}
