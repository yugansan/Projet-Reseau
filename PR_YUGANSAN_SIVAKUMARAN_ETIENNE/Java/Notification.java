import java.util.*;

public class Notification{
	private String typeDeNotif, idSource, flux, munem[] = null;

	public Notification(String typeDeNotif, String idSource){
		this.typeDeNotif = typeDeNotif;
		this.idSource = idSource;
		this.flux = msgServ(typeDeNotif) + idSource + "+++";
	}

	//MESSAGES
	public Notification(String typeDeNotif, String idSource, String msg[]){
		this.typeDeNotif = typeDeNotif;
		this.idSource = idSource;
		int num_mess = msg.length;
		this.flux = msgServ(typeDeNotif)+ idSource + " " + num_mess + "+++";
		this.munem = new String[num_mess];
		for(int i=0; i<num_mess; i++){
			munem[i]="MUNEM " + i + " " + msg[i] + "+++";
		}
	}

	//INONDATION
	public Notification(String typeDeNotif, String idSource, String msg){
		this.typeDeNotif = typeDeNotif;
		this.idSource = idSource;
		this.flux = msgServ(typeDeNotif)+ idSource + " " + msg + "+++";
	}

/***********************************MsgServeur*********************************/
	// Messages envoyés par Serveur
	public String msgServ(String typeDeNotif) {
		String msgServ = null;
		switch (typeDeNotif) {
			case "0":
				msgServ = "EIRF> ";
				break;
			case "1":
				msgServ = "FRIEN ";
				break;
			case "2":
				msgServ = "NOFRI ";
				break;
			case "3":
				msgServ = "SSEM> ";
				break;
			case "4":
				msgServ = "OOLF> ";
				break;
			case "5":
				msgServ = "LBUP> ";
				break;
			default:
				break;
		}
		return msgServ;
	}//fin_msgServ

/*************************************GETTER***********************************/
	public String getTypeDeNotif() {
		return typeDeNotif;
	}

	public String getIdSource() {
		return idSource;
	}

	public String getFlux() {
		return flux;
	}

	public String[] getMunem() {
		return munem;
	}
	//fin_getter

}//fin_class
