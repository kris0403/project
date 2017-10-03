#ifndef __DATA_H__
#define __DATA_H__

#include "rdframe.h"
#include "rdblock.h"
#include "matrix.h"
#include "rdpacket.h"
#include <vector>
#include <string>

class Data{
private:
	
	void initData();
public:
	std::vector<RD_packet> orig;
	int num_orig;
	std::vector<RD_packet> rece;
	int num_rece;
	bool erasure[110];

	/*Compare*/
	void before_decode_compare(std::string);
	void after_decode_compare(std::string);

	/*Set information*/

	void set_orig_Matrix();
	void set_rece_Matrix();
	void set_all_Matrix();

	void set_orig(int);
	void set_rece(int);
	void clear_orig();

	/*Read data*/
	void read_orig(std::string, int);
	void read_rece(std::string, int);
	void read_to_decode_rece(std::string, int);

	void read_trace(std::string, int);

	/*Output*/
	void out_orig();	//before encode
	void out_encoded_orig();
	void out_rece(std::string);	//after decode
	void out_to_decoded_rece(std::string);

	/*Encode & Decode*/
	void orig_Encode();
	void rece_Encode();
	void orig_Decode(int);
	void rece_Decode(int);

	void rece_BFDecode(int);
	void rece_block_decode();
	void block_erasure(std::string, bool);
	void total_equal(std::string);

	void rand_orig_data(int,int);
	void decode_and_compare(std::string, int);

	Data();
	~Data();
};

#endif