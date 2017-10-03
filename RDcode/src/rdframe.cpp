#include "../header/rdblock.h"
#include "../header/rdframe.h"
#include <itpp/itcomm.h>

using namespace itpp;
using namespace std;

/******************
 * Private Member *
 ******************/


int RD_frame::initFrame(int s)
{
	blocks.clear();
	fid = s;
	return 0;
}

/*****************
 * Public Member *
 *****************/
bvec RD_frame::getBlockSymbols(int bid){
	return blocks[bid].getSymbols();
}

int RD_frame::getBlockID(int bid){
	return blocks[bid].getID();
}

bool RD_frame::getBlockValid(int bid){
	return blocks[bid].getValid();
}

int RD_frame::getLengthofBlockSymbols(int bid){
	return blocks[bid].getLengthofSymbols();
}

int RD_frame::getFID(){
	return fid;
}

int RD_frame::getNumofBlocks(){
	return blocks.size();
}



void RD_frame::setBlocks()
{
	for(int i=0; i<116; i++)
	{
		RD_block b(i);
		blocks.push_back(b);
	}
	return ;
}

void RD_frame::setFID(int no)
{
	fid = no;
	return ;
}

void RD_frame::setBID(int bid){
	blocks[bid].setID(bid);
	return ;
}

void RD_frame::setBlockValid(int bid, bool v){
	blocks[bid].setValid(v);
	return ;
}

void RD_frame::setBlockSymbols(int bid, bvec data){
	blocks[bid].setSymbols(data);
	return ;
}



RD_frame::RD_frame(){
	initFrame(-1);
}

RD_frame::RD_frame(int s){
	initFrame(s);
}

RD_frame::~RD_frame(){
}