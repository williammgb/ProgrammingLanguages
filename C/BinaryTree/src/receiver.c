#include <stdio.h>
#include "node.h"
#include "tcpstream.h"
#include "tree.h"

int main(void) {
    printf("RECEIVING...\n");
    Node *root = receive_tree("127.0.0.1"); // localhost, points back to own machine
    inorder(root);
    return 0;
}

// gcc src/receiver.c -Iinclude -o receiver