#include <stdio.h>
#include "node.h"
#include "tcpstream.h"
#include "tree.h"

int main(void){
    printf("ACTIVE...\n");
    int values[] = {5, 25, 20, 10, 15, 30, 50, 40, 35, 45};
    size_t size = sizeof(values) / sizeof(values[0]);
    Node *root = NULL;
    for (size_t i = 0; i < size; i++) {
        root = insert(root, values[i]);
    }

    send_tree(root);
    return 0;
}

// gcc src/sender.c -Iinclude -o sender