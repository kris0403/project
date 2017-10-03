#include "../header/rdblock.h"
#include "../header/rdframe.h"
#include "../header/rdpacket.h"
#include "../header/data.h"
#include "../header/matrix.h"

#include <itpp/itcomm.h>
#include <iostream>
#include <fstream>
#include <unistd.h>
#include <time.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <string>
#include <iomanip>

using namespace itpp;
using namespace std;

/******************
 * Private Member *
 ******************/

void Data::initData(){
	orig.clear();
	rece.clear();
	num_orig = 0;
	num_rece = 0;
	for(int i=0; i<110; i++)
		erasure[i] = true;

	return ;
}

 /*****************
 * Public Member *
 *****************/

/*Compare*/

void Data::before_decode_compare(string date){

	vector<BERC> berc;
	float temp = 0;
	for(int i=0; i<116; i++)
	{
		BERC b;
		b.clear();
		berc.push_back(b);
	}
	if(num_orig != num_rece)
		cout << "Something error on num_rece != num_orig" << endl;
	else
	{
		for(int i=0; i<num_orig; i++)
		{
			for(int j=0; j<orig[i].getNumFrameofPacket(); j++)
			{
				if( !erasure[ (i%2)+j*2+(i/2)*22] )
				{
					for(int l=0; l<116; l++)
						berc[l].count( orig[i].getSymbolsFromBlockOfFrame(l,j), 
							rece[i].getSymbolsFromBlockOfFrame(l,j) );
				}
				else
					temp ++;
			}
		}



		fstream f;

		string path;
		char buf[80];
		float error = 0;
		float total = 0;
		getcwd(buf, sizeof(buf));
		path = buf;
		path = path + "/" + date + "/result";

		f.open( path.c_str(), ios::app | ios::out);

		f << "---Before_decode_compare---" << endl << endl;;

		for(int i=0; i<116; i++)
		{
			f << setw(5) << i << " : " << setw(8) << berc[i].get_errorrate();
			error = error + berc[i].get_errors();
			total = total + berc[i].get_total_bits();

			if(i%5 == 4)
				f << endl;
		}

		f << endl << endl << "total error rate without erasure: " << error/total << endl;
		f << "total bits: " << total << endl;
		f << "total error rate with erasure: " ;
		f << ( error+ (temp*116*126) )/( total+(temp*116*126) ) << endl;
		f << "total bits: " << ( total+(temp*116*126) ) << endl ;

		f << endl << "---End of Before_decode_compare ---" << endl << endl;
		f.close();
	}

	berc.clear();
	return ;

}

void Data::after_decode_compare(string date)
{
	vector<BERC> berc;
	float test[116] = {0};
	for(int i=0; i<116; i++)
	{
		BERC b;
		b.clear();
		berc.push_back(b);
		test[i] = 0;
	}

	if(num_orig != num_rece)
		cout << "Something error on num_rece != num_orig after decode" << endl;
	else
	{
		fstream f;

		string path;
		char buf[80];
		float error = 0;
		float total = 0;
		getcwd(buf, sizeof(buf));
		path = buf;
		path = path + "/" + date + "/result";

		float temp = 0;

		f.open( path.c_str(), ios::app | ios::out);

		f << "---- after_decode_compare ----" << endl << endl;

		f << "Without discarding nonvalid block" << endl;
		for(int i=0; i<num_orig; i++)
		{
			for(int j=0; j<orig[i].getframe_matrix_k(); j++)
			{
				for(int l=0; l<116; l++)
					if(l%4 !=3)
						if(rece[i].getSymbolsSizeFromBlockOfFrame(l,j) >= 75)
							berc[l].count( (orig[i].getSymbolsFromBlockOfFrame(l,j)).get(0,74), 
									(rece[i].getSymbolsFromBlockOfFrame(l,j)).get(0,74) );
						else
							test[l]++;
			}
		}


		for(int i=0; i<116; i++)
		{
			f << setw(5) << i << " : " << setw(8) 
			<<( berc[i].get_errors() + test[i]*75 ) / ( berc[i].get_total_bits() + test[i]*75);
			error = error + berc[i].get_errors() + test[i]*75;
			total = total + berc[i].get_total_bits() + test[i]*75;

			if(i%5 == 4)
				f << endl;
		}

		f << endl << "total error rate: " << error/total << endl;
		f << "total bits: " << total << endl << endl;



		for(int i=0; i<116; i++)
			berc[i].clear();

		for(int i=0; i<num_orig; i++)
		{
			for(int j=0; j<orig[i].getframe_matrix_k(); j++)
			{
				for(int l=0; l<116; l++)
					if(l%4 !=3)
					{
						if( rece[i].getValidFromBlockOfFrame(l,j))
						{
							//cout << i << " "<< l <<" "<< j << " "<< rece[i].getSymbolsSizeFromBlockOfFrame(l,j)<<endl;
							berc[l].count( (orig[i].getSymbolsFromBlockOfFrame(l,j)).get(0,74), 
								(rece[i].getSymbolsFromBlockOfFrame(l,j)).get(0,74) );
						}
						else
							temp ++;
					}
			}
		}

		error = 0;
		total = 0;


		f << "Discarding the erasure block" << endl;

		for(int i=0; i<116; i++)
		{
			f << setw(5) << i << " : " << setw(8) << berc[i].get_errorrate();
			error = error + berc[i].get_errors();
			total = total + berc[i].get_total_bits();

			if(i%5 == 4)
				f << endl;
		}

		f << endl << "total error rate with erasure: " << (error + temp *75 )/(total + temp*75)<< endl;
		f << "total bits: " << (total + temp*75) << endl;


		f << "--- End of after_decode_compare ---" << endl << endl;
		f.close();
	}

	berc.clear();

	return ;
}


/*Set information*/

void Data::set_orig_Matrix(){
	for(int i=0; i<num_orig; i++)
		orig[i].setFBMatrix();
	return ;
}

void Data::set_rece_Matrix(){
	for(int i=0; i<num_orig; i++)
		rece[i].setFBMatrix();
	return;
}

void Data::set_all_Matrix(){
	set_orig_Matrix();
	set_rece_Matrix();
	return;
}

void Data::set_orig(int num)
{
	num_orig = num;
	for(int i=0; i<num; i++)
	{
		RD_packet p(i);
		p.setFBMatrix();
		p.setFrame();
		orig.push_back(p);
	}

	return ;
}

void Data::set_rece(int num)
{
	num_rece = num;
	for(int i=0; i<num; i++)
	{
		RD_packet p(i);
		p.setFBMatrix();
		p.setFrame();
		rece.push_back(p);
	}

	return ;
}

void Data::clear_orig()
{
	orig.clear();
	num_orig=0;

	return;
}

/*Read data*/
void Data::read_orig(string ti, int num){
	fstream f;
	string temp, csv, path, no, in;
	char buf[80];
	bvec input;

	num_orig = num;
	int no_packet, no_frame;

	set_orig(num);

	csv = ".csv";
	getcwd(buf, sizeof(buf) );
	temp = buf;
	int i = 0;


	temp = temp + "/" + ti + "/" + "original/";

	sprintf(buf,"%d", i);
	no = buf;
	path = temp + no + csv;

	f.open(path.c_str(), ios::in);
	while( f.is_open() )
	{
		
		no_packet = (i/22) * 2 + (i%2);
		no_frame = (i%22)/2;

		for(int j=0; j<116; j++)
		{
			input.clear();

			getline(f, in);
			input.set_size( (in.size()+1) /2 );


			for(int l=0; l< (in.size()+1) /2; l++)
				input(l) = ( ( (in[l*2]-'0') == 0 ) ? 0 : 1 );

			orig[no_packet].setSymbolsofBlockofFrame( j, no_frame, input);

		}

		f.close();
		i++;
		sprintf(buf,"%d", i);
		no = buf;
		path = temp + no + csv;
		f.open(path.c_str(), ios::in);
	}

	f.close();

	return ;
}

void Data::read_rece(string ti, int num){
	fstream f;
	string temp, csv, path, no, in;
	char buf[80];
	bvec input;

	num_rece = num;
	int no_packet, no_frame;

	set_rece(num);

	csv = ".csv";
	getcwd(buf, sizeof(buf) );
	temp = buf;
	int i = 0;


	temp = temp + "/" + ti + "/" + "recevied/";

	sprintf(buf,"%d", i);
	no = buf;
	path = temp + no + csv;

	f.open(path.c_str(), ios::in);
	while( f.is_open() )
	{
		// 22 is default, TODO : need to change
		no_packet = (i/22) * 2 + (i%2);
		no_frame = (i%22)/2;

		for(int j=0; j<116; j++)
		{
			input.clear();

			getline(f, in);
			input.set_size( in.size()/2 );

			for(int l=0; l<in.size()/2; l++)
				input(l) = ( ( (in[l*2]-'0') == 0 ) ? 0 : 1 );

			rece[no_packet].setSymbolsofBlockofFrame( j, no_frame, input);

		}


		f.close();
		i++;
		sprintf(buf,"%d", i);
		no = buf;
		path = temp + no + csv;
		f.open(path.c_str(), ios::in);
	}

	f.close();

	return ;
}


void Data::read_trace(string ti, int num)
{
	fstream f;
	string temp, txt, path, fid, in;
	char input_line[400], buf[80];
	bvec input(126), input1(126);
	int count, fid_int, i, flag;
	

	
	num_rece = num;
	int no, no1, f0, f1;

	set_rece(num);


	txt = "receiver_trace.txt";
	getcwd(buf, sizeof(buf) );
	temp = buf;

	path = temp + "/" + ti + "/" + txt;

	f.open(path.c_str(), ios::in);
	if(f.is_open())
	{
		i=0;
		f >> input_line >> count;
		while( i < count)
		{
			flag = 0;
			f >> input_line >> fid;
			fid_int = atoi(fid.c_str());

			//no_packet = (i/22) * 2 + (i%2);
			//no_frame = (i%22)/2;
			no = ( (fid_int*2)/22 ) *2 + ( fid_int*2 )%2;
			f0 = ( (fid_int*2) % 22 )/2;
			no1 = ( (fid_int*2+1)/22 ) *2 + ( fid_int*2+1 )%2;
			f1 = ( (fid_int*2+1) % 22 )/2;

			erasure[fid_int*2] = false;
			erasure[fid_int*2+1] = false;

			//cout << fid_int << ":" << fid_int*2 << "&" << fid_int*2+1 << endl;
			//cout << no << " " << f0 << endl;
			//cout << no1 << " " << f1 << endl;

			f.getline(input_line,400);
			for(int j=0; j<=116; j++)
			{	//jth block
				input.clear(), input1.clear();
				f.getline(input_line, 400);
				if(fid_int < 55)
				{
					if(j != 58)
					{
						for (int l=0; l<126; l++)
						{
							int index = 38 + l*2;

							if( (input_line[index]-'0') >= 4)
								input_line[index] = input_line[index] - 4;

							if( (input_line[index]-'0')/2 == 1)
								input(l) = itpp::bin(1);
							else
								input(l) = itpp::bin(0);

							if( (input_line[index]-'0')%2 == 1)
								input1(l) = itpp::bin(1);
							else
								input1(l) = itpp::bin(0);
						}

						//cout << rece[no].getSymbolsSizeFromBlockOfFrame(j-flag, f0) << endl;
						rece[no].setSymbolsofBlockofFrame( j-flag, f0, input);
						rece[no1].setSymbolsofBlockofFrame( j-flag, f1, input);
					}
					else
						flag = 1;
				}
			}
			i++;
		}

	}
	else
		cout << "open error!!" << endl;
	f.close();
	path =  temp + "/" + ti + "/result"; 
	f.open( path.c_str(), ios::app | ios::out);
	f << "Erasure frame:";
	for(int i=0; i<110; i++)
		if(erasure[i])
			f << " " << i;
	f << endl << endl;

	f.close();

	return ;
}
/*Output*/

void Data::out_orig(){
	fstream f;
	string temp, csv, path, no, ti;
	char buf[80];
	bvec out;

	csv = ".csv";
	getcwd(buf, sizeof(buf));
	temp = buf;
	//temp = temp + "/original/";

	time_t t = time(NULL);
	struct tm tm = *localtime(&t);
	sprintf(buf,"%02d%02d", tm.tm_mon+1, tm.tm_mday);
	ti = buf;
	temp = temp + "/" + ti +"/";
	mkdir( temp.c_str(), ACCESSPERMS);
	temp = temp + "original/";
	mkdir( temp.c_str(), ACCESSPERMS);



	for(int i=0; i<num_orig; i++)
	{	//"i"th packet
		for(int j=0; j<orig[i].getNumFrameofPacket(); j++)
		{	//"j"th frame
			sprintf(buf,"%d", (i%2)+j*2+(i/2)*22 );
			no = buf;
			path = temp + no + csv;

			f.open(path.c_str(), ios::out);

			for(int l=0; l<orig[i].getNumofBlocksFromFrame(j) ; l++)
			{	//"l"th block
				/*if(l%4!=3)
				{*/
					out = orig[i].getSymbolsFromBlockOfFrame(l,j);
					for(int q=0; q<out.size();q++)
					{
						f << out[q];
						if(q != out.size()-1)
							f << ",";
					}
					f << endl;
				/*}*/
			}
			
			f.close();
		}
	}
	return ;
}


void Data::out_encoded_orig(){
	fstream f;
	string temp, csv, path, no, ti;
	char buf[80];
	bvec out;

	csv = ".csv";
	getcwd(buf, sizeof(buf));
	temp = buf;
	//temp = temp + "/original/";

	time_t t = time(NULL);
	struct tm tm = *localtime(&t);
	sprintf(buf,"%02d%02d", tm.tm_mon+1, tm.tm_mday);
	ti = buf;
	temp = temp + "/" + ti +"/";
	mkdir( temp.c_str(), ACCESSPERMS);
	temp = temp + "encodedframes/";
	mkdir( temp.c_str(), ACCESSPERMS);



	for(int i=0; i<num_orig; i++)
	{	//"i"th packet
		for(int j=0; j<orig[i].getNumFrameofPacket(); j++)
		{	//"j"th frame
			sprintf(buf,"%d", (i%2)+j*2+(i/2)*22 );
			no = buf;
			path = temp + no + csv;

			f.open(path.c_str(), ios::out);

			for(int l=0; l<orig[i].getNumofBlocksFromFrame(j) ; l++)
			{	//"l"th block
				out = orig[i].getSymbolsFromBlockOfFrame(l,j);
				for(int q=0; q<out.size();q++)
				{
					f << out[q] << ",";
				}

				for(int q=out.size(); q<128; q++)
					f << "0,";
			}
			
			f.close();
		}
	}
	return ;
}


void Data::out_rece(string ti){
	fstream f;
	string temp, csv, path, no;
	char buf[80];
	bvec out;

	csv = ".csv";
	getcwd(buf, sizeof(buf));
	temp = buf;


	//cout << "plz enter the date :";
	//cin >> ti;
	
	temp = temp + "/" + ti +"/";
	temp = temp + "recevied/";
	mkdir( temp.c_str(), ACCESSPERMS);



	for(int i=0; i<num_rece; i++)
	{	//"i"th packet
		for(int j=0; j<rece[i].getNumFrameofPacket(); j++)
		{	//"j"th frame
			sprintf(buf,"%d", (i%2)+j*2+(i/2)*22 );
			no = buf;
			path = temp + no + csv;

			f.open(path.c_str(), ios::out);

			for(int l=0; l<rece[i].getNumofBlocksFromFrame(j) ; l++)
			{	//"l"th block
				/*if(l%4!=3)
				{*/
					out = rece[i].getSymbolsFromBlockOfFrame(l,j);
					for(int q=0; q<out.size();q++)
					{
						f << out[q];
						if(q != out.size()-1)
							f << ",";
					}
					f << endl;
				/*}*/
			}
			
			f.close();
		}
	}
	return ;
}

void Data::out_to_decoded_rece(string ti){
	fstream f;
	string temp, csv, path, no;
	char buf[80];
	bvec out;

	csv = ".csv";
	getcwd(buf, sizeof(buf));
	temp = buf;

	

	temp = temp + "/" + ti +"/";
	temp = temp + "to_decoded/";
	mkdir( temp.c_str(), ACCESSPERMS);



	for(int i=0; i<num_rece; i++)
	{	//"i"th packet
		for(int j=0; j<rece[i].getNumFrameofPacket(); j++)
		{	//"j"th frame
			sprintf(buf,"%d", (i%2)+j*2+(i/2)*22 );
			no = buf;
			path = temp + no + csv;

			f.open(path.c_str(), ios::out);

			for(int l=0; l<rece[i].getNumofBlocksFromFrame(j) ; l++)
			{	//"l"th block
				out = rece[i].getSymbolsFromBlockOfFrame(l,j);
				for(int q=0; q<out.size();q++)
				{
					f << out[q] << ",";
				}

				for(int q=out.size(); q<128; q++)
					f << "0,";
			}
			
			f.close();
		}
	}
	return ;
}



/*Encode & Decode*/

void Data::orig_Encode(){

	for(int i=0; i<num_orig; i++)
		orig[i].RDEncode();

	return ;
}
void Data::rece_Encode(){
	for(int i=0; i<num_rece; i++)
		rece[i].RDEncode();
	return ;
}
void Data::orig_Decode(int round){
	for(int i=0; i<num_orig; i++)
		orig[i].RDDecode(round);
	return ;
}
void Data::rece_Decode(int round){
	for(int i=0; i<num_rece; i++)
		rece[i].RDDecode(round);
	return ;
}

void Data::rand_orig_data(int p, int symbolsize)
{
	set_orig(p);
	bvec bits;

	for(int i=0;  i<p; i++)
		for(int l=0; l<8; l++)
			for(int j=0; j<116; j++)
			{
				if(j%4 != 3)
				{
					bits = randb(symbolsize);
					orig[i].setSymbolsofBlockofFrame( j, l,bits);
				}

			}


	return ;
}

void Data::rece_BFDecode(int round)
{
	for(int i=0; i<num_rece; i++)
		rece[i].BFDecode(round);
}

void Data::rece_block_decode()
{
	for(int i=0; i<num_rece; i++)
		if( !erasure[i] )
		{
			rece[i].Block_level_decode();
		}
	return ;
}

void Data::block_erasure(string date, bool flag)
{
	float rate[116] = {0};
	float count = 0;
	float result = 0;
	float temp = 0;

	for(int i=0; i<num_rece; i++)
	{	//ith packet
		for(int j=0; j<rece[i].getNumFrameofPacket(); j++)
		{	//jth frame of packet
			for(int l=0; l<116; l++)
			{	//lth block of frame
				count ++;
				if( !rece[i].getValidFromBlockOfFrame(l, j) )
					rate[l]++;
			}
		}
	}

	for(int i=0; i<116; i++)
		result = result + rate[i];
	


	fstream f;
	string path;
	char buf[80];
	getcwd(buf, sizeof(buf));
	path = buf;
	path = path + "/" + date + "/result";
	f.open( path.c_str(), ios::app | ios::out);

	if(flag)
	{	
		for(int i=0; i<116; i++)
			if(erasure[i])
				temp++;

		f << "----- Only block decoding--------------" << endl;
		f << "block erasure rate: " << (result-temp*116)/(count-temp*116) << "." << endl;
		f << "block erasure rate with BID :"<< endl;

		for(int i=0; i<116; i++)
		{
			f << setw(4) << i << " : " << setw(10) << (rate[i]-temp)/( (num_rece*rece[0].getNumFrameofPacket() )-temp) << " ";
			if(i%5 == 4)
				f << endl;
		}
		f << endl<<"--- end of only block decoding---" << endl;
		f << endl;
	}


	f << endl;
	f << "--- block_erasure ---" << endl;
	f << "total block:" << count << endl;
	f << "number of erasure block" << result << endl;
	f << "block erasure rate: " << result/count << "." << endl;
	f << "block erasure rate with BID :"<< endl;

	for(int i=0; i<116; i++)
	{
		f << setw(4) << i << " : " << setw(9) << rate[i]/(num_rece*rece[0].getNumFrameofPacket() ) << " ";
		if(i%5 == 4)
			f << endl;
	}
	f << endl;
	f << "--- end of block_erasure ---" << endl << endl;

	f.close();

	return;
}

void Data::total_equal(string date)
{
	int total = 0;
	for(int i=0; i<num_rece; i++)
		for(int j=0; j<rece[i].getNumFrameofPacket(); j++)
		{
			for(int l=0; l<116; l++)
			{
				if( rece[i].getValidFromBlockOfFrame(l,j) )
					if( ( rece[i].getSymbolsFromBlockOfFrame(l,j) ).get(0,74) == 
						( orig[i].getSymbolsFromBlockOfFrame(l,j) ).get(0,74) )
						total++;
			}
		}

	fstream f;
	string path;
	char buf[80];
	getcwd(buf, sizeof(buf));
	path = buf;
	path = path + "/" + date + "/result";
	f.open( path.c_str(), ios::app | ios::out);
	f << endl<< "# of total equal block : " << total << endl;
	f.close();


	return;
}

void Data::decode_and_compare(string date, int num)
{
	read_trace(date,num);
	out_to_decoded_rece(date);
	clear_orig();
	read_orig(date, num);
	orig_Encode();

	before_decode_compare(date);


	rece_block_decode();
	block_erasure(date, true);


	rece_BFDecode(5);
	block_erasure(date, false);
	rece_BFDecode(5);
	block_erasure(date, false);
	rece_BFDecode(5);
	block_erasure(date, false);

	out_rece(date);
	orig_Decode(10);

	after_decode_compare(date);
	out_rece(date);
	total_equal(date);

	return ;
}



Data::Data(){
	initData();
}

Data::~Data(){
}