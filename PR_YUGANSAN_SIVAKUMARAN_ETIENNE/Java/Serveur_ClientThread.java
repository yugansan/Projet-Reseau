import java.net.*;
import java.io.*;
import java.util.*;

class Serveur_ClientThread implements Runnable {

	private Thread clientThread; // un clt = un thread
	private Socket socket;
	private Serveur serv;

	// Constructeur
	public Serveur_ClientThread (Socket so, Serveur serveur) {
		serv=serveur;
		socket=so;

		System.out.println("[Client]>>> Nouvelle connection avec " + socket.getInetAddress().getHostAddress() +
		" (port " + socket.getPort() + ")");

		clientThread = new Thread(this);
		clientThread.start(); //exec de run()
	}

	// Méthode ::: interface Runnable
	public void run() {
		String message = " ";
		String []tabmessage;
		while(true){
			try{
				message =getStringFromInputReader(socket);
				tabmessage = subNPlus(message,3);
				for(int i=0;i<tabmessage.length;i++){
					if ((tabmessage[i].trim()).equalsIgnoreCase("IQUIT")){
						serv.deconnection(socket);
					}
					lanceAction(tabmessage[i].trim());
				}
			}catch(Exception e){
				serv.deconnection(socket);
			}
		}
	}

	//lis en bytes et transforme en String
	public String getStringFromInputReader(Socket socket){
		String message="";
		try{
			InputStream inputStream =socket.getInputStream();
			byte[] input=new byte[1024];
			int bytesRead=inputStream.read(input);
			message = new String(input);
			message = message.trim();
		}catch(Exception e){}
			return message;
		}

		//lis en bytes et transforme en String en supprimant les '\n'
		public String getStringFromInputReaderWithoutbackN(Socket socket){
			String message = getStringFromInputReader(socket);
			message = message.replace("\r\n","");
			message = message.trim();
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
		* Traitement des commandes recus.
		* @param message : data recu
		*/
		public void lanceAction(String message){
			if (message.length()>=5){
				String cmd = message.substring(0,5);
				//** Demande d'inscription **
				if (cmd.equalsIgnoreCase("REGIS")){
					serv.inscription(socket, message);
				}
				//** Connection **
				else if (cmd.equalsIgnoreCase("CONNE")){
					serv.connection(socket, message);
				}
				//** Demandes d'amitiés **
				else if (cmd.equalsIgnoreCase("FRIE?")){
					serv.demandesAmities(socket, message);
				}
				//** Envois de messages **
				else if (cmd.equalsIgnoreCase("MESS?")){
					envoisMsg(socket, message);
				}
				//** Liste des clients **
				else if (message.equalsIgnoreCase("LIST?")){
					serv.listeClients(socket);
				}
				//** Inondation **
				else if (cmd.equalsIgnoreCase("FLOO?")){
					message = message.substring(6);
					serv.inondation(socket, message);
				}
				//** Consultation **
				else if (message.equalsIgnoreCase("CONSU")){
					consultation();
				}
				//** Deconnection **
				else if (message.equalsIgnoreCase("IQUIT")){
					serv.deconnection(socket);
				}
				// // ** Extension :: historique des messages **
				// else if (cmd.equalsIgnoreCase("TMESS")){
				// 	serv.histoMsg(socket, message);
				// }
				// ** Extension :: liste des amis **
				else if (message.equalsIgnoreCase("AMIS?")){
					serv.listeAmis(socket);
				}
				// ** Extension :: nb notif non consultes **
				else if (message.equalsIgnoreCase("NBNOT")){
					serv.nbNotifNonConsu(socket);
				}
			}
		}


		/**
		* MESSAGE du Client: MESS?␣id␣num-mess+++
		* @param id : identité de l’ami
		* @param num-mess : nombre de messages partiels envoyé qui correspondront au message global
		* @return ([MESS>+++] si msg transmis et si dernier MENUM envoyé) ([MESS<+++] si msg non transmis)
		*/
		public void envoisMsg(Socket socket, String message) {
			String splitMsg[] = null, id = null;
			try {
				splitMsg = message.split(" ");
				if (splitMsg.length==3){
					id = splitMsg[1];
					int num_mess = Integer.parseInt(splitMsg[2]);
					if (serv.isEnvoiPossible(socket,id)){
						String []tabMess=new String[num_mess];
						String msg="";
						String []tmp;
						for (int i=0; i<num_mess; i++){
							msg=getStringFromInputReader(socket);
							tmp=parseMessMesg(msg);
							int condition =  Integer.parseInt(tmp[0]);
							if(tmp[0]=="-1" || condition>=num_mess || condition<0){
								serv.MsgServ(socket, 6);
								return;
							}
							tabMess[Integer.parseInt(tmp[0])] =tmp[1];
						}

						for(int i=0; i<num_mess; i++){
							if(tabMess[i]==null){
								serv.MsgServ(socket, 6);
								return;
							}
						}
						serv.envoisMsg(socket,tabMess,id);
						serv.MsgServ(socket, 5);
						return;
					}
					else {
						serv.MsgServ(socket, 6);
						return;
					}
				}
			}
			catch (Exception e) {
				serv.MsgServ(socket, 6);
			}
		}

		public String []parseMessMesg(String message){
			String []tmp= new String[2];
			tmp[0]="-1";
			String cmd = message.substring(0,5);
			if(cmd.equalsIgnoreCase("MENUM")){
				try{
					int numMess=Integer.parseInt(message.substring(6,7));
					tmp[0]=numMess+"";
					//enleve +
					for(int i=0; i<3;i++){
						if(message.charAt((message.length()-1)-i)!='+'){
							tmp[0]="-1";
						}
					}
					//le reste du message sans +++
					tmp[1]=message.substring(8,message.length()-3);
				}catch(Exception e){
					tmp[0]="-1";
				}
			}
			return tmp;
		}

		/**
		* MESSAGE du Client : CONSU+++
		* @return flux
		*/
		synchronized public void consultation() {
			try {
				String id=serv.bd.getId(socket);
				String flux;
				if(serv.bd.getStatus(id)){
					if(serv.bd.getNbNotifNonConsu(id)<=0){
						flux = "NOCON+++";
						serv.sendStringByOutputStream(socket,flux);
					}else{
						serv.bd.setNbFluxNonConsu(id, false);
						Notification notif = serv.bd.getNotification(id);
						String typeDeNotif = notif.getTypeDeNotif();
						serv.bd.removeNotification(id);
						if (typeDeNotif.equals("3")) { //CONSU_MESSAGE
							flux=notif.getFlux();
							serv.sendStringByOutputStream(socket,flux);
							String []tab = notif.getMunem();
							for (int i=0 ; i<tab.length ; i++) {
								serv.sendStringByOutputStream(socket,tab[i]);
							}
						}else if(typeDeNotif.equals("0")) { //CONSU_DemandeAmis
							flux = notif.getFlux();
							serv.sendStringByOutputStream(socket,flux);
							String reponse = ReponseDemandesAmities();
							traiterReponseDemandesAmities(reponse, notif, id);
							flux = "ACKRF+++";
							serv.sendStringByOutputStream(socket,flux);
						}else{
							flux = notif.getFlux();
							serv.sendStringByOutputStream(socket,flux);
						}
					}
				}
			}
			catch (Exception e) {

			}
		}

		/**
		* Attente de la reponse a une demandesAmities.
		* @return (reponse)
		*/
		public String ReponseDemandesAmities(){
			String msg=null;
			try {
				msg = getStringFromInputReaderWithoutbackN(socket);
			}catch (Exception e){
				msg = "PLTAR+++";
			}
			return msg;
		}

		/**
		* Traitement de la reponse a une demandesAmities.
		*/
		public void traiterReponseDemandesAmities(String reponse, Notification notif, String idCourant){
			try {
				Notification reponseNotif;
				String idSource = notif.getIdSource();
				DatagramSocket ds = new DatagramSocket();
				byte data[]=new byte[3], tmp[];
				DatagramPacket packet;
				// ** reponse positive a demandesAmities **
				if(reponse.equalsIgnoreCase("OKIRF+++")){
					serv.bd.addAmis(idSource, idCourant);
					// ** notification UDP [1XX] **
					serv.bd.setNbFluxNonConsu(idSource, true);	//incremente nbFluxNonConsu XX
					tmp = serv.bd.getNbFluxNonConsu(idSource);	//recuperer XX
					//definir YXX
					data[0] = (byte)1;	//Y
					data[1] = tmp[0];		//X
					data[2] = tmp[1];		//X
					packet = new DatagramPacket(data, data.length, InetAddress.getByName(serv.bd.getAddressIP(idSource)), serv.bd.getPort(idSource));
					ds.send(packet);
					// ** ajout flux **
					reponseNotif = new Notification("1", idCourant);
					serv.bd.addNotification(idSource, reponseNotif);
				}
				// ** reponse negative a demandesAmities **
				else if(reponse.equalsIgnoreCase("NOKRF+++")){
					// ** notification UDP [2XX] **
					serv.bd.setNbFluxNonConsu(idSource, true);	//incremente nbFluxNonConsu XX
					tmp = serv.bd.getNbFluxNonConsu(idSource);	//recuperer XX
					//definir YXX
					data[0] = (byte)2;	//Y
					data[1] = tmp[0];		//X
					data[2] = tmp[1];		//X
					packet = new DatagramPacket(data, data.length, InetAddress.getByName(serv.bd.getAddressIP(idSource)), serv.bd.getPort(idSource));
					ds.send(packet);
					// ** ajout flux **
					reponseNotif = new Notification("2", idCourant);
					serv.bd.addNotification(idSource, reponseNotif);
				}
				// ** sinon ::: Extension PLTAR ::: consulter 'demandesAmities' plus tard **
				else{
					serv.bd.addNotification(idCourant, notif);
					serv.bd.setNbFluxNonConsu(idCourant, true);
				}
			} catch (Exception e) {
				System.out.println("catch!! >> traiterReponseDemandesAmities");
			}
		}

	}//fin_class
