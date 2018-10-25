import java.net.*;
import java.io.*;
import java.util.*;

class Serveur_PromoteurThread implements Runnable {

	private Thread promThread;
	private Socket socket;
	private Serveur serv;

	// Constructeur
	public Serveur_PromoteurThread (Socket so, Serveur serveur) {
		serv=serveur;
		socket=so;

		System.out.println("[Promoteur]>>> Nouvelle connection avec " + socket.getInetAddress().getHostAddress() +
		" (port " + socket.getPort() + ")");

		promThread = new Thread(this);
		promThread.start(); //exec de run()
	}

	// Méthode ::: interface Runnable
	public void run() {
		String message = " ";
		String []tabmessage;
		while(true){
			try{
				message = serv.getStringFromInputReader(socket);
				tabmessage = serv.subNPlus(message,3);
				for(int i=0;i<tabmessage.length;i++){
					tabmessage[i]=tabmessage[i].trim();
					if (tabmessage[i].length()>5){
						String cmd = tabmessage[i].substring(0,5);
						if (cmd.equalsIgnoreCase("PUBL?")){
							tabmessage[i] = tabmessage[i].substring(6);
							serv.publicite(socket, tabmessage[i]);
						}
					}
				}
			}catch(Exception e){
				serv.deconnection(socket);
			}
		}
	}//fin_run

}//fin_class
