import java.net.*;
import java.io.*;
import java.util.*;
import java.nio.*;
import java.nio.channels.*;

public class Serveur {

	static BDServer bd;
	private static int port=4242;
	private static int portDiff=9999;

	//** Methode : Main **
	public static void main(String args[]) {
		if (args.length==1){
			port=getPortServer(args[0]);
		}
		if (args.length==2){
			port=getPortServer(args[0]);
			portDiff=getPortServer(args[1]);
		}
		bd = new BDServer();
		Serveur serveur = new Serveur();
		try {
			//obtenir selecteur
			Selector s = Selector.open();
			//creer Channel
			ServerSocketChannel serverSocket = createServerChannel(s, port);
			ServerSocketChannel serverSocketPromoteur = createServerChannel(s, portDiff);
			MsgAccueilServ();
			while (true) {
				//ecouter les connections clients et prom
				//System.out.println("En attente de connections ...");
				s.select();
				Iterator<SelectionKey> it = s.selectedKeys().iterator();
				while(it.hasNext()){
					SelectionKey sk = it.next(); it.remove();
					// recup the Channel
					ServerSocketChannel ssc = (ServerSocketChannel) sk.channel();
					//accept()
					Socket sock = ssc.socket().accept();
					//Lancer un Serv avec le Service Socket concerné
					if(sk.channel()==serverSocket){
						new Serveur_ClientThread(sock,serveur); // un client connecté = un thread
					}
					if(sk.channel()==serverSocketPromoteur){
						new Serveur_PromoteurThread(sock,serveur); // un promoteur connecté = un thread
					}
				}
			}
		}catch (Exception e) {
			System.out.println("Erreur ::: Lancement du Serveur");
			System.out.println("Usage: java Serveur <portTCP> <portDiffusion>");
			// System.out.println(e);
			// e.printStackTrace();
			System.exit(1);
		}
	} //fin main()

	// Message d'accueil Serveur
	static private void MsgAccueilServ() {
		System.out.println("--------------------------------------");
		System.out.println("              IPortbook               ");
		System.out.println("--------------------------------------");
		System.out.println("Serveur en ecoute des clients sur le port "+port);
		System.out.println("Serveur en ecoute des promoteurs sur le port "+portDiff);
		System.out.println("Quitter : entrez \"Ctrl+C\"\n");
	}

	// Messages envoyés par Serveur
	public void MsgServ(Socket socket, int i) {
		String msgServ = "";
		switch (i) {
			case 0:
			msgServ = "GOBYE+++";
			break;
			case 1:
			msgServ = "WELCO+++";
			break;
			case 2:
			msgServ = "HELLO+++";
			break;
			case 3:
			msgServ = "FRIE>+++";
			break;
			case 4:
			msgServ = "FRIE<+++";
			break;
			case 5:
			msgServ = "MESS>+++";
			break;
			case 6:
			msgServ = "MESS<+++";
			break;
			case 7:
			msgServ = "FLOO>+++";
			break;
			case 9:
			msgServ = "PUBL>+++";
			break;
			default:
			break;
		}
		sendStringByOutputStream(socket,msgServ);
		// return;
	}//fin_MsgServ

	/**
	* S’inscrire.
	* MESSAGE du Client: REGIS␣id␣port␣mdp+++
	* @param id : identité du client
	* @param port : son port UDP
	* @param mdp : son mot de passe
	* @return ([WELCO+++] si il accepte) ([GOBYE+++] si il refuse)
	*/
	synchronized public void inscription(Socket socket, String message) {
		try {
			String splitMsg[] = null;
			splitMsg = message.split(" ");
			if (splitMsg.length==4 && !checkIsConnected(socket)){
				String id = splitMsg[1];
				int port = Integer.parseInt(splitMsg[2]);
				int mdp = Integer.parseInt(splitMsg[3]);
				if (bd.containsId(id)==false){
					String addressIP = socket.getInetAddress().getHostAddress();
					boolean addClient = bd.addClient(id, port, mdp, addressIP);
					if (addClient==false) deconnection(socket, id);
					bd.setPortTCP(id, socket.getPort());
					// System.out.println("[Client]>>> Nouveau client inscrit > "+bd.getClient(id).toString());
					MsgServ(socket, 1);
				}
				else {
					deconnection(socket, id);
				}
			}
		} catch (Exception e) {
			deconnection(socket);
		}
	}//fin_inscription

	/**
	* MESSAGE du Client: CONNE␣id␣mdp+++
	* @param id : son identité
	* @param mdp : son mot de passe
	* @return ([HELLO+++] si il accepte) ([GOBYE+++] si il refuse et ferme sa connection)
	*/
	synchronized public void connection(Socket socket, String message) {
		String splitMsg[] = null, id = null;
		try{
			splitMsg = message.split(" ");
			if (splitMsg.length==3 && !checkIsConnected(socket)){
				id = splitMsg[1];
				int mdp = Integer.parseInt(splitMsg[2]);
				/* accepter connection si le client "id" existe dans BDServer + [status] isConnected==false + mdp correct */
				if (bd.containsId(id) && bd.isMdpCorrect(id,mdp) && !bd.getStatus(id)){
					bd.setStatus(id, true);
					bd.setAddressIP(id, socket.getInetAddress().getHostAddress());
					bd.setPortTCP(id,socket.getPort());
					// System.out.println("Client connecté > "+bd.getClient(id).toString());
					MsgServ(socket, 2);
				} else {
					deconnection(socket);
				}
			}
		} catch (Exception e) {
			deconnection(socket);
		}
	}//fin_connection

	/**
	* MESSAGE du Client: FRIE?␣id+++
	* @param id : identité du client avec lequel il souhaite être ami
	* @return ([FRIE>+++] si demande d'amis transmise) ([FRIE<+++] si il ne connaît pas le client id)
	*/
	synchronized public void demandesAmities(Socket socket, String message) {
		String splitMsg[] = null, id = null, idSource = null;
		try {
			idSource = bd.getId(socket);
			splitMsg = message.split(" ");
			if (splitMsg.length==2){
				id = splitMsg[1];
				if (!id.equals(idSource) && bd.containsId(id) && checkIsConnected(socket) && !bd.isAmis(idSource,id)){
					//notification UDP [0XX]
					bd.setNbFluxNonConsu(id, true);
					byte []data=new byte[3];
					byte []tmp = bd.getNbFluxNonConsu(id);
					data[0] = (byte)0;
					data[1] = tmp[0];
					data[2] = tmp[1];
					DatagramSocket ds = new DatagramSocket();
					DatagramPacket packet = new DatagramPacket(data, data.length, InetAddress.getByName(bd.getAddressIP(id)), bd.getPort(id));
					ds.send(packet);
					//ajout flux
					Notification notif = new Notification("0", bd.getId(socket));
					bd.addNotification(id, notif);
					MsgServ(socket, 3);
				}
				else {
					MsgServ(socket, 4);
				}
			}
		}
		catch (Exception e) {
			MsgServ(socket, 4);
		}
	}

	/**
	* MESSAGE du Client: MESS?␣id␣num-mess+++
	* @param id : identité de l’ami
	* @param num-mess : nombre de messages partiels envoyé qui correspondront au message global
	* @return ([MESS>+++] si msg transmis et si dernier MENUM envoyé) ([MESS<+++] si msg non transmis)
	*/
	synchronized public void envoisMsg(Socket socket, String []message,String idDest) {
		String id = null;
		try {
			id=bd.getId(socket);
			// ** notification UDP [3XX] **
			bd.setNbFluxNonConsu(idDest, true);
			byte []data=new byte[3];
			byte []tmp = bd.getNbFluxNonConsu(idDest);
			data[0] = (byte)3;
			data[1] = tmp[0];
			data[2] = tmp[1];
			DatagramSocket ds = new DatagramSocket();
			DatagramPacket packet = new DatagramPacket(data, data.length, InetAddress.getByName(bd.getAddressIP(idDest)), bd.getPort(idDest));
			ds.send(packet);
			ds.close();
			// ** Ajout notif aux flux du client 'idDest' **
			Notification notif = new Notification("3",id,message);
			bd.addNotification(idDest,notif);

			/* Extension ::: historique des messages */
			for (int i=0;i<message.length ; i++) {
				bd.addMessage(idDest,id,message[i]);
			}//fin_ext
		}
		catch (Exception e) {
			MsgServ(socket, 6);
		}
	}

	/** Extension
	* MESSAGE du Client: TMESS␣id+++
	* @param id : identité de l’ami
	* @return ([SSEMT idAmis DaytimeMess Mess+++]
	*/
	synchronized public void histoMsg(Socket socket, String message){
		String splitMsg[] = null, id = null, idAmis = null, msgServ = null;
		try {
			id = bd.getId(socket);
			splitMsg = message.split(" ");
			if (splitMsg.length==2){
				idAmis = splitMsg[1].trim();
				if (bd.containsId(id) && bd.containsId(idAmis) && checkIsConnected(socket) && bd.isAmis(id,idAmis)){
					ArrayList<Message> histo = bd.getMessages(id,idAmis);
					for (int i=0;i<histo.size();i++){
						Message msg = histo.get(i);
						String []tabMessage = msg.getTabMessage();
						for (int j=0; j<tabMessage.length; j++) {
							msgServ = "SSEMT "+msg.getId()+" "+msg.getMessageTime()+" "+tabMessage[i]+"+++";
							sendStringByOutputStream(socket,msgServ);
						}
					}
				}
			}
		}catch (Exception e) {

		}
	}


	/**
	* MESSAGE du Client : LIST?+++
	* @return (liste des clients)
	*/
	synchronized public void listeClients(Socket socket) {
		try {
			if(checkIsConnected(socket)){
				int num_item = bd.getNbClients(); //nombre de clients dans la liste
				String msgServ = "RLIST "+num_item+"+++";
				sendStringByOutputStream(socket,msgServ);
				// recuperer un tab d'int contenant les "id des clients" +++ extension !! afficher "valeur de isConnected"
				String tabClients[] = bd.getListeClients();
				for (int i=0;i<tabClients.length;i++){
					String id = tabClients[i];
					if(bd.getStatus(id)){
						msgServ = "LINUM "+tabClients[i]+"[Connecte]+++";
					}else{
						msgServ = "LINUM "+tabClients[i]+"+++";
					}
					sendStringByOutputStream(socket,msgServ);
				}
			}
		}
		catch (Exception e) {
		}
	}

	/** Extension
	* MESSAGE du Client : AMIS?+++
	* @return (liste des amis)
	*/
	synchronized public void listeAmis(Socket socket) {
		String idSource=null;
		try {
			idSource = bd.getId(socket);
			if(checkIsConnected(socket)){
				// recuperer un tab d'int contenant les "id des amis" + afficher "valeur de isConnected"
				ArrayList<String> tabAmis = bd.getListAmis(idSource);
				int num_item = tabAmis.size(); //nombre d'amis dans la liste
				String msgServ = "RAMIS "+num_item+"+++";
				sendStringByOutputStream(socket,msgServ);
				for (int i=0;i<num_item;i++){
					String idAmis = tabAmis.get(i);
					if(bd.getStatus(idAmis)){
						msgServ = "AMIS> "+idAmis+"[Connecte]+++";
					}else{
						msgServ = "AMIS> "+idAmis+"+++";
					}
					sendStringByOutputStream(socket,msgServ);
				}
			}
		}
		catch (Exception e) {
		}
	}

	/**
	* Inondation.
	* MESSAGE du Client: FLOO?␣mess+++
	* @param mess : message envoyé (< 200caracteres)
	* @return ([FLOO>+++] s’il a transmis le message)
	*/
	synchronized public void inondation(Socket socket, String message) {
		String idSource, idDest;
		DatagramSocket ds;
		try {
			if(checkIsConnected(socket)){
				idSource = bd.getId(socket);
				Notification notif = new Notification("4", idSource, message);
				ArrayList<String> listeInondation = bd.getAmisDAmisDAmis(idSource);
				ds = new DatagramSocket();
				for(int i=0; i<listeInondation.size(); i++){
					idDest = listeInondation.get(i);
					// ** notification UDP [4XX] **
					bd.setNbFluxNonConsu(idDest, true);
					byte []data=new byte[3];
					byte []tmp = bd.getNbFluxNonConsu(idDest);
					data[0] = (byte)4;
					data[1] = tmp[0];
					data[2] = tmp[1];
					DatagramPacket packet = new DatagramPacket(data, data.length, InetAddress.getByName(bd.getAddressIP(idDest)), bd.getPort(idDest));
					ds.send(packet);
					// ** Ajout notif aux flux du client 'idDest' **
					bd.addNotification(idDest, notif);
				}
				ds.close();
				//FLOO transmis
				MsgServ(socket, 7);
			}
		}
		catch (Exception e) {

		}
	}

	/** Extension
	* MESSAGE du Client : NBNOT+++
	* @return (nb notif non consultes)
	*/
	synchronized public void nbNotifNonConsu(Socket socket) {
		String idSource=null;
		try {
			idSource = bd.getId(socket);
			if(checkIsConnected(socket)){
				byte []tmp = bd.getNbFluxNonConsu(idSource);
				// String nbNotifNonConsu = new String(tmp);
				// String msgServ = "USNOC "+nbNotifNonConsu.trim()+"+++";
				String msgServ = "USNOC "+tmp[1]+tmp[0]+"+++";
				sendStringByOutputStream(socket,msgServ);
			}
		}
		catch (Exception e) {
		}
	}

	/**
	* Deconnection.
	* MESSAGE du Client : IQUIT+++
	* @return ([GOBYE+++])
	*/
	synchronized public void deconnection(Socket socket) {
		try {
			String id = bd.getId(socket);
			deconnection(socket, id);
		}
		catch (Exception e) {
		}
	}

	synchronized public void deconnection(Socket socket, String id) {
		try {
			MsgServ(socket, 0);
			bd.setStatus(id, false);
			socket.close();
		}
		catch (Exception e) {
		}
	}

	/**
	* MESSAGE du Promoteur : PUBL? ip-diff port mess+++
	* @return ([PUBL>+++])
	*/
	synchronized public void publicite(Socket socket, String message) {
		String id, tabClients[];
		DatagramSocket ds;
		try {
			Notification notif = new Notification("5", message);
			tabClients = bd.getListeClients();
			ds = new DatagramSocket();
			for (int i=0;i<tabClients.length;i++){
				id = tabClients[i];
				// ** notification UDP [5XX] **
				bd.setNbFluxNonConsu(id, true);
				byte []data=new byte[3];
				byte []tmp = bd.getNbFluxNonConsu(id);
				data[0] = (byte)5;
				data[1] = tmp[0];
				data[2] = tmp[1];
				DatagramPacket packet = new DatagramPacket(data, data.length, InetAddress.getByName(bd.getAddressIP(id)), bd.getPort(id));
				ds.send(packet);
				// ** Ajout notif aux flux du client 'id' **
				bd.addNotification(id, notif);
			}
			ds.close();
			//PUBL transmis
			MsgServ(socket, 9);
		}
		catch (Exception e) {
		}
	}//fin_publicite

	//*************************Fonctions_Utiles***********************************

	/**
	* Verifie si le client de la socket est connecte.
	* @param socket : socket du client
	* @return (boolean 1 si connecte )
	*/
	public boolean checkIsConnected(Socket socket){
		String id=bd.getId(socket);
		return bd.getStatus(id);
	}

	/**
	* Verifie si le client id est un ami de l'id la socket connecte.
	* @param socket,id : socket du client, id de amis
	* @return (boolean 1 si vrai)
	*/
	public boolean isEnvoiPossible(Socket socket,String id){
		String idEnvoi=bd.getId(socket);
		return bd.containsId(id) &&  bd.getStatus(idEnvoi) && bd.isAmis(idEnvoi,id);
	}

	/**
	* Renvoie port du Serveur.
	* @param param : port donne en parametre
	* @return ( (int)param sinon 4242)
	*/
	public static int getPortServer(String param){
		int port;
		try{
			port =Integer.parseInt(param);
		}catch(Exception e){
			port=4242;
			portDiff=9999;
		}
		return port;
	}

	/**
	* Writer.
	* @param socket,message : socket courante, data a envoyer
	*/
	public void sendStringByOutputStream(Socket socket, String message){
		try{
			OutputStream outputStream = socket.getOutputStream();
			outputStream.write(message.getBytes());
			outputStream.flush();
		}catch (Exception e) {

		}
	}

	/**
	* Reader.
	* @param socket : socket courante
	* @return ( (String)data lu)
	*/
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

	/**
	* Suppression les '+++'.
	* @param message,nbDePlusASupprimer
	* @return (tableau des data recus)
	*/
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

	/**
	* ServerSocketChannel.
	*/
	public static ServerSocketChannel createServerChannel(Selector s, int port){
		try{
			//creer un canal serv
			ServerSocketChannel ssc = ServerSocketChannel.open();
			//mode non bloquant
			ssc.configureBlocking(false);
			//configure socket
			ServerSocket ss = ssc.socket();
			ss.bind(new InetSocketAddress(port));
			//configure selecteur
			ssc.register(s, ssc.validOps());
			return ssc;
		} catch (BindException be) {
			System.out.println("Erreur ::: Lancement du Serveur");
			System.out.println("Port deja utilise");
			System.out.println("Usage: java Serveur <portTCP> <portDiffusion>");
			System.exit(1);
		} catch (Exception e) {
			System.out.println("Erreur ::: Lancement du Serveur");
			System.exit(1);
		}
		return null;
	}

}//fin_class
