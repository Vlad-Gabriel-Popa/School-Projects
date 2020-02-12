#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <netinet/tcp.h>
#include <bits/stdc++.h>
#include "helpers.h"

using namespace std;



typedef struct udp_msg{
    char topic[50];
    uint8_t  type;
    char payload[1500];
}UDP_MSG;

typedef struct tcp_msg{
	char sender_ip[16];
	int port;
	UDP_MSG message;

}TCP_MSG;

typedef struct user_msg{
	//type = 0 pt connect  type = 1 pt subscribe type = 2 pt unsubscribe
	uint8_t type;
	char id[11];
	char topic[51];
	uint8_t SF;
}USER_MSG;

//count retine numarul de useri care nu au primit inca mesajul
typedef struct message{
	TCP_MSG tcp_msg;
	int count;
}Message;

typedef struct user{
	string id;
	int sockfd;
	bool is_online;
	vector<list<Message>::iterator> unsent_messages;
}User;

//o topica este un set de id-uri de utilizatori
typedef unordered_set<string> Topic;




void usage(char *file)
{
	fprintf(stderr, "Usage: %s server_port\n", file);
	exit(0);
}

// trimite mesajul daca userul e online. Daca nu e, il ignora
void sendOrThrow(User &user, list<Message>::iterator &msg_itr){
	if(user.is_online){
		int MSG_SIZE = sizeof(TCP_MSG);
		char buffer[MSG_SIZE];
		memcpy(buffer,&(msg_itr->tcp_msg), MSG_SIZE);
		int n = send(user.sockfd, buffer, MSG_SIZE, 0);
		DIE(n < 0, "send in sendOrThrow");
	}
}
// trimite mesajul daca userul e online. Daca nu e, il salveaza
void sendOrStore(User &user, list<Message>::iterator &msg_itr){
	if(user.is_online){
		int MSG_SIZE = sizeof(TCP_MSG);
		char buffer[MSG_SIZE];
		memcpy(buffer, &(msg_itr->tcp_msg), MSG_SIZE);
		int n = send(user.sockfd, buffer, MSG_SIZE, 0);
		DIE(n < 0, "send in sendOrStore");
		msg_itr->count--;
	} else{
		user.unsent_messages.push_back(msg_itr);
	}
}


int main(int argc, char *argv[])
{
	int tcp_sockfd, udp_sockfd, newsockfd, portno;
	char buffer[BUFLEN];
	char aux_TCP[BUFLEN];
	char aux_UDP[BUFLEN];
	int bytes_read_UDP = 0, bytes_read_TCP = 0;
	struct sockaddr_in serv_addr, cli_addr;
	int n, i, ret;
	socklen_t clilen;

	fd_set read_fds;	// multimea de citire folosita in select()
	fd_set tmp_fds;		// multime folosita temporar
	int fdmax;			// valoare maxima fd din multimea read_fds

	if (argc < 2) {
		usage(argv[0]);
	}

	//Map cu useri.
	//Cheie: id-ul unui user.  Valoare: informatiile legate de acel user
	map<string, User> users;

	//Map cu topici
	//Cheie: numele unei topici. Vloare: informatiile legate de acea topica
	map<string, Topic> nonSF_topics;
	map<string, Topic> SF_topics;

	//Retine mesajele ce mai trebuie pastrate( pt useri offline cu SF = 1)
	//Mesajele vor fi retinute o singura data indiferent cati useri trebuie
	//sa le primeasca.
	list<Message> unsent_messages; 

	//Face legatura intre un socket si un user
	map<int, string> sock_to_user;


	// se goleste multimea de descriptori de citire (read_fds) si multimea temporara (tmp_fds)
	FD_ZERO(&read_fds);
	FD_ZERO(&tmp_fds);

	//deschidem socketurile UDP si TCP
	tcp_sockfd = socket(AF_INET, SOCK_STREAM, 0);
	udp_sockfd = socket(PF_INET, SOCK_DGRAM, 0);
	DIE(tcp_sockfd < 0, "socket tcp error");
	DIE(udp_sockfd < 0, "socket udp error");

	//activam optiunea TCP_NODEALAY
	int option = 1;
	setsockopt(tcp_sockfd, IPPROTO_TCP, TCP_NODELAY,(void*)(&option), sizeof(option));

	portno = atoi(argv[1]);
	DIE(portno == 0, "atoi");

	memset((char *) &serv_addr, 0, sizeof(serv_addr));
	serv_addr.sin_family = AF_INET;
	serv_addr.sin_port = htons(portno);
	serv_addr.sin_addr.s_addr = INADDR_ANY;

	ret = bind(tcp_sockfd, (struct sockaddr *) &serv_addr, sizeof(struct sockaddr_in));
	DIE(ret < 0, "bind tcp error");
	ret = bind(udp_sockfd, (struct sockaddr *) &serv_addr, sizeof(struct sockaddr_in));
	DIE(ret < 0, "bind udp error");

	ret = listen(tcp_sockfd, MAX_CLIENTS);
	DIE(ret < 0, "listen");

	int k = 0;
	// se adauga noul file descriptor (socketul pe care se asculta conexiuni) in multimea read_fds
	FD_SET(tcp_sockfd, &read_fds);
	FD_SET(udp_sockfd, &read_fds);
	FD_SET(0, &read_fds);
	fdmax = max(tcp_sockfd, udp_sockfd);

	while (1) {
		tmp_fds = read_fds; 
		
		ret = select(fdmax + 1, &tmp_fds, NULL, NULL, NULL);
		DIE(ret < 0, "select");

		for (i = 0; i <= fdmax; i++) {
			if (FD_ISSET(i, &tmp_fds)) {
				//daca primim input de la tastatura
				if(i == 0){
					// se citeste de la tastatura
					memset(buffer, 0, BUFLEN);
					fgets(buffer, BUFLEN - 1, stdin);
					
					if (strncmp(buffer, "exit", 4) == 0) {
						for(pair<int,string> p : sock_to_user){
							close(p.first);
						}
						close(tcp_sockfd);
						close(udp_sockfd);
						return 0;
					}
				} else if (i == tcp_sockfd) {

					// a venit o cerere de conexiune pe socketul inactiv (cel cu listen),
					// pe care serverul o accepta
					clilen = sizeof(cli_addr);
					newsockfd = accept(tcp_sockfd, (struct sockaddr *) &cli_addr, &clilen);
					DIE(newsockfd < 0, "accept");					

					//activam TCP_NODELAY
					setsockopt(newsockfd, IPPROTO_TCP, TCP_NODELAY,(void*)(&option), sizeof(option));
					
					// se adauga noul socket intors de accept() la multimea descriptorilor de citire
					FD_SET(newsockfd, &read_fds);
					if (newsockfd > fdmax) { 
						fdmax = newsockfd;
					}
				// daca primim un mesaj de la un client UDP		
				} else if(i == udp_sockfd){
					
					//citim mesajul
					clilen = sizeof(cli_addr);
					memset(buffer, 0, BUFLEN);
                  	n = recvfrom(i, buffer ,sizeof(UDP_MSG), 0, (struct sockaddr *) &cli_addr, &clilen);
					DIE(n < 0, "recv");
					if(n == 0) continue;

                    //citim ip-ul senedrului si portul de pe care transmite
                    char udp_ip[50];
                    sprintf(udp_ip, "%s", inet_ntoa(cli_addr.sin_addr));
                    int udp_port = ntohs(cli_addr.sin_port);

                    //extragem mesajul
                    UDP_MSG message;
                    memcpy(&message, buffer, sizeof(UDP_MSG));			
					
					//construim mesajul ce va fi transmis catre clientii tcp
					Message msg;
					msg.count = 0;
					strcpy(msg.tcp_msg.sender_ip, udp_ip);
					msg.tcp_msg.port = udp_port;
					msg.tcp_msg.message = message;	
					
					//presupunem initial ca mesajul trebuie pastrat
					unsent_messages.push_front(msg);
					list<Message>::iterator msg_itr = unsent_messages.begin();

					  
					string topic_name = message.topic;
					map<string, Topic>::iterator it;

					//cautam useri abonati la topica mesajului cu SF = 1;
					it = SF_topics.find(topic_name);
					//daca exista useri
					if(it != SF_topics.end()){
						Topic topic = it->second;
						//initializam count-ul la numarul  de useri de tip SF
						//count se va decrementa daca mesajul e trimis
						//daca userul e offline acesta nu se va decrementa
						msg_itr->count = topic.size();

						//decidem ce e de facut cu mesajul pentru fiecare user
						for(string user_id : topic){
							User &user = (users.find(user_id))->second;
							sendOrStore(user, msg_itr);
						}
					} else {
						//cautam useri abonati la topica mesajului cu SF = 0;
						it = nonSF_topics.find(topic_name);
						//daca exista useri
						if(it != nonSF_topics.end()){
							Topic topic = it->second;

							//decidem ce e de facut cu mesajul pentru fiecare user
							for(string user_id : topic){
								map<string,User>::iterator itt = users.find(user_id);
								if(itt != users.end()){
								User user = itt->second;
								sendOrThrow(user, msg_itr);
								}
							}
						}
					}

					//daca niciun user nu are nevoie ca acest mesaj sa fie
					//pastrat atunci il eliminam
					if(msg_itr->count == 0){
						unsent_messages.pop_front();
					}
                }else{
					// s-au primit date pe unul din socketii de client TCP,
					// asa ca serverul trebuie sa le receptioneze
					memset(buffer, 0, BUFLEN);
					clilen = sizeof(cli_addr);
					
					n = recvfrom(i, buffer, sizeof(USER_MSG), 0, (struct sockaddr *) &cli_addr, &clilen);
					DIE(n < 0, "recv");

					if (n == 0) {
						// conexiunea s-a inchis
						map<int, string>::iterator itr = sock_to_user.find(i);
						if(itr != sock_to_user.end()){
							string username = itr->second;
							map<string, User>::iterator it = users.find(username);
							if(it != users.end()){
								User &user = it->second;
								user.is_online = false;
							}

							cout<<"Client ("<<username<<") disconnected.\n";
						}
						sock_to_user.erase(i);
						close(i);
						
						// se scoate din multimea de citire socketul inchis 
						FD_CLR(i, &read_fds);
					} else {
						// altfel citim mesajul primit de la clientul TCP
						USER_MSG user_message;
						memcpy(&user_message, buffer, sizeof(USER_MSG));

						map<string, Topic>::iterator it;
						map<string,User>::iterator itr;
						string topic_name = user_message.topic;
						string username = user_message.id;
						//identificam tipul mesajului
						switch(user_message.type){
							case 0: 
									// cazul connect (userul incearca sa se logheze la server)
									sock_to_user[i] = username;
									itr = users.find(username);
									cout<<"New client ("<<username<<") connected from "<<
									inet_ntoa(cli_addr.sin_addr)<<":"<<ntohs(cli_addr.sin_port)<<".\n";
									fflush(stdout);

									//Vedem daca userul a mai fost logat inainte.
									//Daca un user incearca conectarea la server cu un
									//username care e activ (deja e cineva online cu acel username)
									//atunci el va deveni noul detinator al acelui username
									//celelalt user fiind uitat
									if(itr == users.end()){
										User new_user;
										new_user.id = username;
										new_user.sockfd = i;
										new_user.is_online = true;
										users[username] = new_user;
									} else{
										User &user = itr->second;
										user.is_online = true;
										user.sockfd = i;
										
										//cautam daca exista mesaje SF care trebuie trimise
										for(list<Message>::iterator &msg : user.unsent_messages){
											sendOrStore(user, msg);
											//daca mesajul nu mi trebuie retinut atuni se elimina
											if(msg->count == 0){
												unsent_messages.erase(msg);
											}
										}
										user.unsent_messages.clear();
									}
									break;

							case 1:
									// cazul subscribe (userul incearca sa dea subscribe la o topica)
									// adaugam id-ul userului in topica ceruta, daca exista
									// de asemenea asiguram ca acesta e introdus o singura data
									if(user_message.SF == 0){
										it = nonSF_topics.find(topic_name);
										if(it != nonSF_topics.end()){
											Topic &topic = it->second;
											if(topic.find(user_message.id) == topic.end()){
												topic.insert(user_message.id);
											}
										} else{
											unordered_set<string> new_topic;
											new_topic.insert(user_message.id);
											nonSF_topics[user_message.topic] = new_topic;
										}	
									} else if (user_message.SF == 1){
										it = SF_topics.find(topic_name);
										if(it != SF_topics.end()){
											Topic &topic = it->second;
											if(topic.find(user_message.id) == topic.end()){
												topic.insert(user_message.id);
											}
										} else{
											unordered_set<string> new_topic;
											new_topic.insert(user_message.id);
											SF_topics[user_message.topic] = new_topic;
										}
									}
									break;
							case 2:	
									//cazul unsubscribe (userul incearca sa dea unsubscribe la o topica)
									it = nonSF_topics.find(topic_name);
									if(it != nonSF_topics.end()){
										(it->second).erase(username);	
									}
									it = SF_topics.find(topic_name);
									if(it != SF_topics.end()){
										(it->second).erase(username);	
									}
									break;
						}
					
					}
				}
			}
		}
	}

	close(tcp_sockfd);
	close(udp_sockfd);

	return 0;
}
