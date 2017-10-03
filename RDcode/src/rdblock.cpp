#include "../header/rdblock.h"
#include <itpp/itcomm.h>

using namespace itpp;

/******************
 * Private Member *
 ******************/
int RD_block::initBlock(int s)
{
	//symbols.set_size(75);
	symbols.clear();
	valid = false;
	bid = s;
	return 0;
}

/*****************
 * Public Member *
 *****************/
int RD_block::getID()
{
	return bid;
}

bvec RD_block::getSymbols()
{
	return symbols;
}

bool RD_block::getValid()
{
	return valid;
}

int RD_block::getLengthofSymbols()
{
	return symbols.size();
}

void RD_block::setID(int no)
{
	bid = no;
	return ;
}

void RD_block::setValid(bool temp)
{
	valid = temp;
	return ;
}

void RD_block::setSymbols(bvec data)
{
	symbols = data;
	return ;
}

RD_block::RD_block(){
	initBlock(-1);
}
RD_block::RD_block(int s){
	initBlock(s); // s is the ID of block
}

RD_block::~RD_block(){
}