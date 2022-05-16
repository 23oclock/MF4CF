#include "Inference.h"

/*
 * Class:     Inference
 * Method:    dsdgemm
 * Signature: ([I[D[D)V
 */
JNIEXPORT void JNICALL Java_Inference_dsdgemm(
    JNIEnv *env, jobject thisObj, jintArray dimsAndIndices_, jdoubleArray ab_, jdoubleArray c_)
{
    int *dimsAndIndices = env->GetIntArrayElements(dimsAndIndices_, NULL);
    double *ab = env->GetDoubleArrayElements(ab_, NULL);
    double *c = env->GetDoubleArrayElements(c_, NULL);

    int m = dimsAndIndices[0];
    int n = dimsAndIndices[1];
    int k = dimsAndIndices[2];

    int *bColPtrs = dimsAndIndices + 3;
    int *bRowIndices = bColPtrs + n + 1;

    double *a = ab;
    double *bData = a + m * k;

    int j, start, end, indx, l, i;
    double bVal;
    for (j=0; j<n; j++) {
        start = bColPtrs[j];
        end = bColPtrs[j+1];
        for (indx=start; indx<end; indx++) {
            l = bRowIndices[indx];
            bVal = bData[indx];
            for (i=0; i<m; i++) {
                c[i + j * m] += a[i + l * m] * bVal;
            }
        }
    }

    env->ReleaseIntArrayElements(dimsAndIndices_, dimsAndIndices, JNI_ABORT);
    env->ReleaseDoubleArrayElements(ab_, ab, JNI_ABORT);
    env->ReleaseDoubleArrayElements(c_, c, 0);

}
