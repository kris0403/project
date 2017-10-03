#ifndef __RDBLOCK_H__
#define __RDBLOCK_H__

#include <itpp/itcomm.h>


class RD_block
{
private:
	//symbols for block
	itpp::bvec symbols;
	//block ID
	int bid;
	//flag for decoding, 0 for failed on block level EEC
	bool valid;

	int initBlock(int);

public:

	/**Get information from member**/
	int getID();
	itpp::bvec getSymbols();
	bool getValid();
	int getLengthofSymbols();

	/**Set the information of member**/
	void setID(int);
	void setValid(bool);
	void setSymbols(itpp::bvec);

	/*Encode & Decode on block level*/
	//void RSencode();

	/** Constructor, destructor**/
	RD_block();
	RD_block(int);
	~RD_block();


};


#endif