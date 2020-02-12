POPA Vlad-Gabriel 323CB
Tema 2 PC

 
Structuri mesaje:

- UDP_MSG folosita pentru citirea mesajelor UDP primite.

- TCP_MSG folosita pentru transmiterea mesajelor de la server la clientii TCP.
Fata de structura UDP_MSG, aceasta structura mai are in plus si adresa
senderului UDP si portul de pe care transmite.

- USER_MSG folosita pentru transmiterea mesajelor de la user la server
Acesta are urmatoarea strucutra:
   ->campul type (uint8_t) cu urmatoarele valori:
        / 0 pentru un mesaj de conectare (trimiterea username-ului la sever)
        / 1 pentru un mesaj de subscribe
        / 2 pentru un mesaj de unsubscribe
    ->id (numele userului)
    ->topic (numele topicii)
    ->SF

- Message - folosita pentru a stoca in server mesajele care trebuie pastrate
conform politicii sotre & forward. Un mesaj ce trebuie pastrat nu va fi 
duplicat pentru fiecare user, in schimb fiecare user pastreaza pointeri
la mesajele pe care trebuie sa le pastreze. Cand nu mai exista utilizatori
care sa aiba nevoie de un mesaj acesta este eliminat.

Server.
Serverul asculta initial pe 2 socketuri cel UDP si cel TCP.
Atunci cand un nou client se inregistreaza acesta trimite un
mesaj de tip USER_MSG cu type = 0 in care isi specifica usernameul.
Daca exista deja un utiliator cu acel nume care e online, utilizatorul
nou ia locul celui care e de mai mult timp online. 

Cand ajunge un mesaj de la un client UDP, se cauta multimile cu useri 
care au dat subscribe la topica primita. Exista 2 mapuri ce retin
userii inrolati la topici. Una pentru subscritiile cu SF (SFtopics) si
una pentru subscriptiile fara SF (nonSFtopics). Daca un user da 
subscribe la aceeasi topica si cu SF = 1 si cu SF = 0 atunci 
se va lua in considerare doar cea cu SF = 1. Pentru fiecare
user din mutlime se apeleaza fie functia sendOrThrow care trimite
mesajul catre clientul TCP daca acesta e logat si nu face nimic
daca nu e, fie functia sendOrStore care trimite mesajul daca
clientul e online sau pastreaza un pointer (iterator) catre
mesaj daca acesta nu e online.

Mesajele ce trebuie pastrate se pun in strucutra 
list<Message> unsent_messages. Un element de tip Message are 
o variabila count care retine cati useri mai trebuie sa primeasca
acel mesaj. Daca count ajunge pe 0 atunci mesajul e eliminat din lista.
Am folosit strucutra sock_to_user pentru a accesa rapid userii
avand la dispozitie doar socketul lor (folositor la deconectare).

Subscriber.
Un subscriber asculta de la doua socketuri. Socketul 0 pentru stdin
si socketul alocat pentru comunicarea cu serverul. Cand primeste
o comanda de la tastatura aceasta verifica validitatea comenzii
(daca comanda nu are structura ceruta va fi ignorat), si apoi
alcatuieste un mesaj USER_MSG pe care il trimite la server.
Cand se primeste ceva de la server se identifica tipul de informatie
transmis si se alcatuieste un mesaj de afisare.
