#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <string.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <netinet/tcp.h>
#include <arpa/inet.h>
#include <netdb.h>
#include <ctype.h>
#include <bits/stdc++.h>
#include <cmath>
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
	vector<Message*> unsent_messages;
}User;

//o topica este un set de id-uri de utilizatori
typedef unordered_set<string> Topic;


void usage(char *file)
{
	fprintf(stderr, "Usage: %s user_id server_address server_port\n", file);
	exit(0);
}

int main(int argc, char *argv[])
{
	char id[11];
	int sockfd, n, ret;
	struct sockaddr_in serv_addr;
	char buffer[BUFLEN];
	int bytes_read = 0;
	fd_set read_fds;	// multimea de citire folosita in select()
	fd_set tmp_fds;

	FD_ZERO(&read_fds);
	FD_ZERO(&tmp_fds);
	

	if (argc < 3) {
		usage(argv[0]);
	}

	if(strlen(argv[1]) > 10){
		printf("Id must have 10 characters or fewer.\n");
		return 0;
	}
	strcpy(id, argv[1]);
	char aux[BUFLEN];
	memset(aux, 0, BUFLEN);

	sockfd = socket(AF_INET, SOCK_STREAM, 0);
	DIE(sockfd < 0, "socket");

	serv_addr.sin_family = AF_INET;
	serv_addr.sin_port = htons(atoi(argv[3]));
	ret = inet_aton(argv[2], &serv_addr.sin_addr);
	DIE(ret == 0, "inet_aton");

	//ne conectam la server
	ret = connect(sockfd, (struct sockaddr*) &serv_addr, sizeof(serv_addr));
	DIE(ret < 0, "connect");

	//activam TCP_NODELAY
	int option = 1;
	setsockopt(sockfd, IPPROTO_TCP, TCP_NODELAY,(void*)(&option), sizeof(option));

	// se trimite catre server un mesaj de autentificare
	USER_MSG connect_msg;
	connect_msg.type = 0;
	strcpy(connect_msg.id, id);
	memcpy(buffer, &connect_msg, sizeof(connect_msg));
	n = send(sockfd, buffer, sizeof(connect_msg), 0);
	DIE(n < 0, "send");
	
	// se adauga socketi in multime
	FD_SET(sockfd, &read_fds);
	FD_SET(0, &read_fds);
	while (1) {

		tmp_fds = read_fds; 
		
		ret = select(sockfd + 1, &tmp_fds, NULL, NULL, NULL);
		DIE(ret < 0, "select");
		//daca am primit input de la tastatura
		if(FD_ISSET(0, &tmp_fds)){

			// se citeste de la tastatura
			memset(buffer, 0, BUFLEN);
			fgets(buffer, BUFLEN - 1, stdin);
			
			//mesaj de iesire
			if (strncmp(buffer, "exit", 4) == 0) {
				break;
			}

			// identificam comanda topica si valoarea SF
			char* command = strtok(buffer, " ");
			char* topic = strtok(NULL," ");
			char* SF_str = strtok(NULL, "\n");
			// verificam corectitudinea inputului
			if(command == NULL || topic == NULL ){
				continue;
			}
			if(SF_str != NULL && (strlen(SF_str) != 1 || !isdigit(SF_str[0]) )){
				continue;
			}
			// convertim SF la int
			uint8_t SF = 0;
			if(SF_str != NULL){
				SF = atoi(SF_str);
				if(SF != 1 && SF != 0) continue;
			}
			//in functie de tipul mesajului completam un mesaj
			USER_MSG msg;
			memset(&msg, 0, sizeof(msg));
			if(strncmp(buffer, "subscribe", 9) == 0){
				if(SF_str == NULL) continue;
				msg.type = 1;
				msg.SF = SF;
			} else if(strncmp(buffer, "unsubscribe", 11) == 0){
				topic[strlen(topic) - 1] = '\0';
				msg.type = 2;
			} else{
				continue;
			}

			//salvam comanda si topica
			char cmd[15];
			char t[51];
			strncpy(cmd, buffer, 11);
			strncpy(t, topic, 51);

			strcpy(msg.id, id);
			strcpy(msg.topic, topic);
			memcpy(buffer, &msg, sizeof(msg));
			
			// se trimite mesaj la server
			n = send(sockfd, buffer, sizeof(msg), 0);
			DIE(n < 0, "send");
			
			if(strncmp(cmd, "subscribe", 9) == 0){
				cout<<"subscribed "<<t<<".\n";
			} else if(strncmp(cmd, "unsubscribe", 11) == 0){
				cout<<"unsubscribed "<<t<<".\n";
			}
			fflush(stdout);
		}
		if(FD_ISSET(sockfd, &tmp_fds)){
				//daca se primeste ceva de la server
				//asiguram ca primim mesajul prin citiri succesive
				memset(buffer, 0, BUFLEN);
				memcpy(buffer, aux, bytes_read);
				while(bytes_read < sizeof(TCP_MSG)){
				n = recv(sockfd, buffer + bytes_read, sizeof(TCP_MSG), 0);
				if(n == 0) {break;}
				DIE(n < 0, "recv");
				bytes_read += n;
				}
				if(n == 0) break;
				bytes_read -= sizeof(TCP_MSG);
				memcpy(aux, buffer + sizeof(TCP_MSG),bytes_read);


				TCP_MSG tcp_msg;
				memcpy(&tcp_msg, buffer, sizeof(TCP_MSG));
				//identificam componentele mesajului primit
				//construim stringul de output
				char sign_bit;
				char type[50] = {0};
				char message[1501] = {0};
                switch(tcp_msg.message.type){
                    case 0:
                            //cazul INT
							strcpy(type, "INT");
                            uint32_t int_number;
                            memcpy(&sign_bit, tcp_msg.message.payload, 1);
                            memcpy(&int_number, tcp_msg.message.payload + 1, 4);
							int_number = ntohl(int_number);
							if(sign_bit == 1){
								sprintf(message, "%d", int_number);
							} else{
								sprintf(message, "-%d", int_number);
							}
                            break;
                    case 1:
							uint16_t short_real_number;
							strcpy(type, "SHORT_REAL");
							memcpy(&short_real_number, tcp_msg.message.payload, 2);
							short_real_number = ntohs(short_real_number);
							sprintf(message, "%.2f", ((float)(short_real_number))/100);
                            break;
                    case 2:
					        uint32_t float_number;
							uint8_t power;
							strcpy(type, "FLOAT");
							memcpy(&sign_bit, tcp_msg.message.payload, 1);
							memcpy(&float_number, tcp_msg.message.payload + 1, 4);
							memcpy(&power, tcp_msg.message.payload + 5, 1);
							float_number = ntohl(float_number);
							if(sign_bit == 1){
								sprintf(message ,"-%.8g",  ((double)float_number)/pow(10,power));
							} else {
								sprintf(message ,"%.8g", ((double)float_number)/pow(10,power));
							}
                            break;
                    case 3:
							sprintf(message, "%s", tcp_msg.message.payload);
							strcpy(type, "STRING");
                            break;
                    default:
                            continue;
                }

				//afisam mesajul formatat
				printf("%s:%u - %s - %s - %s\n", tcp_msg.sender_ip, tcp_msg.port, tcp_msg.message.topic, type, message);
				fflush(stdout);

		}

	}

	close(sockfd);

	return 0;
}
