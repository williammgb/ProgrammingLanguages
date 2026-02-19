#ifndef SERIALIZE_H
#define SERIALIZE_H

#include <stdio.h>
#include "node.h"

void serialize(Node *root, FILE *out) {
    if (!root) {
        int marker = -1;
        fwrite(&marker, sizeof(int), 1, out);
        return;
    }
    fwrite(&root->value, sizeof(int), 1, out);
    serialize(root->left, out);
    serialize(root->right, out);
}

Node* deserialize(FILE *in) {
    int value;
    if (fread(&value, sizeof(int), 1, in) != 1)
        return NULL;
    if (value == -1)
        return NULL;
    Node *root = create_node(value);
    root->left = deserialize(in);
    root->right = deserialize(in);
    return root;
}

#endif