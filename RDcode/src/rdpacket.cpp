#include "../header/rdblock.h"
#include "../header/rdframe.h"
#include "../header/rdpacket.h"
#include "../header/matrix.h"
#include <itpp/itcomm.h>
#include <iostream>

using namespace itpp;
using namespace std;

/******************
 * Private Member *
 ******************/

int RD_packet::initPacket(int s){
	frames.clear();
	frame_matrix.clear();
	block_matrix.clear();
	pid = s;
	num = 0;
	return 0;
}

/*****************
 * Public Member *
 *****************/

/**Get information from member**/
//block level
bvec RD_packet::getSymbolsFromBlockOfFrame(int bid, int fid){
	return frames[fid].getBlockSymbols(bid);
}

int RD_packet::getBlockIDFromBlockOfFrame(int bid, int fid){
	return frames[fid].getBlockID(bid);
}

bool RD_packet::getValidFromBlockOfFrame(int bid, int fid){
	return frames[fid].getBlockValid(bid);
}

int RD_packet::getSymbolsSizeFromBlockOfFrame(int bid, int fid){
	return frames[fid].getLengthofBlockSymbols(bid);
}

//frame level

int RD_packet::getFrameID(int fid){
	return frames[fid].getFID();
}

int RD_packet::getNumofBlocksFromFrame(int fid){
	return frames[fid].getNumofBlocks();
}

//packet level

int RD_packet::getPID(){
	return pid;
}

int RD_packet::getNumFrameofPacket(){
	return num;
}

int RD_packet::getframe_matrix_k()
{
	return frame_matrix.k;
}

/*Set the information of member*/

//block level
void RD_packet::setBIDofBlockofFrame(int bid, int fid){
	frames[fid].setBID(bid);
	return ;
}

void RD_packet::setValidofBlockofFrame(int bid, int fid, bool v){
	frames[fid].setBlockValid(bid,v);
	return ;
}

void RD_packet::setSymbolsofBlockofFrame(int bid, int fid, bvec data){
	frames[fid].setBlockSymbols(bid, data);
	return ;
}

//frame level

void RD_packet::setBlocksOfFrame(int fid){
	frames[fid].setBlocks();
	return ;
}

void RD_packet::setFIDOfFrame(int fid){
	frames[fid].setFID(fid);
	return ;
}

//packet level
void RD_packet::setPID(int no){
	pid = no;
	return ;
}

void RD_packet::setNum(int temp){
	num = temp;
	return ;
}

void RD_packet::setFrameMatrix(){
	//cout << "Please enter the frame matrix: ";
	string name = "frame_matrix_8_3"; // default
	//cout << name << endl;
	frame_matrix.readMatrixFromFile(name);
	setNum( frame_matrix.k + frame_matrix.m );
	return ;
}

void RD_packet::setBlockMatrix(){
	//cout << "Please enter the block matrix: ";
	string name = "block_matrix_87_29"; //default
	//cout << name << endl;
	block_matrix.readMatrixFromFile(name);
	return ;
}

void RD_packet::setFBMatrix(){
	setFrameMatrix();
	setBlockMatrix();
	return ;
}

void RD_packet::setFrame(){
	for(int i=0; i<num; i++)
	{
		RD_frame f(i);
		f.setBlocks();
		frames.push_back(f);
	}
	return ;
}

void RD_packet::showAllPara(){
	cout << endl;
	cout << "---------show all parameter -------" << endl;
	cout << "Pid : " << pid << " Num : " << num << endl;
	cout << "frames : " << frames.size() << endl;

	/*for(int i=0; i<frames.size(); i++)
		cout << frames[i].getFID() << " ";
	cout << endl;
	for(int i=0; i<116; i++)
		cout << frames[0].getBlockID(i) <<" ";
	cout << endl;*/

	cout << "Frame Matrix" << endl;
	frame_matrix.showMatrix();
	cout << endl << "Block Matrix" << endl;
	block_matrix.showMatrix();
	cout << "--------------end------------------" << endl;
	return ;
}

/**Encode & Decode packet level**/
void RD_packet::Packet_level_encode()
{
	bvec temp;
	int k = frame_matrix.k;
	temp.set_size( getSymbolsSizeFromBlockOfFrame(0,0) );
	temp.clear();
	for(int i=0; i < frame_matrix.m; i++) //encode the "k+i"th frame
	{
		for(int j=0; j < blocks_per_frame; j++) // handle the jth block
		{
			temp.clear();
			for(int l=0; l < frame_matrix.getDegree( k + i ) ; l++)
			{
				temp = temp + getSymbolsFromBlockOfFrame( j, frame_matrix.getInfoVnode( k+i,l) );
			}
			setSymbolsofBlockofFrame( j, k+i, temp);
		}
	}

	return ;
}

void RD_packet::Frame_level_encode()
{
	bvec temp;
	temp.set_size( getSymbolsSizeFromBlockOfFrame(0,0) );
	temp.clear();

	int total_block = block_matrix.k + block_matrix.m;
	for(int i=0; i<total_block; i++)
	{
		//find the block whose degree is not equal 1
		if( block_matrix.v_node[i].size() != 1 )
		{
			//encode the frame level with "j"th frame
			for(int j=0; j < frame_matrix.k; j++)
			{
				temp.clear();
				for(int l=0; l < block_matrix.v_node[i].size(); l++)
					temp = temp + getSymbolsFromBlockOfFrame( block_matrix.v_node[i][l], j );
				setSymbolsofBlockofFrame( i, j, temp);
			}
		}
	}

	return;
}

void RD_packet::Block_level_encode()
{
	bvec temp, uncoded, coded;
	int m, t, NumCodeWords; //Reed-Solomon parameter
	m = 3;
	t = 1;
	NumCodeWords = 6;
	Reed_Solomon reed_solomon(m, t, 1);

	int total_block, total_frame, length;
	total_block = block_matrix.k + block_matrix.m;
	total_frame = frame_matrix.k + frame_matrix.m;
	length = 75 / ( NumCodeWords - 1 );	//default

	for(int i=0; i < total_frame; i++)
	{
		for(int j=0; j < total_block; j++)
		{	//RS encode on "j" block of "i" frame
			temp.clear();	uncoded.clear();	coded.clear();

			uncoded = getSymbolsFromBlockOfFrame( j, i);

			temp.set_size(length);
			temp.zeros();
			for(int l=0; l < (NumCodeWords-1); l++)
				temp = temp + uncoded(l*length, (l+1)*length-1);

			uncoded.ins(uncoded.size(), temp);
			coded = reed_solomon.encode(uncoded);

			setSymbolsofBlockofFrame( j, i, coded);
		}
	}

	return ;
}

void RD_packet::Block_level_decode()
{
	bvec temp, receive, decoded, zero;
	int m, t, NumCodeWords; //Reed-Solomon parameter
	m = 3;
	t = 1;
	NumCodeWords = 6;
	Reed_Solomon reed_solomon(m, t, 1);

	int total_block, total_frame, length;
	total_block = block_matrix.k + block_matrix.m;
	total_frame = frame_matrix.k + frame_matrix.m;
	length = 75 / ( NumCodeWords - 1 );	//default

	zero.set_size(length);
	zero.zeros();

	for(int i=0; i < total_frame; i++)
	{
		for(int j=0; j < total_block; j++)
		{
			if(getSymbolsSizeFromBlockOfFrame(j,i)!=0)
			{
				temp.clear();	receive.clear();	decoded.clear();
				receive = getSymbolsFromBlockOfFrame( j, i);

				decoded = reed_solomon.decode(receive);
				setSymbolsofBlockofFrame( j, i, decoded);


				temp.set_size(length);
				temp.zeros();
				for(int l=0; l < (NumCodeWords); l++)
					temp = temp + decoded(l*length, (l+1)*length-1);


				if(temp == zero)
					setValidofBlockofFrame( j, i, true);
				else
					setValidofBlockofFrame( j, i, false);
			}
		}
	}


	return ;
}

void RD_packet::Frame_level_decode()
{
	int total_frame, total_block, neig;
	bool flag;
	bvec temp;

	total_frame = num;
	total_block = block_matrix.k + block_matrix.m;
	neig = 0;
	temp.set_size(90);//default TODO

	for(int i=0; i<total_block; i++)
	{	// "i"th block
		neig = block_matrix.v_neighbor[i].size();
		for(int j=0; j<total_frame; j++)
		{	// "j"th frame
			if( !getValidFromBlockOfFrame(i,j) )
			{
				flag = true;
				temp.zeros();

				for(int l=0; l<neig; l++)
				{
					if( !getValidFromBlockOfFrame( block_matrix.v_neighbor[i][l], j) )
					{
						flag = false;
						break;
					}
				}

				if(flag)
				{
					for(int l=0; l<neig; l++)
						temp = temp + getSymbolsFromBlockOfFrame(block_matrix.v_neighbor[i][l], j);
					setSymbolsofBlockofFrame(i,j,temp);
					setValidofBlockofFrame(i,j,true);
				}
			}
		}		
	}



	return ;
}

void RD_packet::Packet_level_decode()
{
	int total_frame, total_block, neig;
	bool flag;
	bvec temp;

	total_frame = num;
	total_block = block_matrix.k + block_matrix.m;
	neig = 0;
	temp.set_size( 90 );//default TODO

	for(int i=0; i<total_frame; i++)
	{	// "i"th frame
		neig = frame_matrix.v_neighbor[i].size();
		for(int j=0; j<total_block; j++)
		{	// "j"th block
			if( !getValidFromBlockOfFrame(j,i) )
			{
				flag = true;
				temp.zeros();

				for(int l=0; l<neig; l++)
				{
					if( !getValidFromBlockOfFrame(j, frame_matrix.v_neighbor[i][l]) )
					{
						flag = false;
						break;
					}
				}

				if(flag)
				{
					for(int l=0; l<neig; l++)
					{
						temp = temp + getSymbolsFromBlockOfFrame(j, frame_matrix.v_neighbor[i][l]);
					}

					setSymbolsofBlockofFrame(j,i,temp);
					setValidofBlockofFrame(j,i,true);
				}
			}
		}
	}

	return ;
}

void RD_packet::RDEncode()
{
	Frame_level_encode();
	Packet_level_encode();
	Block_level_encode();
	return ;
}

void RD_packet::RDDecode(int round)
{
	Block_level_decode();
	for(int i=0; i<round; i++)
	{
		Frame_level_decode();
		Packet_level_decode();
	}
	return ;
}

void RD_packet::BFDecode(int round)
{
	for(int i=0; i<round; i++)
	{
		Frame_level_decode();
		Packet_level_decode();
	}
	return ;
}



RD_packet::RD_packet(){
	initPacket(-1);
}

RD_packet::RD_packet(int s){
	initPacket(s);
}

RD_packet::~RD_packet(){
}