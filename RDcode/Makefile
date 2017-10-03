BLOCK_H = header/rdblock.h
BLOCK_C = src/rdblock.cpp
BLOCK_O = obj/rdblock.o

FRAME_H = header/rdframe.h
FRAME_C = src/rdframe.cpp
FRAME_O = obj/rdframe.o

PACKET_H = header/rdpacket.h
PACKET_C = src/rdpacket.cpp
PACKET_O = obj/rdpacket.o

MATRIX_H = header/matrix.h
MATRIX_C = src/matrix.cpp
MATRIX_O = obj/matrix.o

DATA_H = header/data.h
DATA_C = src/data.cpp
DATA_O = obj/data.o

MAIN_C = src/main.cpp
MAIN_O = obj/main.o

OBJS = $(BLOCK_O) $(FRAME_O) $(PACKET_O) $(MATRIX_O) $(DATA_O) $(MAIN_O)

###

main : $(OBJS)
	g++ -o $@ $(OBJS) -litpp

$(MAIN_O) : $(MAIN_C) $(FRAME_H)
	g++ -c -o $@ $(MAIN_C) -litpp

$(BLOCK_O) : $(BLOCK_C) $(BLOCK_H)
	g++ -c -o $@ $(BLOCK_C) -litpp

$(FRAME_O) : $(FRAME_C) $(FRAME_H) $(BLOCK_H)
	g++ -c -o $@ $(FRAME_C) -litpp

$(PACKET_O) : $(PACKET_C) $(PACKET_H) $(FRAME_H) $(BLOCK_H) $(MATRIX_H)
	g++ -c -o $@ $(PACKET_C) -litpp

$(MATRIX_O) : $(MATRIX_C) $(MATRIX_H)
	g++ -c -o $@ $(MATRIX_C)

$(DATA_O) : $(DATA_C) $(DATA_H) $(MATRIX_H) $(PACKET_H) $(FRAME_H) $(BLOCK_H)
	g++ -c -o $@ $(DATA_C) -litpp

clean :
	rm -f main $(OBJS)