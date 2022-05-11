/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class Inference */

#ifndef _Included_Inference
#define _Included_Inference
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     Inference
 * Method:    dddgemm
 * Signature: ([I[D[D)V
 */
JNIEXPORT void JNICALL Java_Inference_dddgemm
  (JNIEnv *, jobject, jintArray, jdoubleArray, jdoubleArray);

/*
 * Class:     Inference
 * Method:    dsdgemm
 * Signature: (III[D[D[I[I[D)V
 */
JNIEXPORT void JNICALL Java_Inference_dsdgemm
  (JNIEnv *, jobject, jint, jint, jint, jdoubleArray, jdoubleArray, jintArray, jintArray, jdoubleArray);

#ifdef __cplusplus
}
#endif
#endif