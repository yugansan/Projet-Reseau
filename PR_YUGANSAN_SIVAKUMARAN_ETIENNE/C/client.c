#include <stdio.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <netdb.h>
#include <arpa/inet.h>
#include <string.h>
#include <stdlib.h>
#include <pthread.h>
#include <unistd.h>

#define MAX_MESS 20

//fonction qui renvoie une sous-chaine de la chaine s
char *str_sub (const char *s, unsigned int start, unsigned int end){
   char *new_s = NULL;
   if (s != NULL && start <= end){
      new_s = calloc ((end - start + 2), sizeof (*new_s));
      if (new_s != NULL){
         int i;
         for (i = start; i <= end; i++)
         {
            new_s[i-start] = s[i];
         }
         new_s[i-start] = '\0';
      }
      else{
         fprintf (stderr, "Memoire insuffisante\n");
         exit (EXIT_FAILURE);
      }
   }
   return new_s;
}

//fonction pour passer de big endian à little endian et inversement
int inverserNombre(int nb1, int taille){
  char *chaine = calloc(taille, sizeof(char));
  sprintf(chaine, "%u", nb1);
  int i = 0;
  char chaine2[strlen(chaine)];
  for(i = 0; i < strlen(chaine); i++){
    chaine2[i] = chaine[strlen(chaine)-i-1];
  }
  //free(chaine);
  return atoi(chaine2);
}

//fonction pour coder le mot de passe enregistré par le serveur
char *codageMdp(int nb1){
  int nb = nb1/256;
  int rest = nb1%256;
  char *chaine  = calloc(6, sizeof(char));
  sprintf(chaine, "%u%u", rest, nb);
  return chaine;
}

//fonction qui retourne un nombre aléatoire entre min et max
int alea(int min, int max){
  return (min + (rand() % (max-min+1)));
}

//fonction pour lire une chaine de 200 caractère en TCP
char *lire(int sock){
  char *buff = calloc(200, sizeof(char));
  int recu=read(sock,buff,199*sizeof(char));
  buff[recu]='\0';
  char *mess = strtok(buff, "+++");
  free(buff);
  return mess;
}

//fonction pour lire pour la commande liste en TCP
char **lireListe(int sock){
  char *buff = calloc(200, sizeof(char));
  int recu=read(sock,buff,199*sizeof(char));
  buff[recu]='\0';
  char *debut = strtok(buff, "+++");
  if(strcmp(str_sub(debut, 0, 4), "RLIST") != 0){
    return NULL;
  }
  int taille = atoi(str_sub(debut, 6, strlen(debut)));

  char **listeMess = calloc(taille, sizeof(char *));
  int i = 0;
  listeMess[0] = debut;
  while(listeMess[i] != NULL && i <= taille){
    i++;
    listeMess[i] = calloc(taille*220, sizeof(char));
    listeMess[i] = strtok(NULL, "+++");
  }
  int finit = 0;
  if(i == taille + 1){
    finit = 1;
  }
  while(finit != 1){
    char *buffSuite = calloc(200, sizeof(char));
    int recu=read(sock,buffSuite,199*sizeof(char));
    buffSuite[recu]='\0';
    listeMess[i] = strtok(buffSuite, "+++");
    listeMess[i] = calloc(taille*220, sizeof(char));
    while(listeMess[i] != NULL && i <= taille){
      if(strcmp(str_sub(listeMess[i], 0, 4), "LINUM") != 0){
        char *prec = calloc(300, sizeof(char));
        strcpy(prec, listeMess[i-1]);
        listeMess[i-1] = calloc(taille*500, sizeof(char));
        sprintf(listeMess[i-1], "%s%s", prec, listeMess[i]);
        //free(prec);
        i--;
      }
      i++;
      listeMess[i] = calloc(taille*220, sizeof(char));
      listeMess[i] = strtok(NULL, "+++");
    }
    if(i == taille + 1){
      finit = 1;
    }
    //free(buffSuite);
  }
  //free(buff);
  //free(debut);
  return listeMess;
}

//fonction pour lire pour la commande message en TCP
char **lireMessage(int sock){
  char *buff = calloc(200, sizeof(char));
  int recu=read(sock,buff,199*sizeof(char));
  buff[recu]='\0';
  char *copie = calloc(200, sizeof(char));
  strcpy(copie, buff);

  char *debut = strtok(buff, "+++");
  char *type = strtok(debut, " ");

  if(strcmp(type, "SSEM>") != 0){
    char **listeMess = calloc(1, sizeof(char *));
    listeMess[0] = strtok(copie, "+++");
    return listeMess;
  }

  strtok(NULL, " ");
  int taille = atoi(strtok(NULL, " "));
  char **listeMess = calloc(taille, sizeof(char *));
  int i = 0;
  listeMess[0] = strtok(copie, "+++");
  while(listeMess[i] != NULL && i <= taille){
    i++;
    listeMess[i] = calloc(taille*220, sizeof(char));
    listeMess[i] = strtok(NULL, "+++");
  }
  int finit = 0;
  if(i == taille + 1){
    finit = 1;
  }
  while(finit != 1){
    char *buffSuite = calloc(200, sizeof(char));
    int recu=read(sock,buffSuite,199*sizeof(char));
    buffSuite[recu]='\0';
    listeMess[i] = calloc(taille*220, sizeof(char));
    listeMess[i] = strtok(buffSuite, "+++");
    while(listeMess[i] != NULL && i <= taille){
      if(strcmp(str_sub(listeMess[i], 0, 4), "MENUM") != 0){
        char *prec = calloc(300, sizeof(char));
        strcpy(prec, listeMess[i-1]);
        listeMess[i-1] = calloc(taille*500, sizeof(char));
        sprintf(listeMess[i-1], "%s%s", prec, listeMess[i]);
        i--;
        //free(prec);
      }
      i++;
      listeMess[i] = calloc(taille*220, sizeof(char));
      listeMess[i] = strtok(NULL, "+++");
    }
    if(i == taille + 1){
      finit = 1;
    }
    //free(buffSuite);
  }
  //free(buff);
  //free(debut);
  //free(type);
  return listeMess;
}

//fonction qui vérifie qu'une chaine de contient pas +++
int troisPlus(char *mess){
  int i = 0;
  for(i = 0; i < strlen(mess); i++){
    if((mess[i] == '+')&&(i < strlen(mess) - 2)&&(mess[i+1] == '+')&&(mess[i+2] == '+')){
      return 0;
    }
  }
  return 1;
}

//fonction qui vérifie un id donné
int verifId(char *id){
  if(strlen(id) > 8){
    return 0;
  }
  return troisPlus(id);
}

//fonction qui vérifie un mdp
int verifMdp(char *mdp){
  int i = 0;
  for(i = 0; i < strlen(mdp); i++){
    if(mdp[i] != '0' && mdp[i] != '1' && mdp[i] != '2' && mdp[i] != '3'
    && mdp[i] != '4' && mdp[i] != '5' && mdp[i] != '6' && mdp[i] != '7'
    && mdp[i] != '8' && mdp[i] != '9'){
      return 0;
    }
  }
  if(atoi(mdp) < 0 || atoi(mdp) > 65535){
    return 0;
  }
  return 1;
}

//fonction qui vérifie un port
int verifPort(char *port){
  int i = 0;
  for(i = 0; i < strlen(port); i++){
    if(port[i] != '0' && port[i] != '1' && port[i] != '2' && port[i] != '3'
    && port[i] != '4' && port[i] != '5' && port[i] != '6' && port[i] != '7'
    && port[i] != '8' && port[i] != '9'){
      return 0;
    }
  }
  if(atoi(port) < 0 || atoi(port) > 9999){
    return 0;
  }
  return 1;
}

//fonction qui à partir de l'entier pour le port, ajoute les zeros et renvoie
//une chaine de 4 char
char *chainePort(int portAlea){
  char *port = calloc(4, sizeof(char));
  if(portAlea < 10){
    sprintf(port, "000%u", portAlea);
  }
  else if(portAlea < 100){
    sprintf(port, "00%u", portAlea);
  }
  else if(portAlea < 1000){
    sprintf(port, "O%u", portAlea);
  }
  else{
    sprintf(port, "%u", portAlea);
  }
  return port;
}

//fonction pour se connecter au port UDP choisi et écoute sur le port
//affiche les notifications reçues
void *notification(void *i){
  int port = *((int *) i);
  int sockUDP  = socket(PF_INET, SOCK_DGRAM, 0);
  struct sockaddr_in address_sock;
  address_sock.sin_family = AF_INET;
  address_sock.sin_port = htons(port);
  address_sock.sin_addr.s_addr = htonl(INADDR_ANY);
  int r = bind(sockUDP, (struct sockaddr *)&address_sock, sizeof(struct sockaddr_in));
  if(r == 0){
    while(1){
      char *tampon = calloc(100, sizeof(char));
      int rec = recv(sockUDP, tampon, 100, 0);
      tampon[rec] = '\0';
      printf("Tampon : [%s]\n", tampon);
      char *nombre = calloc(2, sizeof(char));
      nombre[0] = tampon[1];
      nombre[1] = tampon[2];
      int inv = inverserNombre(atoi(nombre), 2);
      if(tampon[0] == '0'){
        printf("Vous avez une demande d'amitié.\n");
        printf("Vous avez %d notifications en attentes.\n", inv);
      }
      else if(tampon[0] == '1'){
        printf("On a accepté une de vos demandes d'amitié.\n");
        printf("Vous avez %d notifications en attentes.\n", inv);
      }
      else if(tampon[0] == '2'){
        printf("On a refusé une de vos demandes d'amitié.\n");
        printf("Vous avez %d notifications en attentes.\n", inv);
      }
      else if(tampon[0] == '3'){
        printf("Vous avez recu un message privé.\n");
        printf("Vous avez %d notifications en attentes.\n", inv);
      }
      else if(tampon[0] == '4'){
        printf("Vous avez recu un message public.\n");
        printf("Vous avez %d notifications en attentes.\n", inv);
      }
      else{
        printf("La notification reçu : [%s], est inconnue.\n", tampon);
      }
      //free(nombre);
    }
  }
  free(i);
  return NULL;
}

//fonction pour s'authentifier : insciption ou connexion
char *authentification(int sock, char *port){
  char inscrit[50];
  printf("Etes-vous inscrit? oui/non\n");
  scanf("%s", inscrit);
  if(strcmp(inscrit, "non") == 0){
    char *idc = calloc(8, sizeof(char));
    char *mdpc = calloc(2, sizeof(char));
    char *mdpc2 = calloc(2, sizeof(char));
    printf("Inscription\n");
    printf("Votre id:\n");
    scanf("%s", idc);
    printf("Mot de passe:\n");
    scanf("%s", mdpc);
    printf("Confirmez votre mot de passe:\n");
    scanf("%s", mdpc2);
    if(!verifId(idc) || !verifMdp(mdpc)){
      printf("Problème dans le mot de passe ou l'id, veuillez recommencer.\n");
      return authentification(sock, port);
    }
    if(strcmp(mdpc, mdpc2) == 0){
      char *message = calloc(200, sizeof(char));
      sprintf(message, "REGIS %s %s %s+++", idc, port, codageMdp(atoi(mdpc)));
      write(sock,message,strlen(message)*sizeof(char));
      //free(message);
      char *buff = lire(sock);
      if(strcmp(buff, "WELCO") == 0){
        printf("Vous êtes bien inscrit.\n");
        char ligneEnTrop[200];
        fgets(ligneEnTrop, sizeof(ligneEnTrop), stdin);
        return authentification(sock, port);
      }
      else if(strcmp(buff, "GOBYE") == 0){
        printf("L'inscription a été refusé.\n");
      }
      //free(buff);
    }
    //free(idc);
    //free(mdpc);
    //free(mdpc2);
  }
  else{
    char *idc = calloc(8, sizeof(char));
    char *mdpc = calloc(2, sizeof(char));
    printf("Connexion\n");
    printf("Votre id:\n");
    scanf("%s", idc);
    printf("Mot de passe:\n");
    scanf("%s", mdpc);
    if(!verifId(idc) || !verifMdp(mdpc)){
      printf("Problème dans le mot de passe ou l'id, veuillez recommencer.\n");
      return authentification(sock, port);
    }
    char *message = calloc(200, sizeof(char));;
    sprintf(message, "CONNE %s %s+++", idc, codageMdp(atoi(mdpc)));
    write(sock,message,strlen(message)*sizeof(char));
    //free(message);
    char *buff = lire(sock);
    if(strcmp(buff, "HELLO") == 0){
      printf("Vous êtes connecté.\n");
      char ligneEnTrop[200];
      fgets(ligneEnTrop, sizeof(ligneEnTrop), stdin);
      return idc;
    }
    else if(strcmp(buff, "GOBYE") == 0){
      printf("La connexion a été refusé.\n");
    }
    //free(idc);
    //free(mdpc);
  }
  return authentification(sock, port);
}

//fonction pour demander quelqu'un en amie
void friend(int sock, char *ligne){
  char *message = calloc(200, sizeof(char));;
  if(verifId(str_sub(ligne, 7, strlen(ligne)-2)) == 0){
    printf("L'id saisit n'est pas correct !!\n");
  }
  else{
    sprintf(message, "FRIE? %s+++", str_sub(ligne, 7, strlen(ligne)-2));
    write(sock,message,strlen(message)*sizeof(char));
    char *buff = lire(sock);
    if(strcmp(buff, "FRIE>") == 0){
      printf("La demande a bien été transmise.\n");
    }
    else if(strcmp(buff, "FRIE<") == 0){
      printf("Je ne reconnais pas le client %s/ Ou vous êtes déjà amis.\n", str_sub(ligne, 7, strlen(ligne)-2));
    }
    //free(buff);
  }
  //free(message);
  char ligneEnTrop[200];
  fgets(ligneEnTrop, sizeof(ligneEnTrop), stdin);
}

//fonction pour envoyer des messages à quelqu'un
void message(int sock, char*ligne){
  char *messageTot = calloc(200, sizeof(char));
  char *reste = str_sub(ligne, 8, strlen(ligne)-2);
  char *idMess = strtok(reste, " ");
  char *debutMess = str_sub(ligne, 9+strlen(idMess), strlen(ligne)-2);

  char **listeMess = calloc(MAX_MESS, sizeof(char *));
  int i = 0;
  listeMess[i] = calloc(MAX_MESS*200, sizeof(char));
  while(fgets(listeMess[i], 200*sizeof(char), stdin) && i < MAX_MESS && strcmp(listeMess[i], "\n") != 0){
    i++;
    listeMess[i] = calloc(MAX_MESS*200, sizeof(char));
  }

  int aTroisPlus = 0;
  sprintf(messageTot, "MESS? %s %s+++", idMess, chainePort(i+1));
  char **message = calloc((i+1), sizeof(char *));
  message[0] = calloc((i+1)*220, sizeof(char));
  sprintf(message[0], "MENUM 0 %s+++", debutMess);
  if(troisPlus(debutMess) == 0){
    aTroisPlus = 1;
  }

  int j = 1;
  for(j = 1; j <= i; j++){
    message[j] = calloc((i+1)*220, sizeof(char));
    sprintf(message[j], "MENUM %d %s+++", j, str_sub(listeMess[j-1], 0, strlen(listeMess[j-1])-2));
    if(troisPlus(str_sub(listeMess[j-1], 0, strlen(listeMess[j-1])-2)) == 0){
      aTroisPlus = 1;
    }
  }

  if(verifId(idMess) == 0){
    printf("L'id saisit n'est pas correct !!\n");
  }
  else if(aTroisPlus == 1){
    printf("Vous avez entré +++ dans le texte du message.\n");
  }
  else{
    write(sock,messageTot,strlen(messageTot)*sizeof(char));
    for(j = 0; j <= i; j++){
      write(sock,message[j],strlen(message[j])*sizeof(char));
    }
    printf("J'attend...\n");
    char *buff = lire(sock);
    if(strcmp(buff, "FRIE>") == 0){
      printf("La demande a bien été transmise.\n");
    }
    else if(strcmp(buff, "FRIE<") == 0){
      printf("Je ne reconnais pas le client\n");
    }
    //free(buff);
  }
  //free(messageTot);
  //free(reste);
  //free(idMess);
  //free(debutMess);
}

//fonction pour envoyer un message à tous ces amis...
void innondation(int sock, char *ligne){
  char *message = calloc(200, sizeof(char));
  sprintf(message, "FLOO? %s+++", str_sub(ligne, 12, strlen(ligne)-2));
  write(sock,message,strlen(message)*sizeof(char));
  char *buff = lire(sock);
  if(strcmp(buff, "FLOO>") == 0){
    printf("Le message a bien été transmise.\n");
  }
  char ligneEnTrop[200];
  fgets(ligneEnTrop, sizeof(ligneEnTrop), stdin);
  //free(message);
  //free(buff);
}

//fonction pour afficher la liste des personnes connectées
void liste(int sock, char *ligne){
  char *message = calloc(200, sizeof(char));
  sprintf(message, "LIST?+++");
  write(sock,message,strlen(message)*sizeof(char));

  printf("En attente d'une réponse du serveur...\n");
  char **buff = lireListe(sock);

  if(strcmp("RLIST", str_sub(buff[0], 0, 4)) == 0){
    printf("Voici la liste des clients du serveur :\n");
    int j = 1;
    int taille = atoi(str_sub(buff[0], 6, strlen(buff[0])));
    for(j = 1; j <= taille; j++){
      if(strcmp("LINUM", str_sub(buff[j], 0, 4)) == 0){
        printf("Le client d'id : %s\n", str_sub(buff[j], 6, strlen(buff[j])-1));
      }
    }
  }
  char ligneEnTrop[200];
  fgets(ligneEnTrop, sizeof(ligneEnTrop), stdin);
  //free(message);
  //free(buff);
}

//fonction pour lire les notifications
void flux(int sock, char *ligne){
  char *message = "CONSU+++";
  write(sock,message,strlen(message)*sizeof(char));
  //free(message);
  char **buff = lireMessage(sock);

  if(strcmp("SSEM>", str_sub(buff[0], 0, 4)) == 0){
    int j = 1;
    strtok(buff[0], " ");
    char *idEnv = strtok(NULL, " ");
    printf("Vous avez recu un message privé de %s\n", idEnv);
    int taille = atoi(strtok(NULL, " "));
    for(j = 1; j <= taille; j++){
      if(strcmp("MUNEM", str_sub(buff[j], 0, 4)) == 0){
        char *copie = calloc(strlen(buff[j]), sizeof(char));
        strcpy(copie, buff[j]);
        strtok(buff[j], " ");
        char *numMess = strtok(NULL, " ");
        printf("Message n°%d : %s.\n", atoi(numMess), str_sub(copie, (6+strlen(numMess))*sizeof(char), strlen(copie)-1));
      }
    }
    //free(idEnv);
  }

  else if(strcmp(str_sub(buff[0], 0, 4), "OOLF>") == 0){
    char *copie = calloc(strlen(buff[0]), sizeof(char));
    strcpy(copie, buff[0]);
    strtok(buff[0], " ");
    char *idMess = strtok(NULL, " ");
    printf("Message de %s : %s\n", idMess, str_sub(copie, (6+strlen(idMess))*sizeof(char), strlen(copie)-1));
  }

  else if(strcmp(str_sub(buff[0], 0, 4), "EIRF>") == 0){
    printf("%s vous a demandé en amitié.\n", str_sub(buff[0], 6, 9));
    printf("Voulez-vous être amie avec cette personne ? oui/non\n");
    char reponse[200];
    fgets(reponse, sizeof(reponse), stdin);
    if(strcmp(str_sub(reponse, 0, 2), "oui") == 0){
      char *accepte = "OKIRF+++";
      write(sock,accepte,strlen(accepte)*sizeof(char));
      char *buff1 = lire(sock);
      if(strcmp(buff1, "ACKRF") == 0){
        printf("La réponse a été envoyé.\n");
      }
      //free(buff1);
      //free(accepte);
    }
    else if(strcmp(str_sub(reponse, 0, 2), "non") == 0){
      char *accepte = "NOKRF+++";
      write(sock,accepte,strlen(accepte)*sizeof(char));
      char *buff1 = lire(sock);
      if(strcmp(buff1, "ACKRF") == 0){
        printf("La réponse a été envoyé.\n");
      }
      //free(buff1);
      //free(accepte);
    }
  }

  else if(strcmp(str_sub(buff[0], 0, 4), "FRIEN") == 0){
    printf("%s vous a accepté en amis.\n", str_sub(buff[0], 6, strlen(buff[0])-1));
  }

  else if(strcmp(str_sub(buff[0], 0, 4), "NOFRI") == 0){
    printf("%s vous a refusé de ses amis.\n", str_sub(buff[0], 6, strlen(buff[0])-1));
  }

  else if(strcmp(str_sub(buff[0], 0, 4), "LBUP>") == 0){
    printf("Vous avez recu une publicité.\n");
    printf("ip-diff : %s.\n", str_sub(buff[0], 6, 20));
    printf("port : %d.\n", atoi(str_sub(buff[0], 22, 25)));
    printf("%d\n", atoi(str_sub(buff[0], 27, strlen(buff[0])-1)));
  }

  else if(strcmp(str_sub(buff[0], 0, 4), "NOCON") == 0){
    printf("Vous n'avez plus aucun flux à consulter.\n");
  }

  else{
    printf("Requete du serveur inconnue.\n");
  }
  char ligneEnTrop[200];
  fgets(ligneEnTrop, sizeof(ligneEnTrop), stdin);
  free(buff);
}

//fonction pour se deconnecter
void deconnexion(int sock, char *liste){
  char *message = "IQUIT+++";
  write(sock,message,strlen(message)*sizeof(char));
  char *buff = lire(sock);
  if(strcmp(buff, "GOBYE") == 0){
    printf("Vous êtes déconnecté. A bientôt :)\n");
  }
  char ligneEnTrop[200];
  fgets(ligneEnTrop, sizeof(ligneEnTrop), stdin);
  //free(buff);
  //free(message);
}

//promotteurs à tester
//notification
//bug message longs
//liste
int main(int argc, char *argv []) {
  char *id = NULL;
  int portAlea;
  srand(time(NULL));
  portAlea = alea(1024,9999);
  char *port = chainePort(portAlea);
  int sock = socket(PF_INET,SOCK_STREAM,0);
  struct sockaddr_in *addressin;
  struct addrinfo *first_info;
  struct addrinfo hints;
  bzero(&hints,sizeof(struct addrinfo));
  hints.ai_family = AF_INET;
  hints.ai_socktype = SOCK_STREAM;
  int r;
  if(argc > 1 && verifPort(argv[1]) == 1){
    r = getaddrinfo("localhost", argv[1], &hints, &first_info);
  }
  else{
    r = getaddrinfo("localhost", "4242", &hints, &first_info);
  }
  pthread_t th;
  if(r == 0){
    if(first_info != NULL){
      addressin = (struct sockaddr_in *)first_info->ai_addr;
      int ret = connect(sock,(struct sockaddr *)addressin,(socklen_t)sizeof(struct sockaddr_in));
      if(ret == 0){
        while(1){
          if(id == NULL){
            id = authentification(sock, port);
            if(id != NULL){
              int *arg = malloc(sizeof(*arg));
              if ( arg == NULL ) {
                  fprintf(stderr, "Problème d'allocation de mémoire.\n");
                  exit(EXIT_FAILURE);
              }
              *arg = atoi(port);
              pthread_create(&th,NULL,notification,arg);
            }
          }
          else{
            char ligne[200];
            printf("%s> ", id);
            fgets(ligne, sizeof(ligne), stdin);
            if(strcmp(str_sub(ligne, 0, 5), "friend") == 0){
              friend(sock, ligne);
            }
            else if(strcmp(str_sub(ligne, 0, 6), "message") == 0){
              message(sock, ligne);
            }
            else if(strcmp(str_sub(ligne, 0, 10), "innondation") == 0){
              innondation(sock, ligne);
            }
            else if(strcmp(str_sub(ligne, 0, 4), "liste") == 0){
              liste(sock, ligne);
            }
            else if(strcmp(str_sub(ligne, 0, 3), "flux") == 0){
              flux(sock, ligne);
            }
            else if(strcmp(str_sub(ligne, 0, 10), "deconnexion") == 0){
              deconnexion(sock, ligne);
              pthread_cancel(th);
              close(sock);
              exit(EXIT_SUCCESS);
            }
          }
        }
      }
      else{
        printf("Probleme de connexion!\n");
      }
    }
  }
  return 0;
}
