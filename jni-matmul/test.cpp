#include<iostream>
#include<algorithm>

using namespace std;

int computeNNZ(
    double* aValues,
    int* aColPtrs,
    int* aRowIndices,
    double* bValues,
    int* bColPtrs,
    int* bRowIndices,
    int* workIndex,
    int m,
    int k,
    int n)
{
    int nnz = 0;
    int i, j, bStart, bEnd, bIndx, bRow, aStart, aEnd, aIndx, aRow;
    for (j = 0; j < n; j++)
    {
        bStart = bColPtrs[j];
        bEnd = bColPtrs[j + 1];
        for (bIndx = bStart; bIndx < bEnd; bIndx++)
        {
            bRow = bRowIndices[bIndx];
            aStart = aColPtrs[bRow];
            aEnd = aColPtrs[bRow + 1];
            for (aIndx = aStart; aIndx < aEnd; aIndx++)
            {
                aRow = aRowIndices[aIndx];
                if (workIndex[aRow] < j)
                {
                    workIndex[aRow] = j;
                    nnz += 1;
                }
            }
        }
    }

    return nnz;
}

void ssdgemm()
{
    // 1 0 0
    // 0 1 0
    // 0 0 1

    double aValues[3] = {1, 1, 1};
    int aColPtrs[4] = {0, 1, 2, 3};
    int aRowIndices[3] = {0, 1, 2};
    int m = 3;
    int k = 3;

    // 1 0 0 0
    // 0 0 2 0
    // 1 0 0 1
    double bValues[4] = {1, 1, 2, 1};
    int bColPtrs[5] = {0, 2, 2, 3, 4};
    int bRowIndices[4] = {0, 2, 1, 2};
    int n = 4;

    double *workData = (double *)malloc(sizeof(double) * m);
    int *workIndex = (int *)malloc(sizeof(int) * m);

    for (int i = 0; i < m; i++)
    {
        workIndex[i] = -1;
    }

    int totalNNZ = computeNNZ(
        aValues, aColPtrs, aRowIndices,
        bValues, bColPtrs, bRowIndices,
        workIndex,
        m, k, n);

    for (int i = 0; i < m; i++)
    {
        workIndex[i] = -1;
    }

    int *resRows = (int *)malloc(sizeof(int) * totalNNZ);
    double *resData = (double *)malloc(sizeof(double) * totalNNZ);
    int *resColPtrs = (int *)malloc(sizeof(int) * (n+1));
   resColPtrs[0] = 0;

    int col, nnzUsed, nnz, bOff, bRow, bVal, aOff, aRow, aVal, resOff;
    nnzUsed = 0;
    for (col=0; col<n; col++) {
        nnz = nnzUsed;
        for (bOff=bColPtrs[col]; bOff<bColPtrs[col+1]; bOff++) {
            bRow = bRowIndices[bOff];
            bVal = bValues[bOff];
            for (aOff=aColPtrs[bRow]; aOff<aColPtrs[bRow+1]; aOff++) {
                aRow = aRowIndices[aOff];
                aVal = aValues[aOff];

                if (workIndex[aRow] < col) {
                    workData[aRow] = 0;
                    workIndex[aRow] = col;
                    resRows[nnz] = aRow;
                    nnz += 1;
                }

                workData[aRow] += aVal * bVal;
            }
        }

        resColPtrs[col + 1] = nnz;
        nnzUsed = nnz;

        std::sort(resRows+resColPtrs[col], resRows+resColPtrs[col+1]);

        for (resOff=resColPtrs[col]; resOff<resColPtrs[col+1]; resOff++) {
            int row = resRows[resOff];
            resData[resOff] = workData[row];
        }

        // assert(nnz <= totalNNZ)
    }

    std::cout<<"=====totalNNZ====="<<std::endl;
    std::cout<<totalNNZ<<std::endl;

    std::cout<<"=====nnz====="<<std::endl;
    cout<<nnz<<endl;
    
    std::cout<<"=====nnzUsed====="<<std::endl;
    cout<<nnzUsed<<endl;

    cout<<"=====colPtrs====="<<endl;
    for (int i=0; i < n+1; i++) {
        cout<<resColPtrs[i]<<" ";
    }
    cout<<endl;

    cout<<"=====values====="<<endl;
    for (int i = 0; i < totalNNZ; i++) {
        cout<<resData[i]<<" ";
    }
    cout<<endl;

    cout<<"=====rowIndices"<<endl;
    for (int i=0; i < nnz; i++) {
        cout<<resRows[i]<<" ";
    }
    cout<<endl;
}

int main() {
    ssdgemm();
    return 0;
}