#include "../header/matrix.h"
#include <cstdio>
#include <iostream>
#include <fstream>
#include <string>
#include <algorithm>

using namespace std;

/*****************
 * Public Member *
 *****************/

int RD_Matrix::readMatrixFromFile(string name)
{
	
	//cout << "Reading the matrix...." << endl;
	
	int temp;
	fstream f;
	f.open(name.c_str(), ios::in);
	/** Read k **/
	f >> k >> m;
	for(int i=0; i<k+m; i++)
	{
		f >> temp;
		degree.push_back(temp);
	}

	for(int i=0; i<k+m; i++)
	{
		for(int j=0; j<degree[i]; j++)
		{
			f >> temp;
			v_node[i].push_back(temp);
		}
	}

	for(int i=0; i<k+m; i++)
	{
		for(int j=0; j<degree[i]; j++)
		{
			v_neighbor[i].push_back( v_node[i][j] );
			temp = v_node[i][j];
			for(int l=0; l<degree[ temp ]; l++)
			{
				if( v_node[ v_node[i][j] ][l] != i)
					v_neighbor[i].push_back( v_node[ v_node[i][j] ][l] );
			}
		}

		sort(v_neighbor[i].begin(), v_neighbor[i].end() );
	}

	f.close();
	/*cout << "End of reading the matrix" << endl;
	cout << "-------------------------------" << endl; */
	return 0;

}

int RD_Matrix::clear(){
	k = 0;
	m = 0;
	degree.clear();
	for(int i=0; i<116; i++)
	{
		v_node[i].clear();
		v_neighbor[i].clear();
	}

	return 0;
}


void RD_Matrix::showMatrix()
{
	printf("k:%d  m:%d\n", k, m);
	for(int i=0; i<k+m; i++)
		cout << degree[i] << " ";
	cout << endl;

	for(int i=0; i<k+m; i++)
	{
		for(int j=0; j<degree[i]; j++)
			cout << v_node[i][j] <<" ";
		cout << endl;
	}

	for(int i=0; i<k+m; i++)
	{
		cout << i << ": ";
		for(int j=0; j<v_neighbor[i].size(); j++)
		{
			cout << v_neighbor[i][j] << " ";
		}
		cout << endl;
	}

	return ;
}

int RD_Matrix::getDegree(int no){
	return degree[no];
}

int RD_Matrix::getInfoVnode(int num, int temp){
	return v_node[num][temp];
}

RD_Matrix::RD_Matrix(){
}

RD_Matrix::~RD_Matrix(){
}