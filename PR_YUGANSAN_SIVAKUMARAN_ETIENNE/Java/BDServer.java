import java.net.*;
import java.io.*;
import java.util.*;

public class BDServer{

	private static Map<String, InfoClient> ServerBD;
	private final int tailleMax=100;

	public BDServer(){
		ServerBD=new HashMap<String, InfoClient>();
	}

	private static Map<String, InfoClient> getServerBD() {
		return ServerBD;
	}

	public boolean addClient(String id,int port,int mdp,String addressIP){
		if(getNbClients()<tailleMax){
			InfoClient infoC=new InfoClient(id,port,mdp,addressIP);
			if(getServerBD().put(id,infoC)==null)	return true;
		}
		return false;
	}

	public InfoClient getClient(String id){
		return getServerBD().get(id);
	}

	//::::::::::::::Extension Si on veux desinscrire un client:::::::::::::
	public void removeClient(String id){
		if(getServerBD().containsKey(id))
		getServerBD().remove(id);
	}

	public boolean containsId(String id){
		return getServerBD().containsKey(id);
	}

	public boolean getStatus(String id){
		if(getClient(id)!=null)	return getClient(id).getIsConnected();
		return false;
	}

	public void setStatus(String id,boolean isConnected){
		if (getServerBD().containsKey(id))	getClient(id).setIsConnected(isConnected);
	}

	/* getter and setter ::: Nombre de flux non consultes du client 'id' */
	public byte []getNbFluxNonConsu(String id){
		return getClient(id).getNbFluxNonConsu();
	}

	public void setNbFluxNonConsu(String id,boolean action){
		if(getClient(id)!=null) getClient(id).setNbFluxNonConsu(action);
	}

	/* getter ::: Nombre de clients inscrits sur le Serveur (<100) */
	public int getNbClients(){
		return getServerBD().size();
	}

	public String[] getListeClients(){
		String []tab = new String[getNbClients()];
		int i=0;
		for (String mapKey : getServerBD().keySet()) {
			tab[i]=mapKey;
			i++;
		}
		return tab;
	}

	public boolean isMdpCorrect(String id,int mdp){
		InfoClient info=getServerBD().get(id);
		return (info.getMdp()==mdp);
	}

	public void addAmis(String id,String idAmis){
		if(getClient(id)!=null && getClient(idAmis)!=null){
			getClient(id).addAmis(idAmis);
			getClient(idAmis).addAmis(id);
		}

	}

	public void removeAmis(String id,String idAmis){
		if(getClient(id)!=null)
		getClient(id).removeAmis(idAmis);
	}

	//Liste amis du client 'id'
	public ArrayList<String> getListAmis(String id){
		if(getClient(id)!=null)
		return getClient(id).getListAmis();
		return null;
	}

	// si client 'id' a un ami client 'idAmis'
	public boolean isAmis(String id,String idAmis){
		ArrayList<String> listAmis = getListAmis(id);
		return listAmis.contains(idAmis);
	}

	//getter pour amis damis
	public ArrayList<String> getAmisDAmisDAmis(String id){
		ArrayList<String> amisDAmis=new ArrayList<String>();
		ArrayList<String> listeAmis =getArrayCopy(getListAmis(id));
		int i=0;
		while(i<listeAmis.size()){
			String amis=listeAmis.get(i);
			if(!amisDAmis.contains(amis) && !amis.equals(id)){
				amisDAmis.add(amis);
				listeAmis.addAll(getListAmis(amis));
			}
			i++;
		}
		return amisDAmis;
	}

	private ArrayList<String>  getArrayCopy(ArrayList<String> arraylist){
		ArrayList<String> array=new ArrayList <String>();
		for(String item : arraylist){
			array.add(item);
		}
		return array;
	}

	/* getter & setter ::: addressIP */
	public String getAddressIP(String id){
		return getClient(id).getAddressIP();
	}
	public void setAddressIP(String id, String ip){
		if(getClient(id)!=null)	getClient(id).setAddressIP(ip);
	}

	/* getter ::: port */
	public int getPort(String id){
		return getClient(id).getNumPort();
	}

	/*:::::::::::::::::::::::::::::Recuperer ID:::::::::::::::::::::::::::::::::*/

	/* recuperer id de la socket so */
	public String getId(Socket so){
		String ip = so.getInetAddress().getHostAddress();
		int port = so.getPort();
		Map<String, InfoClient> clients = getServerBD();
		for (Map.Entry<String, InfoClient> entry : clients.entrySet()) {
			if (entry.getValue().getAddressIP().equals(ip) && entry.getValue().getPortTCP()==port) {
				return entry.getKey();
			}
		}
		return null;
	}

	public void setPortTCP(String id,int portTCP){
		this.getClient(id).setPortTCP(portTCP);
	}

	public int getPortTCP(String id){
		return this.getClient(id).getPortTCP();
	}

	/*:::::::::::::::::::::::::::::MESSAGES Extensions:::::::::::::::::::::::::::*/
	public void addMessage(String id,String idSource, String message){
		this.getClient(id).addMessage(idSource,message);
	}

	public void removeMessages(String id, String idAmis){
		this.getClient(id).removeMessages(idAmis);
	}

	public ArrayList<Message> getMessages(String id,String idAmis){
		//message de amis au client courant
		ArrayList<Message> messageAmis=this.getClient(id).getMessages(idAmis);
		//message de client courant a l ami
		ArrayList<Message> messageClient =this.getClient(idAmis).getMessages(id);
		ArrayList<Message> messRanger= new ArrayList<Message>();
		int iAmis=0,iClient=0;
		Message msClient,msAmis;
		while(messRanger.size()<(messageClient.size()+messageAmis.size())){
			msClient=messageClient.get(iClient);
			msAmis =messageAmis.get(iAmis);
			if((msAmis.getMessageTime().compareTo(msClient.getMessageTime()))<=0 && iAmis<messageAmis.size()){
				messRanger.add(msClient);
				iClient++;
			}else if(iClient<messageClient.size()){
				messRanger.add(msAmis);
				iAmis++;
			}else{
				messRanger.add(msClient);
				iClient++;
			}
		}
		return messRanger;
	}

	//:::::::::::::::::::::::::::::::::NOTIFICATIONS::::::::::::::::::::::::::::::

	//Recupere la premiere notification dans ArrayList<Notification> du client 'id'
	public Notification getNotification(String id){
		return this.getClient(id).getNotification();
	}

	//Ajout notification dans ArrayList<Notification> du client 'id'
	public boolean addNotification(String id, Notification notif){
		return this.getClient(id).addNotification(notif);
	}

	//Supp la premiere notification dans ArrayList<Notification> du client 'id'
	public void removeNotification(String id){
		this.getClient(id).removeNotification();
	}

	//Taille de ArrayList<Notification>
	public int getNbNotifNonConsu(String id){
		return this.getClient(id).getNbNotifNonConsu();
	}
	//fin_notifications

}
