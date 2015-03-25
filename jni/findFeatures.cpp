#include <jni.h>
#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/highgui/highgui.hpp>
#include <opencv2/calib3d/calib3d.hpp>
#include <opencv2/nonfree/nonfree.hpp>
#include <opencv2/features2d/features2d.hpp>
#include <vector>

using namespace std;
using namespace cv;

extern "C" {
JNIEXPORT void JNICALL Java_ca_ualberta_ev3ye_controller_streaming_ControllerActivity_FindFeatures(JNIEnv*, jobject, jlong addrGray, jlong addrKeypoint, jlong addrDescriptor);

JNIEXPORT void JNICALL Java_ca_ualberta_ev3ye_controller_streaming_ControllerActivity_FindFeatures(JNIEnv*, jobject, jlong addrGray, jlong addrKeypoint, jlong addrDescriptor)
{

	vector<KeyPoint>& keypoints_object = *(vector<KeyPoint>*)addrKeypoint;
	Mat& mdescriptors_object = *(Mat*)addrDescriptor;
	Mat& mGray  = *(Mat*)addrGray;

	//-- Step 1: Detect the keypoints
	int minHessian = 10000;
	SurfFeatureDetector detector(minHessian);
    detector.detect(mGray, keypoints_object);

    //-- Step 2: Calculate descriptors (feature vectors)
    SurfDescriptorExtractor extractor;
    extractor.compute( mGray, keypoints_object, mdescriptors_object );
}


void Mat_to_vector_KeyPoint(Mat& mat, vector<KeyPoint>& v_kp)
{
    v_kp.clear();
    for(int i=0; i<mat.rows; i++)
    {
        Vec<float, 7> v = mat.at< Vec<float, 7> >(i, 0);
        KeyPoint kp(v[0], v[1], v[2], v[3], v[4], (int)v[5], (int)v[6]);
        v_kp.push_back(kp);
    }
    return;
}


void vector_KeyPoint_to_Mat(vector<KeyPoint>& v_kp, Mat& mat)
{
    int count = (int)v_kp.size();
    mat.create(count, 1, CV_32FC(7));
    for(int i=0; i<count; i++)
    {
        KeyPoint kp = v_kp[i];
        mat.at< Vec<float, 7> >(i, 0) = Vec<float, 7>(kp.pt.x, kp.pt.y, kp.size, kp.angle, kp.response, (float)kp.octave, (float)kp.class_id);
    }
}


}
