#include <iostream>
#include <cstring>
#include <fstream>
#include <cstdio>
#include <stdlib.h>   
using namespace std;
class rec_trace
{
	private:
		int exist=0;
		char rec_data[117][137];
		int error=0;
	public:
		void give_rec_trace(char a,int i,int j)
		{
			rec_data[i][j] = a;
		}
		char get_rec_trace(int i,int j)
		{
			return rec_data[i][j];
		}
		void init()
		{
		  exist=1;	
		  for(int i=0;i<117;i++)
		  {
		  	for(int j=0;j<137;j++)
		  	{
		  		rec_data[i][j]=-1;	
			}
		  }
		}
		int print()
		{
			return exist;
		}
		void give_error()
		{
			error++;
		}
		int get_error()
		{
			return error;
		}
};
int main()
{
	char line[1000]={'\0'};
	rec_trace re[55];
	fstream fin;
    fin.open("7receiver_trace.txt",ios::in);
    char filename[]="result7.txt";
    fstream fresult;
    fresult.open(filename, ios::out);//¶}±ÒÀÉ®×
    int count=0;
    int fidcheck=0;
    while(fin.getline(line,sizeof(line))!=NULL)
    {
    	int fid;//FID
    	char* pch;
    	if(line[0]=='F')
    	{
    		fidcheck=0;
    		count = 0;
    		pch = strtok(line," ");
    		pch = strtok (NULL," ");
    		fid = atoi(pch);
    		if(fid>54){
    			fidcheck=1;
				continue;	
			}
    		re[fid].init();
		}
		else if(line[0]=='r' && fidcheck==0)
		{
			int k=22;
			for(int i=0;i<137;i++){
				if(line[k]>='4')
				re[fid].give_rec_trace(line[k]-4,count,i);	
				else
				re[fid].give_rec_trace(line[k],count,i);
				k+=2;
				//cout<<re[fid].get_rec_trace(count,i)<<endl;
			}
			count++;
		}

	}
    /*for(int i=0;i<55;i++)
    {
    	if(re[i].print())
    	{
    		cout<<"FID "<<i<<endl;
    		for(int a=0;a<117;a++)
    		{   
    		    cout<<"raw data "<<a<<" ";
    			for(int b=0;b<137;b++)
    			{
    				cout<<re[i].get_rec_trace(a,b)<<" ";
				}   cout<<endl;
			}
		}
	}*/
	
	fin.close();

    char* s1="(";
    char* s2=").txt";
    for(int fid2=0;fid2<55;fid2++)
    {
    	if(!re[fid2].print()) continue;
    	
    	char* s=new char;
    	sprintf(s,"%d",fid2);
    	char* st=new char[30];
    	strcpy(st,"sender_trace");
    	strcat(st,s);
    	strcat(st,s1);
    	strcat(st,s);
    	strcat(st,s2);
    	fstream fout;
        fout.open(st,ios::in);
        //
        int send_count=1;
      
        fout.getline(line,sizeof(line));
        while(fout.getline(line,sizeof(line))!=NULL){
    	if(send_count % 2 == 0 && send_count/2-1 != 58)
    	{
    		int space_count =0;
    		int send_k = 0;
    		int index=0;  
    		for(int i=0;i<288;i++)
    		{   			
        		if(line[i]==' ')
				{
					space_count++;
					continue;	
				}
				if(space_count >= 7)
				{   
                    //if(index==136)
                    //cout<<line[i]<<" ";

				    if(line[i]!=re[fid2].get_rec_trace(send_count/2-1,index))re[fid2].give_error();
					index++;
				}			
			}
		}
		send_count++;	
	}
    fout.close();
	}
	double rate=0;
	int base=0;
	int errors=0;
	for(int i=0;i<55;i++)
	{
		if(!re[i].print())
		{
		 cout<<"Lost frame ID: "<<i<<endl;
		 char buffer[30];
		 sprintf(buffer,"Lost frame ID: %d",i); 
		 fresult<<buffer<<endl;//¼g¤J¦r¦ê
		continue;
	    }
		base+=116*136;
		cout<<"Frame id: "<<i<<" "<<re[i].get_error()<<" "<<(double)re[i].get_error()/(double)(116*136)*(double)100<<"%"<<endl;
		errors+=re[i].get_error();
		char buffer[100];
		sprintf(buffer,"Frame id: %d %d %f",i,re[i].get_error(),(double)re[i].get_error()/(double)(116*136)*(double)100);
		fresult<<buffer<<"%"<<endl;//¼g¤J¦r¦ê	
	}
	cout<<"Error_rate: "<<(double)errors/(double)base*(double)100<<"%"<<endl;
	char buffer[100];
	sprintf(buffer,"Error_rate: %f",(double)errors/(double)base*(double)100);
	fresult<<buffer<<"%"<<endl;//¼g¤J¦r¦ê
	fresult.close();//Ãö³¬ÀÉ®×
/* 	
	int base;
	base = 116*136;
	double answer=0;
	answer = (double)compare_count/(double)base;
	cout<<answer;*/
 	/*	for(int j = 0;j<117;j++)
 		{
 			if(j==57)continue;
 			 for(int i=0;i<136;i++){
				cout<<send_data[j][i]<<" ";
			
			} 
			cout<<endl;
		 }
	*/	
    return 0;
 } 
