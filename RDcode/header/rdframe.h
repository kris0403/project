#ifndef __RDFRAME_H__
#define __RDFRAME_H__

#include "rdblock.h"
#include <vector>
#include <itpp/itcomm.h>

class RD_frame
{
private:
	//the blocks of frame (there are 116 blocks in one frame) 
	std::vector<RD_block> blocks;
	int fid;

	int initFrame(int);
public:
	/**Get information from member**/
	itpp::bvec getBlockSymbols(int);
	int getBlockID(int);
	bool getBlockValid(int);
	int getLengthofBlockSymbols(int);

	int getFID();
	int getNumofBlocks();
	

	/**Set the information of member**/
	void setBlocks();
	void setFID(int);

	void setBID(int);
	void setBlockValid(int, bool);
	void setBlockSymbols(int, itpp::bvec);
	

	/*Encode & Decode on block level*/
	//void RSencode();

	/** Constructor, destructor**/
	RD_frame();
	RD_frame(int);
	~RD_frame();
};
#endif