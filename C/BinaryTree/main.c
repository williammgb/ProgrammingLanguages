#include "tree.h"
#include <stdio.h>

int main(){
    Node *root = NULL;
    // instead of below, create array with numbers and iterate through them
    root = insert(root, 10);
    root = insert(root, 5);
    root = insert(root, 15);

    inorder(root);
    Node* five = search(root, 5);
    // always check for NULL & always cast to void * with %p
    if (five) {
        printf("\nAddress of node 5: %p", (void *)five);
    } else {
        printf("\nNode not found.");
    }
     
    return 0;
}

// gcc -Iinclude main.c -o binary tree