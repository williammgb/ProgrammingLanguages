#ifndef NODE_H
#define NODE_H

#include <stdlib.h>

// Define the node structure. 
// Pointers are always the best method for trees/graphs (inf loop, sharing, NULL, etc.).
typedef struct Node {
    int value;
    struct Node *left;
    struct Node *right;
} Node;

Node* create_node(int value) {
    Node *n = (Node*) malloc(sizeof *n);
    // If no memory address can be assigned, return NULL
    if (!n) return NULL;
    n->value = value;
    n->left = NULL;
    n->right = NULL;
    return n;
}

#endif