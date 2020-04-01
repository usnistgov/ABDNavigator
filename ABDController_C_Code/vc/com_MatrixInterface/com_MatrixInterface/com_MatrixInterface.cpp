#include "jni.h"
#include "stdio.h"
#include "stdlib.h"
#include "string.h"
#include "com_MatrixInterface.h"

#include "RemoteAccess_API.h"

static JavaVM *jvm;
static jobject jniObj;

static char* trigger1Method;
void trigger1(int, Value[]);

static char* trigger2Method;
void trigger2(int, Value[]);

static char* trigger3Method;
void trigger3(int, Value[]);

static char* trigger4Method;
void trigger4(int, Value[]);

static char* array1Method;
void array1(int, Value[]);


JNIEXPORT void JNICALL Java_com_MatrixInterface_test(JNIEnv *env, jobject thisObj)
{
   printf("Hello World!\n");
   return;
}

JNIEXPORT jlong JNICALL Java_com_MatrixInterface_init(JNIEnv *env, jobject thisObj, jstring rootJ)
{
	const char *rootString = env->GetStringUTFChars(rootJ, 0);
	char *root = strdup(rootString);

	unsigned int rc;
	rc = init(root);
	free(root);

	env->ReleaseStringUTFChars(rootJ, rootString);

	jniObj = env->NewGlobalRef(thisObj);
	jint rs = env->GetJavaVM(&jvm);

	return (jlong)(unsigned long long)rc;
}

JNIEXPORT void JNICALL Java_com_MatrixInterface_rundown(JNIEnv *env, jobject thisObj)
{
	rundown();
	return;
}

JNIEXPORT jstring JNICALL Java_com_MatrixInterface_getStringProperty(JNIEnv *env, jobject thisObj, jstring sJ, jint idx)
{
	const char *sString = env->GetStringUTFChars(sJ, 0);
	char *s = strdup(sString);
	char *r = strdup("               ");
	//double d;
	//getDoubleProperty(s, idx, &d);
	getStringProperty(s, idx, 15, r);

	env->ReleaseStringUTFChars(sJ, sString);

	jstring retString = env->NewStringUTF(r);

	free(s);
	free(r);

	return retString;
}

JNIEXPORT jlong JNICALL Java_com_MatrixInterface_setStringProperty(JNIEnv *env, jobject thisObj, jstring sJ, jint idx, jstring sJ2)
{
	const char *sString = env->GetStringUTFChars(sJ, 0);
	char *s = strdup(sString);

	const char *sString2 = env->GetStringUTFChars(sJ2, 0);
	char *p = strdup(sString2);

	unsigned int rc;
	rc = setStringProperty(s, idx, p);
	free(p);
	free(s);

	env->ReleaseStringUTFChars(sJ2, sString2);
	env->ReleaseStringUTFChars(sJ, sString);

	return (jlong)(unsigned long long)rc;
}

JNIEXPORT jdouble JNICALL Java_com_MatrixInterface_getDoubleProperty(JNIEnv *env, jobject thisObj, jstring sJ, jint idx)
{
	const char *sString = env->GetStringUTFChars(sJ, 0);
	char *s = strdup(sString);

	double d;
	getDoubleProperty(s, idx, &d);
	free(s);

	env->ReleaseStringUTFChars(sJ,sString);

	return d;
}

JNIEXPORT jint JNICALL Java_com_MatrixInterface_getIntegerProperty(JNIEnv *env, jobject thisObj, jstring sJ, jint idx)
{
	const char *sString = env->GetStringUTFChars(sJ, 0);
	char *s = strdup(sString);
	
	int i;
	getIntegerProperty(s, idx, &i);
	free(s);

	env->ReleaseStringUTFChars(sJ, sString);

	return i;
}

JNIEXPORT jint JNICALL Java_com_MatrixInterface_getEnumProperty(JNIEnv *env, jobject thisObj, jstring sJ, jint idx)
{
	const char *sString = env->GetStringUTFChars(sJ, 0);
	char *s = strdup(sString);

	int i;
	getEnumProperty(s, idx, &i);
	free(s);

	env->ReleaseStringUTFChars(sJ, sString);

	return i;
}

JNIEXPORT jboolean JNICALL Java_com_MatrixInterface_getBooleanProperty(JNIEnv *env, jobject thisObj, jstring sJ, jint idx)
{
	const char *sString = env->GetStringUTFChars(sJ, 0);
	char *s = strdup(sString);

	char b;
	getBooleanProperty(s, idx, &b);
	free(s);

	env->ReleaseStringUTFChars(sJ, sString);

	if ((int)b == 0)
		return false;

	return true;
}

JNIEXPORT jlong JNICALL Java_com_MatrixInterface_setDoubleProperty(JNIEnv *env, jobject thisObj, jstring sJ, jint idx, jdouble p)
{
	const char *sString = env->GetStringUTFChars(sJ, 0);
	char *s = strdup(sString);

	unsigned int rc;
	rc = setDoubleProperty(s, idx, p);
	free(s);

	env->ReleaseStringUTFChars(sJ, sString);
	
	return (jlong)(unsigned long long)rc;
}

JNIEXPORT jlong JNICALL Java_com_MatrixInterface_setIntegerProperty(JNIEnv *env, jobject thisObj, jstring sJ, jint idx, jint p)
{
	const char *sString = env->GetStringUTFChars(sJ, 0);
	char *s = strdup(sString);

	unsigned int rc;
	rc = setIntegerProperty(s, idx, p);
	free(s);

	env->ReleaseStringUTFChars(sJ, sString);

	return (jlong)(unsigned long long)rc;
}

JNIEXPORT jlong JNICALL Java_com_MatrixInterface_setEnumProperty(JNIEnv *env, jobject thisObj, jstring sJ, jint idx, jint p)
{
	const char *sString = env->GetStringUTFChars(sJ, 0);
	char *s = strdup(sString);

	unsigned int rc;
	rc = setEnumProperty(s, idx, p);
	free(s);

	env->ReleaseStringUTFChars(sJ, sString);

	return (jlong)(unsigned long long)rc;
}

JNIEXPORT jlong JNICALL Java_com_MatrixInterface_setBooleanProperty(JNIEnv *env, jobject thisObj, jstring sJ, jint idx, jboolean b)
{
	const char *sString = env->GetStringUTFChars(sJ, 0);
	char *s = strdup(sString);

	char c = 0;
	if (b)
		c = 1;

	unsigned int rc;
	rc = setBooleanProperty(s, idx, c);
	free(s);

	env->ReleaseStringUTFChars(sJ, sString);

	return (jlong)(unsigned long long)rc;
}

JNIEXPORT jlong JNICALL Java_com_MatrixInterface_setPairProperty(JNIEnv *env, jobject thisObj, jstring sJ, jint idx, jdouble p0, jdouble p1)
{
	const char *sString = env->GetStringUTFChars(sJ, 0);
	char *s = strdup(sString);

	unsigned int rc;
	rc = setPairProperty(s, idx, p0, p1);
	free(s);

	env->ReleaseStringUTFChars(sJ, sString);

	return (jlong)(unsigned long long)rc;
}

JNIEXPORT jlong JNICALL Java_com_MatrixInterface_callVoidFunction(JNIEnv *env, jobject thisObject, jstring sJ)
{
	const char *sString = env->GetStringUTFChars(sJ, 0);
	char *s = strdup(sString);

	unsigned int rc;
	rc = callFunction(s,0,0);
	free(s);

	env->ReleaseStringUTFChars(sJ, sString);

	return (jlong)(unsigned long long)rc;
}

JNIEXPORT jdouble JNICALL Java_com_MatrixInterface_callDoubleReturnFunction(JNIEnv *env, jobject thisObject, jstring sJ, jstring sJ2)
{
	const char *sString = env->GetStringUTFChars(sJ, 0);
	char *s = strdup(sString);

	const char *sString2 = env->GetStringUTFChars(sJ2, 0);
	char *s2 = strdup(sString2);

	double d;
	struct Value dValue;

	

	struct Value s2Value;
	s2Value.type = vt_STRING;
	s2Value.string = s2;

	//dValue.type = vt_DOUBLE;
	callFunction(s, &dValue, 1, s2Value);
	free(s2);
	free(s);

	d = dValue.real;
	
	env->ReleaseStringUTFChars(sJ, sString2);
	env->ReleaseStringUTFChars(sJ, sString);

	return d;
}


JNIEXPORT jdoubleArray JNICALL Java_com_MatrixInterface_getPairProperty(JNIEnv *env, jobject thisObj, jstring sJ, jint idx)
{
	const char *sString = env->GetStringUTFChars(sJ, 0);
	char *s = strdup(sString);

	jdouble coord[2];
	unsigned int rc;
	
	rc = getPairProperty(s, idx, &coord[0], &coord[1]);
	free(s);

	env->ReleaseStringUTFChars(sJ, sString);

	jdoubleArray outJNIArray = env->NewDoubleArray(2);
	if (NULL == outJNIArray)
		return NULL;

	env->SetDoubleArrayRegion(outJNIArray, 0, 2, coord);

	return outJNIArray;
}

JNIEXPORT jdoubleArray JNICALL Java_com_MatrixInterface_getDoubleArrayProperty(JNIEnv *env, jobject thisObj, jstring sJ, jint idx)
{
	const char *sString = env->GetStringUTFChars(sJ, 0);
	char *s = strdup(sString);

	jdouble *values = new jdouble[500];
	int size = 0;
	unsigned int rc;

	rc = getDoubleArrayProperty(s, idx, &size, &values);
	
	free(s);
	env->ReleaseStringUTFChars(sJ, sString);

	delete[] values;

	return NULL;
	/*
	jdouble info[1];
	info[0] = size;

	jdoubleArray outJNIArray = env->NewDoubleArray(1);
	if (NULL == outJNIArray)
		return NULL;

	env->SetDoubleArrayRegion(outJNIArray, 0, 1, info);

	return outJNIArray;
	*/
	/*
	

	jdoubleArray outJNIArray = env->NewDoubleArray(size);
	if (NULL == outJNIArray)
		return NULL;

	env->SetDoubleArrayRegion(outJNIArray, 0, size, values);

	return outJNIArray;*/
}

JNIEXPORT void JNICALL Java_com_MatrixInterface_setTrigger1(JNIEnv *env, jobject thisObj, jstring sJ, jstring sJ2)
{
	
	//trigger prop in matrix
	const char *sString = env->GetStringUTFChars(sJ, 0);
	char *s = strdup(sString);
	
	//method name in java
	const char *sString2 = env->GetStringUTFChars(sJ2, 0);
	trigger1Method = strdup(sString2);

	//assign the appropriate c method to be the observer
	unsigned int rc;
	rc = setEntityObserver(s, trigger1);
	free(s);

	env->ReleaseStringUTFChars(sJ, sString);
	env->ReleaseStringUTFChars(sJ2, sString2);	
}

void trigger1(int count, Value params[])
{
	JNIEnv *env;
	jint rs = jvm->AttachCurrentThread((void**)&env, NULL);

	jclass thisClass = env->GetObjectClass(jniObj);
	jmethodID trigger1Java = env->GetMethodID(thisClass, trigger1Method, "()V");
	
	env->CallVoidMethod(jniObj, trigger1Java);
	jvm->DetachCurrentThread();
}

JNIEXPORT void JNICALL Java_com_MatrixInterface_setTrigger2(JNIEnv *env, jobject thisObj, jstring sJ, jstring sJ2)
{

	//trigger prop in matrix
	const char *sString = env->GetStringUTFChars(sJ, 0);
	char *s = strdup(sString);

	//method name in java
	const char *sString2 = env->GetStringUTFChars(sJ2, 0);
	trigger2Method = strdup(sString2);

	//assign the appropriate c method to be the observer
	unsigned int rc;
	rc = setEntityObserver(s, trigger2);
	free(s);

	env->ReleaseStringUTFChars(sJ, sString);
	env->ReleaseStringUTFChars(sJ2, sString2);
}

void trigger2(int count, Value params[])
{
	JNIEnv *env;
	jint rs = jvm->AttachCurrentThread((void**)&env, NULL);

	jclass thisClass = env->GetObjectClass(jniObj);
	jmethodID trigger2Java = env->GetMethodID(thisClass, trigger2Method, "()V");

	env->CallVoidMethod(jniObj, trigger2Java);
	jvm->DetachCurrentThread();
}

JNIEXPORT void JNICALL Java_com_MatrixInterface_setTrigger3(JNIEnv *env, jobject thisObj, jstring sJ, jstring sJ2)
{

	//trigger prop in matrix
	const char *sString = env->GetStringUTFChars(sJ, 0);
	char *s = strdup(sString);

	//method name in java
	const char *sString2 = env->GetStringUTFChars(sJ2, 0);
	trigger3Method = strdup(sString2);

	//assign the appropriate c method to be the observer
	unsigned int rc;
	rc = setEntityObserver(s, trigger3);
	free(s);

	env->ReleaseStringUTFChars(sJ, sString);
	env->ReleaseStringUTFChars(sJ2, sString2);
}

void trigger3(int count, Value params[])
{
	JNIEnv *env;
	jint rs = jvm->AttachCurrentThread((void**)&env, NULL);

	jclass thisClass = env->GetObjectClass(jniObj);
	jmethodID trigger3Java = env->GetMethodID(thisClass, trigger3Method, "()V");

	env->CallVoidMethod(jniObj, trigger3Java);
	jvm->DetachCurrentThread();
}

JNIEXPORT void JNICALL Java_com_MatrixInterface_setTrigger4(JNIEnv *env, jobject thisObj, jstring sJ, jstring sJ2)
{

	//trigger prop in matrix
	const char *sString = env->GetStringUTFChars(sJ, 0);
	char *s = strdup(sString);

	//method name in java
	const char *sString2 = env->GetStringUTFChars(sJ2, 0);
	trigger4Method = strdup(sString2);

	//assign the appropriate c method to be the observer
	unsigned int rc;
	rc = setEntityObserver(s, trigger4);
	free(s);

	env->ReleaseStringUTFChars(sJ, sString);
	env->ReleaseStringUTFChars(sJ2, sString2);
}

void trigger4(int count, Value params[])
{
	JNIEnv *env;
	jint rs = jvm->AttachCurrentThread((void**)&env, NULL);

	jclass thisClass = env->GetObjectClass(jniObj);
	jmethodID trigger4Java = env->GetMethodID(thisClass, trigger4Method, "()V");

	env->CallVoidMethod(jniObj, trigger4Java);
	jvm->DetachCurrentThread();
}

JNIEXPORT void JNICALL Java_com_MatrixInterface_setArrayObserver1(JNIEnv *env, jobject thisObj, jstring sJ, jstring sJ2)
{

	//trigger prop in matrix
	const char *sString = env->GetStringUTFChars(sJ, 0);
	char *s = strdup(sString);

	//method name in java
	const char *sString2 = env->GetStringUTFChars(sJ2, 0);
	array1Method = strdup(sString2);

	//assign the appropriate c method to be the observer
	unsigned int rc;
	rc = setEntityObserver(s, array1);
	free(s);

	env->ReleaseStringUTFChars(sJ, sString);
	env->ReleaseStringUTFChars(sJ2, sString2);
}

void array1(int count, Value params[])
{
	JNIEnv *env;
	jint rs = jvm->AttachCurrentThread((void**)&env, NULL);

	jclass thisClass = env->GetObjectClass(jniObj);
	jmethodID array1Java = env->GetMethodID(thisClass, array1Method, "([D)V");
	
	jdoubleArray outJNIArray = env->NewDoubleArray(params[0].realArray.count);
	if (NULL != outJNIArray)
	{
		env->SetDoubleArrayRegion(outJNIArray, 0, params[0].realArray.count, params[0].realArray.pReal);

		env->CallVoidMethod(jniObj, array1Java, outJNIArray);
	}
	jvm->DetachCurrentThread();
}