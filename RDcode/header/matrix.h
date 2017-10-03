#ifndef __MATRIX_H__
#define __MATRIX_H__

#include <vector>
#include <string>

class RD_Matrix{
private:

public:
	int k;
	int m;
	std::vector<int> degree;
	std::vector<int> v_node[116];
	std::vector<int> v_neighbor[116];

	int readMatrixFromFile(std::string);
	int clear();

	void showMatrix();

	int getDegree(int);
	int getInfoVnode(int, int);

	RD_Matrix();
	~RD_Matrix();

};
#endif