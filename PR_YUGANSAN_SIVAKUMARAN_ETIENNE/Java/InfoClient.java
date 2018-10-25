import java.util.*;

public class InfoClient{
	private int tailleMax=100;
	private String identifiant;
	private String addressIP;
	private int numPort;
	private int portTCP;
	private int mdp;
	private boolean isConnected;
	private byte []nbFluxNonConsu = new byte[2];
	private ArrayList<String> listAmis;
	private Map<String,ArrayList<Message>> messages;
	private ArrayList<Notification> notifications;

	public InfoClient(String id,int port,int mdp, String addressIP){
		this.identifiant=id;
		this.numPort=port;
		this.mdp=mdp;
		this.addressIP=addressIP;
		this.nbFluxNonConsu[0]=0;
		this.nbFluxNonConsu[1]=0;
		this.isConnected=false;
		this.listAmis=new ArrayList<String>();
		this.messages=new HashMap<String, ArrayList<Message>>();
		this.notifications=new ArrayList<Notification>();
	}

	public String getIdentifiant(){
		return this.identifiant;
	}

	public int getNumPort(){
		return numPort;
	}

	public String getAddressIP(){
		return addressIP;
	}

	public void setAddressIP(String addressIP){
		this.addressIP=addressIP;
	}

	public boolean getIsConnected(){
		return isConnected;
	}

	public void setIsConnected(boolean isConnected){
		this.isConnected=isConnected;
	}

	public int getMdp(){
		return mdp;
	}

	public void setMdp(int mdp){
		this.mdp=mdp;
	}

	public byte []getNbFluxNonConsu(){
		return nbFluxNonConsu;
	}

	private int getUnsignedBytes(byte b){
		return b & 0xFF;
	}

	/*
	*
	* action 1 :::ajoute 1 notif ET 0:: enleve 1 notif
	*/
		public void setNbFluxNonConsu(boolean action){
			byte []tmp=getNbFluxNonConsu();
			if(action){
					if(getUnsignedBytes(tmp[0])<255){
						tmp[0]++;
					}else{
						if(!(getUnsignedBytes(tmp[1])>254)){
							tmp[1]++;
							tmp[0]++;
						}
					}
			}else{
				if(getUnsignedBytes(tmp[0])>0){
					tmp[0]--;
				}else{
					if(!(getUnsignedBytes(tmp[1])>0)){
						tmp[1]--;
						tmp[0]=(byte)255;
					}
				}
			}
			this.nbFluxNonConsu=tmp;
		}

//::::::::::::::::::::::::::::::::::AMIS::::::::::::::::::::::::::::::::::::::::
	public ArrayList<String> getListAmis(){
		return this.listAmis;
	}

	public boolean addAmis(String idAmis){
		if(!this.listAmis.contains(idAmis)){
			this.listAmis.add(idAmis);
			return true;
		}
		return false;
	}

	public boolean removeAmis(String idAmis){
		if(this.listAmis.contains(idAmis)){
			this.listAmis.remove(idAmis);
			return true;
		}
		return false;
	}

//::::::::::::::::::::::::::::::::MESSAGE:::::::::::::::::::::::::::::::::::::::
	public ArrayList<Message> getMessage(String id) {
		return this.messages.get(id);
	}

	public void addMessage(String id, String message){
		if(!this.messages.containsKey(id)){
			ArrayList<Message>msg=new ArrayList<Message>();
			msg.add(new Message(message, id));
			this.messages.put(id,msg);
		}
		this.messages.get(id).add(new Message(message, id));
	}

	public void removeMessages(String id){
		if(this.messages.containsKey(id))
			this.messages.remove(id);
	}

	public ArrayList<Message> getMessages(String id){
		if(this.messages.containsKey(id))
			return this.messages.get(id);
		return null;
	}

	//:::::::::::::::::::::::::::::::::NOTIFICATIONS::::::::::::::::::::::::::::::
	public Notification getNotification(){
		return this.notifications.get(0);
	}

	public boolean addNotification(Notification notif){
		return this.notifications.add(notif);
	}

	public void removeNotification(){
		this.notifications.remove(0);
	}

	public int getNbNotifNonConsu(){
		return this.notifications.size();
	}
	//fin_notifications

	public void setPortTCP(int portTCP){
		this.portTCP=portTCP;
	}
	public int getPortTCP(){
		return this.portTCP;
	}

	@Override
	public String toString(){
		return "id: ["+this.identifiant+"]";
	}
}

/*
Extensions:

-> si mdp oublie ::: possibilite de changer mdp

NOTIFICATIONS >>>> remove notif
-> PLTAR+++ ::: consulter plus tard alors retirer de la tete et ajouter a la fin de ArrayList
-> NBNOT+++ ::: affiche le nb de notif non consulter

LISTE AMIS
-> AMIS? :: afficher liste des amis d'un client id

MESSAGES :::
-> TMESS id+++ ::: affiche tous les msg recus from 'id' (liste des id )


*/
