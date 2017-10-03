#include <iostream>
#include <fstream>
#include <string>
#include <unistd.h>

#define block_per_frame 116
#define original_block 87
#define parity_block 29
using namespace std;

int main(int argc, char** argv)
{
	fstream f;

	f.open(argv[1],ios::out);
	cout << f.is_open() << endl;

	f << original_block << " " << parity_block << endl;
	for(int i=0; i<block_per_frame; i++)
	{
		if(i%4==3)
			f << "3";
		else
			f << "1";

		if(i<block_per_frame)
			f << " ";
	}
	f << endl;
	
	for(int i=0; i<block_per_frame; i++)
	{
		if(i%4 != 3)
			f << 3+(i/4)*4 << endl;
		else
		{
			f << i-3 << " " << i-2 << " " << i-1 << endl;
		}
	}

	f.close();
}