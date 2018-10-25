import java.io.*;
import java.net.*;
import java.util.*;

public class Promoteur{

  private Scanner sc = new Scanner(System.in);
  private String IPMultiDiff="";
  private static int portMultiDiff;

  public Promoteur(int portMultiDiff){
    this.IPMultiDiff="225.1.2.4";
    this.portMultiDiff=portMultiDiff;
  }

  public void lancePromoteur(){
    if(envoiMessageAuServer()){
      String msgProm="";
      while(true){
        System.out.println("###################VOULEZ-VOUS ENVOYER UNE PUB?#####################");
        System.out.println(":::::::Repondez par oui ou par non:::::::");
        System.out.println("###################POUR CONNECTER A UN NOUVEAU SERVER#####################");
        System.out.println(":::::::NVSERV:::::::");
        msgProm = sc.nextLine();
        if(msgProm.equalsIgnoreCase("OUI")){
          lancerDemmandeEnvoiPub();
        }else if(msgProm.equalsIgnoreCase("NVSERV")){
          lancePromoteur();
        }
      }
    }
  }



  //ajoute "#" au messge pour qu'il contient exactement 300 caractere
  public String getMesssageEnvoiMultidiff(String message){
    String msg=message;
    for(int i=msg.length();i<300;i++){
      msg+="#";
    }
    return ("PROM "+msg);
  }

  //lancer la demmande de pub et envoyer
  public void lancerDemmandeEnvoiPub(){
    try{
      DatagramSocket dso=new DatagramSocket();
      byte[]data;
      String pub=getMessageFromProm(300);
      String messageAEnvoyer=getMesssageEnvoiMultidiff(pub);
      data=messageAEnvoyer.getBytes();
      InetSocketAddress ia=new InetSocketAddress(this.IPMultiDiff,this.portMultiDiff);
      DatagramPacket paquet=new DatagramPacket(data,data.length,ia);
      dso.send(paquet);
      dso.close();
    }catch(Exception e){
      e.printStackTrace();
    }
  }


  //attend le retour du message depuis le serveur
  public void receptionMessageFromServer(Socket socket){
    try{
        String message;
        InputStream inputStream =socket.getInputStream();
  			byte[] input=new byte[1024];
  			int bytesRead=inputStream.read(input);
  			message = new String(input);
  			message = message.trim();
        if(message.equalsIgnoreCase("PUBL>+++")){
          socket.close();
        }else{
          receptionMessageFromServer(socket);
        }
    }catch(Exception e){}

  }

  //envoi le message au serveu et attend la reponse
  public boolean envoiMessageAuServer(){
    String []tab=demandeIPPortServer();
    boolean isDone=false;
    String message="";
    try{
      Socket socket =connectAuServer(tab[0],Integer.parseInt(tab[1]));
      if(socket ==null) envoiMessageAuServer();
      message += getMessageFromProm(200);
      message = message.replace("\r\n","");
      message = message.trim()+"+++";
      message = "PUBL? "+this.IPMultiDiff+" "+this.portMultiDiff+" "+ message;
      if(sendPub(socket,message)){
        receptionMessageFromServer(socket);
        isDone=true;
      }
    }catch(Exception e){}
      return isDone;
  }



  public String getMessageFromProm(int tailleMax){
      String message;
      System.out.println("ENTRER le message a envoyer de taille inferieur a "+tailleMax);
      message = sc.nextLine();
      if(message.length()<tailleMax){
        return message;
      }
      System.out.println("taille trop grande :"+message.length());
      message="";
      return getMessageFromProm(tailleMax);
  }


  /*envoi le message de forme [PUBL?␣ip-diff␣port␣mess+++] au server a laquelle le socket est connecter*/

  public boolean sendPub(Socket socket, String message){
    try{
      OutputStream outputStream = socket.getOutputStream();
      outputStream.write(message.getBytes());
      outputStream.flush();
      return true;
    }catch(Exception e){}
    return false;
  }


  public Socket connectAuServer(String addressIP, int port){
    Socket socket=null;
    try{
      socket=new Socket(addressIP,port);
    }catch(Exception e){}
    return socket;
  }

  public String []demandeIPPortServer(){
    String []infoServertab=new String[2];
    infoServertab[0]="localhost";
    System.out.println("ENTRER l'adress IP et port du server (separer par un espace)");
    try{
      String infoServer=sc.nextLine();
      String []info = infoServer.split(" ");
      infoServertab[0] = (info[0].trim()).length()>7?info[0].trim():"localhost";
      Integer.parseInt(info[1].trim());
      infoServertab[1]=info[1].trim();
    }catch(Exception e){
      infoServertab[1]="9999";
    }
    return infoServertab;
  }

  //return port donne en param sinon 4242
	public static int getPortServer(String param){
		int port;
		try{
			port =Integer.parseInt(param);
		}catch(Exception e){
			port=9999;
		}
		return port;
	}

  public static void main(String[] args) {
    if (args.length==1){
			portMultiDiff=getPortServer(args[0]);
		}
    Promoteur pro=new Promoteur(portMultiDiff);
    pro.lancePromoteur();
  }
}
