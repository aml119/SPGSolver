CC = g++
CFLAGS = -I/usr/include/boost -I/include
LFLAGS = -lboost_program_options -lpthread


ODIR = src/obj
_OBJ = Node.o Graph.o #Measure.o Solution.o
OBJ = $(patsubst %,$(ODIR)/%,$(_OBJ))


all: CPPSolver

clean:
	rm -f src/obj/*

CPPSolver: $(OBJ)
	$(CC) -o CPPSolver src/CPPSolver.cpp $(CFLAGS) $(LFLAGS) $(OBJ)

src/obj/Node.o: src/include/Node.hpp src/Node.cpp
	$(CC) -c -o src/obj/Node.o src/Node.cpp

src/obj/Graph.o: src/include/Graph.hpp src/Graph.cpp src/include/Node.hpp
	$(CC) -c -o src/obj/Graph.o src/Graph.cpp

src/obj/Measure.o: src/include/Measure.hpp src/Measure.cpp
	$(CC) -c -o src/obj/Measure.o src/Measure.cpp

src/obj/Solution.o: src/include/Solution.hpp src/Solution.cpp src/include/Node.hpp
	$(CC) -c -o src/obj/Solution.o src/Solution.hpp


# all:
# 	$(CC) $(CFLAGS) $(LFLAGS) CPPSolver.cpp


# CC=g++
# CFLAGS=-I/usr/include/boost/ -I./include/
# _HEADS = Graph.hpp Node.hpp Measure.hpp Solution.hpp
# HEADS = $(patsubst %,$(ODIR)/%,$(_OBJ))
# LDFLAGS = -lboost_program_options -lpthread
