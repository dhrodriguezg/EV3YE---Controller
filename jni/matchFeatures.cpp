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
JNIEXPORT void JNICALL Java_ca_ualberta_ev3ye_controller_vs_VisualServoing_MatchFeatures(JNIEnv*, jobject, jlong addrMarker, jlong addrScene, jlong addrSceRgba, jlong addrOutput, jlong addrMatch, bool degug);

JNIEXPORT void JNICALL Java_ca_ualberta_ev3ye_controller_vs_VisualServoing_MatchFeatures(JNIEnv*, jobject, jlong addrMarker, jlong addrScene, jlong addrSceRgba, jlong addrOutput, jlong addrMatch, bool degug) {

	Mat& mRgb = *(Mat*) addrSceRgba; //Actually it's BGR
	Mat& img_object = *(Mat*) addrMarker;
	Mat& img_scene = *(Mat*) addrScene;
	Mat& img_output = *(Mat*) addrOutput;
	Mat& mMatches = *(Mat*) addrMatch;

	float thresholdDot = 0.5f; // from 0 to 1.    0 -> perfect square, 1 -> perfect line    >	max 30º
	float thresholdLenght = 0.7f; //from 0 to 1.  1 -> perfect square, 0 -> rhombus			<   max 30%
	Scalar matchColor(0, 255, 0);


	try {

		int radious = mRgb.cols / 192 + 1;
		//-- Step 1: Detect the keypoints using SURF Detector
		int minHessian = 10000;
		SurfFeatureDetector detector(minHessian); //FastFeatureDetector,SurfFeatureDetector
		vector<KeyPoint> keypoints_object, keypoints_scene;

		detector.detect(img_scene, keypoints_scene);
		detector.detect(img_object, keypoints_object);

		//-- Step 2: Calculate descriptors (feature vectors)
		SurfDescriptorExtractor extractor; //OrbDescriptorExtractor,SurfDescriptorExtractor
		Mat descriptors_object, descriptors_scene;

		extractor.compute(img_scene, keypoints_scene, descriptors_scene);
		extractor.compute(img_object, keypoints_object, descriptors_object);

		if (descriptors_scene.empty()) {
			mMatches.at<Vec<float, 8> >(0, 0) = Vec<float, 8>(-1.,-1.,-1.,-1.,-1.,-1.,-1.,-1.);
			img_output = mRgb;
			return;
		}
		if (descriptors_object.empty()) {
			mMatches.at<Vec<float, 8> >(0, 0) = Vec<float, 8>(-1.,-1.,-1.,-1.,-1.,-1.,-1.,-1.);
			img_output = mRgb;
			return;
		}

		//-- Step 3: Matching descriptor vectors using FLANN matcher
		FlannBasedMatcher matcher; // BFMatcher,FlannBasedMatcher
		vector<DMatch> matches;
		matcher.match(descriptors_object, descriptors_scene, matches);

		double max_dist = 0;
		double min_dist = 1000;

		//-- Quick calculation of max and min distances between keypoints
		for (int i = 0; i < descriptors_object.rows; i++) {
			DMatch match = matches[i];
			double dist = match.distance;
			if (dist < min_dist)
				min_dist = dist;
			if (dist > max_dist)
				max_dist = dist;
		}

		//-- Draw only "good" matches (i.e. whose distance is less than 3*min_dist )
		vector<DMatch> good_matches;
		for (int i = 0; i < descriptors_object.rows; i++) {
			DMatch match = matches[i];
			if (match.distance < 3 * min_dist) {
				good_matches.push_back(match);
			}
		}

		//Mat img_output;
		if (degug)
			drawMatches(img_object, keypoints_object, mRgb, keypoints_scene,good_matches, img_output, Scalar::all(-1), Scalar::all(-1),vector<char>(), DrawMatchesFlags::NOT_DRAW_SINGLE_POINTS);

		//-- Localize the object
		vector<Point2f> obj;
		vector<Point2f> scene;

		for (int i = 0; i < good_matches.size(); i++) {
			DMatch good_match = good_matches[i];
			KeyPoint kpo = keypoints_object[good_match.queryIdx];
			KeyPoint kps = keypoints_scene[good_match.trainIdx];
			//-- Get the keypoints from the good matches
			obj.push_back(kpo.pt);
			scene.push_back(kps.pt);
		}

		//-- Get the corners from the image_1 ( the object to be "detected" )
		std::vector<Point2f> obj_corners(4);
		obj_corners[0] = cvPoint(0, 0);
		obj_corners[1] = cvPoint(img_object.cols, 0);
		obj_corners[2] = cvPoint(img_object.cols, img_object.rows);
		obj_corners[3] = cvPoint(0, img_object.rows);
		vector<Point2f> scene_corners(4);

		Mat H = findHomography(obj, scene, CV_RANSAC);
		perspectiveTransform(obj_corners, scene_corners, H);

		Point2f corner_0 = scene_corners[0]; //left-up
		Point2f corner_1 = scene_corners[1]; //up-right
		Point2f corner_2 = scene_corners[2]; //down-right
		Point2f corner_3 = scene_corners[3]; //down-left
		Point2f middlePoint =  (corner_0+corner_1+corner_2+corner_3)*(1./4.);

		//Check if the marker makes sense..
		Point2f diag_0_2=corner_2-corner_0;
		Point2f diag_3_1=corner_3-corner_1;
		float line_0_2=norm(diag_0_2);
		float line_3_1=norm(diag_3_1);
		diag_0_2 = diag_0_2 * (1./line_0_2);
		diag_3_1 = diag_3_1 * (1./line_3_1);
		float dotDiag = abs(diag_0_2.dot(diag_3_1));

		float smallLine;
		float bigLine;
		if(line_0_2 > line_3_1){
			smallLine=line_3_1;
			bigLine=line_0_2;
		}else{
			smallLine=line_0_2;
			bigLine=line_3_1;
		}
		mMatches.at<Vec<float, 10> >(0, 0) = Vec<float, 10>(middlePoint.x,middlePoint.y,corner_0.x,corner_0.y, corner_1.x, corner_1.y, corner_2.x, corner_2.y,corner_3.x, corner_3.y);

		//Logic in case marker fails...
		if(dotDiag > thresholdDot){ //
			mMatches.at<Vec<float, 8> >(0, 0) = Vec<float, 8>(-1.,-1.,-1.,-1.,-1.,-1.,-1.,-1.);
			matchColor = Scalar(0, 200, 255); //orange
		}
		if(smallLine/bigLine < thresholdLenght){
			mMatches.at<Vec<float, 8> >(0, 0) = Vec<float, 8>(-1.,-1.,-1.,-1.,-1.,-1.,-1.,-1.);
			matchColor = Scalar(0, 0, 255); //red
		}

		if (degug) {
			line(img_output, corner_0 + Point2f(img_object.cols, 0),corner_1 + Point2f(img_object.cols, 0), matchColor,4);
			line(img_output, corner_1 + Point2f(img_object.cols, 0),corner_2 + Point2f(img_object.cols, 0), matchColor,4);
			line(img_output, corner_2 + Point2f(img_object.cols, 0),corner_3 + Point2f(img_object.cols, 0), matchColor,4);
			line(img_output, corner_3 + Point2f(img_object.cols, 0),corner_0 + Point2f(img_object.cols, 0), matchColor,4);

			line(img_output, corner_0 + Point2f(img_object.cols, 0),corner_2 + Point2f(img_object.cols, 0), matchColor,4);
			line(img_output, corner_1 + Point2f(img_object.cols, 0),corner_3 + Point2f(img_object.cols, 0), matchColor,4);
		} else {
			img_output = mRgb;
			line(img_output, corner_0, corner_1, matchColor, 4);
			line(img_output, corner_1, corner_2, matchColor, 4);
			line(img_output, corner_2, corner_3, matchColor, 4);
			line(img_output, corner_3, corner_0, matchColor, 4);

		}


	} catch (Exception &ex) {
		mMatches.at<Vec<float, 8> >(0, 0) = Vec<float, 8>(-1.,-1.,-1.,-1.,-1.,-1.,-1.,-1.);
		img_output = mRgb;
		return;
	}

}
}
