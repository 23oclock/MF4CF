#include<iostream>

int main() {
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

    a = (double *)malloc(sizeof(double)*m*k);
    b = (double *)malloc(sizeof(double)*k*n);
    c = (double *)malloc(sizeof(double)*m*n);

    a[0] = 1;
    a[1] = 1;

    b[0] = 1;
    b[1] = 1;

    c[0] = 0; c[1] = 0; c[2] = 0; c[3] = 0;



    for (int i = 0; i < m * n; i++) {
        std::cout<<c[i]<<std::endl;
    }
}