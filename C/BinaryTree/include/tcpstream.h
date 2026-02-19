#ifndef TCPSTREAM_H
#define TCPSTREAM_H

#include <stdio.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <unistd.h>
#include "serialize.h"
#include "node.h"

void send_tree(Node *root) {
    // creates TCP socket
    int server_fd = socket(AF_INET, SOCK_STREAM, 0); 
    // defines how servers listens (port and connections are accepted from any network interface)
    struct sockaddr_in addr = {
        .sin_family = AF_INET,
        .sin_port = htons(8080),
        .sin_addr.s_addr = INADDR_ANY
    }; 
    // connect socket to port & wait for incoming connection
    bind(server_fd, (struct sockaddr*)&addr, sizeof(addr));
    listen(server_fd, 1);
    // if connection, return new (client) socket
    int client_fd = accept(server_fd, NULL, NULL);
    // convert socket into FILE * (as they can be treated the same way, both used for reading/writing)
    FILE *out = fdopen(client_fd, "wb");
    // write tree to socket, everything is sent immediately
    serialize(root, out);
    fflush(out);
    // close stream, shutdown server
    fclose(out);
    close(server_fd);
}

Node* receive_tree(const char *server_ip) {
    // create TCP socket
    int sock = socket(AF_INET, SOCK_STREAM, 0);
    // define where to connect
    struct sockaddr_in addr = {
        .sin_family = AF_INET,
        .sin_port = htons(8080),
        .sin_addr.s_addr = inet_addr(server_ip)
    };
    // initiates TCP connection
    connect(sock, (struct sockaddr*)&addr, sizeof(addr));
    // wrap as FILE *; read tree
    FILE *in = fdopen(sock, "rb");
    Node *root = deserialize(in);
    // close socket
    fclose(in);   
    return root;
}

#endif