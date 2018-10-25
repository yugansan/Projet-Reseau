import java.io.*;
import java.util.*;
import java.net.*;
import java.nio.*;

public class Client{

  private int portServer;
  private String addressIPServer="localhost";
  private boolean isConnected;
  private String id;
  private Scanner sc = new Scanner(System.in);
  private Socket socket;

  //** Methode : Main **
  public static void main(String args[]) {

    String ip="localhost";
    int port = 4242;
    if (args.length==1){
      port=getPortServer(args[0]);
    }else if(args.length==2){
      port=getPortServer(args[0]);
      ip = args[1];
    }
    Client c = new Client(ip, port);
    c.lancerClient();
  }

  public Client(String addressIPServer, int portServer){
    this.addressIPServer = addressIPServer;
    this.portServer = portServer;
    this.isConnected =false;
  }

  //essaye de connection au server
  public Socket connectAuServer(String addressIP, int port){
    Socket socket=null;
    try{
      socket=new Socket(addressIP,port);
    }catch(Exception e){}
      return socket;
    }

    public void lancerClient(){
      Socket socket = connectAuServer(this.addressIPServer,this.portServer);
      if(socket == null){
        System.out.println("Impossible de se connecter au SERVER");
        System.out.println("Veuillez reessayer");
        if(demandeIPPortServer()){
          lancerClient();
        }
      }
      else{
        this.socket = socket;
        if(getIsConnected()){
          ecouterClient();
        }else{
          authentification();
        }
      }
    }

    public void ecouterClient(){
      String cmdClient;
      try{
        while (true){
          cmdClient = sc.nextLine();
          lanceAction(cmdClient);
          //traiter cmd

        }
      }catch (Exception e) {
        deconnection();
      }
    }

    public void lanceAction(String message){

      if (message.length()>=5){
        String cmd = message.substring(0,5);
        String paramCmd=message.substring(6,message.length());
        //** Demande d'inscription **
        if (cmd.equalsIgnoreCase("FRIEND")){
          demandesAmities(paramCmd);
        }
        // //** Connection **
        // else if (cmd.equalsIgnoreCase("CONNE")){
        //   serv.connection(socket, message);
        // }
        // //** Demandes d'amitiés **
        //
        // //** Consultation **
        // else if (message.equalsIgnoreCase("CONSU")){
        //   consultation();
        // }
        // //** Deconnection **
        // else if (message.equalsIgnoreCase("IQUIT")){
        //   serv.deconnection(socket);
        // }
      }
    }

    /**
    * Authentification.
    */
    public void authentification() {
      String reponse = null;
      System.out.println("Etes-vous inscrit? oui/non");
      reponse = sc.nextLine();
      reponse = reponse.trim();
      if (reponse.equalsIgnoreCase("non")){
        inscription();
      }else if (reponse.equalsIgnoreCase("oui")){
        connection();
      }else{
        authentification();
      }
    }//fin_inscription

    public void inscription(){
      try{
        String info, id, message, reponse;
        int port, mdp;
        System.out.println("Inscription");
        System.out.println("Votre id : ");
        id = sc.nextLine();
        System.out.println("Votre port UDP : ");
        info = sc.nextLine();
        port = Integer.parseInt(info);
        System.out.println("Votre mot de passe : ");
        info = sc.nextLine();
        mdp = Integer.parseInt(info);
        message = "REGIS "+id+" "+port+" "+mdp+"+++";
        sendStringByOutputStream(socket, message);
        reponse = getStringFromInputReader(socket);
        if (reponse.equalsIgnoreCase("WELCO+++")){
          System.out.println("Vous êtes bien inscrit.");
          authentification();
        }else if (reponse.equalsIgnoreCase("GOBYE+++")){
          System.out.println("L'inscription a été refusé.");
        }else{
          authentification();
        }
      }catch (Exception e) {
        System.out.println("Erreur produite lors de l'inscription !");
        authentification();
      }
    }//fin_inscription

    public void connection(){
      String info, id, message, reponse;
      int mdp;
      System.out.println("Connection");
      System.out.println("Votre id : ");
      id = sc.nextLine();
      System.out.println("Votre mot de passe : ");
      info = sc.nextLine();
      mdp = Integer.parseInt(info);
      message = "CONNE "+id+" "+mdp+"+++";
      sendStringByOutputStream(socket, message);
      reponse = getStringFromInputReader(socket);
      if (reponse.equalsIgnoreCase("HELLO+++")){
        System.out.println("Vous êtes connecté.");
        isConnected = true;
        lancerClient();
      }else if (reponse.equalsIgnoreCase("GOBYE+++")){
        System.out.println("La connection a été refusé.");
      }else{
        authentification();
      }
    }
    // public void demandesAmities()

    public boolean demandesAmities(String id){
      String msgDemandeAmis="",reponseDuServer="";
      msgDemandeAmis="FRIE? "+id+"+++";
      sendStringByOutputStream(socket, msgDemandeAmis);
      reponseDuServer = getStringFromInputReader(socket);
      if (reponseDuServer.equalsIgnoreCase("FRIE>+++")){
        return true;
      }
      return false;
    }

    public boolean listeClients(){
      String msgDemandelist="",reponseDuServer="",message="";
      int nombreClient=0;
      msgDemandelist="LIST?+++";
      sendStringByOutputStream(socket, msgDemandelist);
      reponseDuServer = getStringFromInputReader(socket);
      message = reponseDuServer.substring(0,5);
      if (message.equalsIgnoreCase("RLIST")){
        nombreClient = getStringToInt(reponseDuServer.substring(0,5));
        int nb=0;
        while(nb<nombreClient){
          reponseDuServer = getStringFromInputReader(socket);
          //teste LINUM et incremente nb pour chaque linum
        }
        return true;
      }
      return false;
    }

    private int getStringToInt(String nombre){
      int nb=0;
      try{
        nb = Integer.parseInt(nombre);
      }catch(Exception e){
        nb=0;
      }
      return nb;
    }
    // public void listeClients()
    // public void envoisMsg()
    // public void consultation()

    //deconnection
    public void deconnection() {
      try {
        socket.close();
      }
      catch (Exception e) {

      }
    }



    //return port donne en param sinon 4242
    public static int getPortServer(String param){
      int port;
      try{
        port =Integer.parseInt(param);
      }catch(Exception e){
        port=4242;
      }
      return port;
    }

    public boolean demandeIPPortServer(){
      String ip="localhost";
      int port=4242;
      System.out.println("ENTRER l'adress IP et port du server (separer par un espace)");
      try{
        String infoServer=sc.nextLine();
        String []info = infoServer.split(" ");
        ip = (info[0].trim()).length()>7?info[0].trim():"localhost";
        port=Integer.parseInt(info[1].trim());
      }catch(Exception e){
        port=4242;
      }
      this.addressIPServer=ip;
      this.portServer=port;
      return true;
    }

    //Writer
    public void sendStringByOutputStream(Socket socket, String message){
      try{
        OutputStream outputStream = socket.getOutputStream();
        outputStream.write(message.getBytes());
        outputStream.flush();
      }catch (Exception e) {

      }
    }

    //Reader
    //lis en bytes et transforme en String
    public String getStringFromInputReader(Socket socket){
      String message="";
      try{
        InputStream inputStream =socket.getInputStream();
        byte[] input=new byte[1024];
        int bytesRead=inputStream.read(input);
        message = new String(input);
        message = message.trim();
      }catch(Exception e){

      }
      return message;
    }

    public boolean getIsConnected(){
      return this.isConnected;
    }

    public String[] subNPlus(String message,int nbDePlusASupprimer){
			String s = new StringBuffer(message).reverse().toString();
			String msg[]= s.split("[+]{"+nbDePlusASupprimer+","+nbDePlusASupprimer+"}");
			for (int i=0; i< msg.length;i++) {
				msg[i] = new StringBuffer(msg[i].trim()).reverse().toString();
			}

			List<String> list = new LinkedList<String>(Arrays.asList(msg));
			for (Iterator<String> iter = list.listIterator(); iter.hasNext(); ) {
				String a = iter.next();
				if (a.equalsIgnoreCase("")) iter.remove();
			}
			Collections.reverse(list);
			String []mess  = (String[]) list.toArray(new String[list.size()]);
			return mess;
		}

  }//fin_class
