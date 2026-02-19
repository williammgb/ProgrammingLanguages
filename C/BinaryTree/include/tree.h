#ifndef TREE_H
#define TREE_H

#include "node.h"
#include <stdio.h>

// tree manipulation
Node* insert(Node *root, int value) {
    if (root == NULL) 
        return create_node(value);
    
    if (value < root->value)
        root->left = insert(root->left, value);
    else if (value > root->value)
        root->right = insert(root->right, value);
    
    return root;
}

// Node* delete(Node *root, int value) {
//     }

void free_tree(Node* root) {
    if (!root) return;
    free_tree(root->left);
    free_tree(root->right);
    free(root);
}

// tree traversals and operations
Node* search(Node *root, int value) {
    if (!root || root->value==value)
        return root;
    
    if (value < root->value)
        return search(root->left, value);
    
    return search(root->right, value);
}

int height(Node *root) {
    if (!root) return -1;
    int hl = height(root->left);
    int hr = height(root->right);
    return (hl > hr ? hl : hr) + 1;
}

int count_nodes(Node *root) {
    if (!root) return 0;
    int cl = count_nodes(root->left);
    int cr = count_nodes(root->right);
    return 1 + cl + cr;
}

void inorder(Node *root) {
    if (!root) return;
    inorder(root->left);
    printf("%d ", root->value);
    inorder(root->right);
}

void preorder(Node* root) {
    if (!root) return;
    printf("%d ", root->value);
    preorder(root->left);
    preorder(root->right);
}

#endif