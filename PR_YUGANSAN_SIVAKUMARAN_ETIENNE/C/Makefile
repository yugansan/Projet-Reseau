CC = gcc
CFLAGS = -Wall
LDFLAGS = -lpthread
EXEC = client

all: $(EXEC)

client: client.o
	$(CC) -o client client.o $(LDFLAGS)

client.o: client.c
	$(CC) -o client.o -c client.c $(CFLAGS)

clean:
	rm -rf *.o

mrproper: clean
	rm -rf $(EXEC)
