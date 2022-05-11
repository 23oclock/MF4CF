#include <iostream>

#include "Inference.h"

extern "C" void dgemm_(
    char *transa,
    char *transb,
    int *m,
    int *n,
    int *k,
    double *alpha,
    double *a,
    int *lda,
    double *b,
    int *ldb,
    double *beta,
    double *c,
    int *ldc);

using namespace std;

JNIEXPORT void JNICALL Java_Inference_dddgemm(JNIEnv *env, jobject thisObj, jintArray dims, jdoubleArray ab, jdoubleArray c)
{
    char TRANSA = 'N';
    char TRANSB = 'N';
    double ALPHA = 1;
    double BETA = 0;

    int *DIMS = env->GetIntArrayElements(dims, 0);
    int M = DIMS[0];
    int N = DIMS[1];
    int K = DIMS[2];
    env->ReleaseIntArrayElements(dims, DIMS, 0);

    int LDA = M;
    int LDB = K;
    int LDC = M;

    double *AB = env->GetDoubleArrayElements(ab, 0);
    double *A = AB;
    double *B = AB + M * K;
    double *C = env->GetDoubleArrayElements(c, 0);
    dgemm_(&TRANSA, &TRANSB, &M, &N, &K, &ALPHA, A, &LDA, B, &LDB, &BETA, C, &LDC);

    cout << "===== A =====" << endl;
    for (int i = 0; i < M; i++)
    {
        for (int j = 0; j < K; j++)
        {
            cout << A[i + j * M] << " ";
        }
        cout << endl;
    }

    cout << "===== B =====" << endl;
    for (int i = 0; i < K; i++)
    {
        for (int j = 0; j < N; j++)
        {
            cout << B[i + j * K] << " ";
        }
        cout << endl;
    }

    cout << "===== C =====" << endl;
    for (int i = 0; i < M; i++)
    {
        for (int j = 0; j < N; j++)
        {
            cout << C[i + j * M] << " ";
        }
        cout << endl;
    }

    env->ReleaseDoubleArrayElements(ab, AB, 0);
    env->ReleaseDoubleArrayElements(c, C, 0);
}

JNIEXPORT void JNICALL Java_Inference_dsdgemm(
    JNIEnv *env, jobject thisObj,
    jint m, jint n, jint k,
    jdoubleArray a,
    jdoubleArray values, jintArray colPtrs, jintArray rowIndices,
    jdoubleArray c)
{
    int M = (int)m;
    int N = (int)n;
    int K = (int)k;

    double *A = env->GetDoubleArrayElements(a, 0);
    double *VALUES = env->GetDoubleArrayElements(values, 0);
    int *COLPTRS = env->GetIntArrayElements(colPtrs, 0);
    int *ROWINDICES = env->GetIntArrayElements(rowIndices, 0);
    double *C = env->GetDoubleArrayElements(c, 0);

    int i, j, start, end, indx, rowIndx;
    double v, temp;
    for (i = 0; i < M; i++)
    {
        for (j = 0; j < N; j++)
        {
            temp = 0;
            start = COLPTRS[j];
            end = COLPTRS[j + 1];
            for (indx = start; indx < end; indx++)
            {
                rowIndx = ROWINDICES[indx];
                v = VALUES[indx];
                temp += A[i + rowIndx * M] * v;
            }
            C[i + j * M] = temp;
        }
    }
    env->ReleaseDoubleArrayElements(a, A, 0);
    env->ReleaseDoubleArrayElements(values, VALUES, 0);
    env->ReleaseIntArrayElements(colPtrs, COLPTRS, 0);
    env->ReleaseIntArrayElements(rowIndices, ROWINDICES, 0);
    env->ReleaseDoubleArrayElements(c, C, 0);
}

int main()
{
    char transa = 'n';
    char transb = 'n';

    double *a, *b, *c;
    double alpha, beta;
    int m, n, k, lda, ldb, ldc;

    m = 2;
    n = 2;
    k = 1;
    lda = m;
    ldb = k;
    ldc = m;
    alpha = 1;
    beta = 0;

    a = (double *)malloc(sizeof(double) * m * k);
    b = (double *)malloc(sizeof(double) * k * n);
    c = (double *)malloc(sizeof(double) * m * n);

    a[0] = 1;
    a[1] = 1;

    b[0] = 1;
    b[1] = 1;

    c[0] = 0;
    c[1] = 0;
    c[2] = 0;
    c[3] = 0;

    dgemm_(&transa, &transb, &m, &n, &k, &alpha, a, &lda, b, &ldb, &beta, c, &ldc);

    for (int i = 0; i < m * n; i++)
    {
        std::cout << c[i] << std::endl;
    }
}


void ssdgemm()
{
    // 1 0 0
    // 0 1 0
    // 0 0 1
    double[] aValues = {1, 1, 1};
    int[] aColPtrs = {0, 1, 2, 3};
    int[] aRowIndices = {0, 1, 2};
    int m = 3;
    int k = 3;

    // 1 0 0 0
    // 0 0 2 0
    // 1 0 0 1
    double[] bValues = {1, 1, 2, 1};
    int[] bColPtrs = {0, 2, 2, 3, 4};
    int[] bRowIndices = {0, 2, 1, 2};
    int n = 4;

    double *workData = (double *)malloc(sizeof(double)*m);
    double *workIndex = (int *)malloc(sizeof(int)*m);

    for (int i=0; i<m; i++) {
        workIndex[i] = -1;
    }

    int toltalNNZ = computeNNZ(aValues, aColPtrs, aRowIndices,
        bValues, bColPtrs, bRowIndices, m, k, n);


}

int computeNNZ(
    double[] aValues,
    int[] aColPtrs,
    int[] aRowIndices,
    double[] bValues,
    int[] bColPtrs,
    int[] bRowIndices,
    int m,
    int k,
    int n)
{
    int nnz = 0;
    int i, j, bStart, bEnd, bIndx, bRow, aStart, aEnd, aIndx, aRow;
    for(j=0; j<n; j++) {
        bStart = bColPtrs[j];
        bEnd = bColPtrs[j+1];
        for (bIndx=bStart; bIndx<bEnd; bIndx++) {
            bRow = bRowIndices[bIndx];
            aStart = aColPtrs[bRow];
            aEnd = aColPtrs[bRow+1];
            for (aIndx=aStart; aIndx<aEnd; aIndx++) {
                aRow = aRowIndices[aIndx];
                if (workIndex[aRow] < j) {
                    workIndex[aRow] = j;
                    nnz += 1;
                }
            }
            
        }
    }

    return nnz;
}
