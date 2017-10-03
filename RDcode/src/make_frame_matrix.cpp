#include <iostream>
#include <fstream>
#include <string>
#include <unistd.h>

#define frame_per_packet 11
#define original_frame 8
#define parity_frame 3
using namespace std;

int main(int argc, char** argv)
{
	fstream f;

	f.open(argv[1],ios::out);
	cout << f.is_open() << endl;

	f << original_frame << " " << parity_frame << endl;
	for(int i=0; i<=7; i++)
		f << "1" << " ";

	f << "3 3 2" << endl;

	f << "8" << endl << "9" << endl << "10" << endl;
	f << "8" << endl << "9" << endl << "10" << endl;
	f << "8" << endl << "9" << endl;
	f << "0 3 6" << endl;
	f << "1 4 7" << endl;
	f << "2 5" << endl;

	f.close();
}