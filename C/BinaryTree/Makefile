CC = gcc
CFLAGS = -Wall -Wextra -Iinclude # W* for warnings, I for where to find the includes

# build in given order
all: sender receiver

sender: src/sender.c 
	$(CC) $(CFLAGS) $^ -o sender

receiver: src/receiver.c 
	$(CC) $(CFLAGS) $^ -o receiver

# if you run 'make clean' the above compiled binary executables are removed
clean:
	rm -f sender receiver

# tells make that all and clean are tasks and not files
.PHONY: all clean

# compile everything by typing 'make' in Linux (WSL)