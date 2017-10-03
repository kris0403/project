#ifndef __RDPACKET_H__
#define __RDPACKET_H__

#include "rdframe.h"
#include "rdblock.h"
#include "matrix.h"
#include <vector>
#define blocks_per_frame 116

class RD_packet{
private:
	std::vector<RD_frame> frames;
	/*ID of packet*/
	int pid;
	/*there are "num" frames in a packet*/
	int num;
	/*frame matrix for packet level*/
	RD_Matrix frame_matrix;
	/*block matrix for frame level*/
	RD_Matrix block_matrix;

	int initPacket(int);


public:

	/**Get information from member**/
		//block level
	itpp::bvec getSymbolsFromBlockOfFrame(int, int);
	int getBlockIDFromBlockOfFrame(int, int);
	bool getValidFromBlockOfFrame(int, int);
	int getSymbolsSizeFromBlockOfFrame(int, int);
		//frame level
	int getFrameID(int);
	int getNumofBlocksFromFrame(int);
		//packet level
	int getPID();
	int getNumFrameofPacket();

	int getframe_matrix_k();

	/*Set the information of member*/
		//block level
	void setBIDofBlockofFrame(int, int);
	void setValidofBlockofFrame(int, int, bool);
	void setSymbolsofBlockofFrame(int, int, itpp::bvec);
		//frame level
	void setBlocksOfFrame(int);
	void setFIDOfFrame(int);
		//packet level
	void setPID(int);
	void setNum(int);

	//set the matrix of special LT code
	void setFrameMatrix();
	void setBlockMatrix();
	void setFBMatrix();

	void setFrame();

	void showAllPara();

	/**Encode & Decode packet level**/
	void Packet_level_encode();
	void Frame_level_encode();
	void Block_level_encode();
	void Block_level_decode();
	void Frame_level_decode();
	void Packet_level_decode();
	void RDEncode();
	void RDDecode(int);
	void BFDecode(int);

	RD_packet();
	RD_packet(int);
	~RD_packet();
};

#endif