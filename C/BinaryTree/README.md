# Binary Tree with Serialization and Socket Transfer (C)
This project implements a binary tree in C using recursion and pointer manipulation. It supports common tree operations (e.g., search, traversals, height, node count) and demonstrates how to serialize and deserialize structured data (since pointers and memory addresses are process-specific) and transfer it between processes (via local IP, two terminals) using TCP sockets wrapped in a `FILE*` stream.

The project is built and tested for **Linux / WSL**.

## Usage
### Build
```bash
make
```
### Run (2 Terminals Required)
In **Terminal 1:**
```bash
./sender
```
In **Terminal 2:**
```bash
./receiver
```

The sender constructs and serializes the binary tree, then sends it over a local TCP connection.
The receiver reads the byte stream and reconstructs the original tree.