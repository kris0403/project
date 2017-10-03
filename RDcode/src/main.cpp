#include "../header/rdblock.h"
#include "../header/rdframe.h"
#include "../header/rdpacket.h"
#include "../header/data.h"
#include "../header/matrix.h"
#include <cstdio>
#include <cstdlib>
#include <itpp/itcomm.h>
#include <fstream>
#include <unistd.h>
#include <string>
#include <time.h>
#include <sys/stat.h>
#include <sys/types.h>

using namespace itpp;
using namespace std;

int main()
{	
	int option;
	option = 999;
	do
	{
		cout << "1 for generate data." << endl;
		cout << "2 for decode." << endl;
		cout << "0 for end the programe" << endl;
		cout << "enter what do u want to do:";
		cin >> option;
		cout << endl;
		switch(option)
		{
			case 1:
			{
				Data data;
				data.rand_orig_data(10,75);
				data.out_orig();
				data.orig_Encode();
				data.out_encoded_orig();
			}
				break;
			case 2:
			{
				Data decode;
				string date;
				cout << "enter the date which to compare (MMDD):";
				cin >> date;
				
				decode.decode_and_compare(date,10);
			}
				break;
			case 0:
				cout << "goodbye" << endl;
				break;
			default:
				break;
		}
		cout << endl;

	}while(option!=0);
	
	return 0;
}


/*Data test;
	
	test.rand_orig_data(10,75);
	test.out_orig();
	test.orig_Encode();
	test.out_encoded_orig();*/